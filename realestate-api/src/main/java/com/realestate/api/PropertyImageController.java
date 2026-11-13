
package com.realestate.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/property-images")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class PropertyImageController {

    private final JdbcTemplate jdbcTemplate;
    private final Path uploadDirectory;

    public PropertyImageController(
            JdbcTemplate jdbcTemplate,
            @Value("${app.upload.dir}") String uploadDir
    ) throws IOException {

        this.jdbcTemplate = jdbcTemplate;

        this.uploadDirectory =
            Paths.get(uploadDir).toAbsolutePath().normalize();

        Files.createDirectories(this.uploadDirectory);
    }

    // ==================================
    // UPLOAD PROPERTY IMAGES
    // ==================================

    @PostMapping(
        value = "/{propertyId}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadImages(
            @PathVariable Long propertyId,
            @RequestParam("images") List<MultipartFile> images,
            @RequestHeader(
                value = "X-CSRF-Token",
                required = false
            ) String csrfToken,
            HttpSession session
    ) throws IOException {

        Object userId = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (userId == null || !"SELLER".equals(role)) {
            return ResponseEntity.status(403).body(
                Map.of("message", "Seller login required")
            );
        }

        String expectedToken =
            (String) session.getAttribute("csrfToken");

        if (expectedToken == null || csrfToken == null ||
            !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                csrfToken.getBytes(StandardCharsets.UTF_8))) {

            return ResponseEntity.status(403).body(
                Map.of("message", "Invalid CSRF token")
            );
        }

        Integer ownerCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM property
            WHERE Property_ID = ?
              AND Owner_ID = ?
              AND Status = 'AVAILABLE'
            """,
            Integer.class,
            propertyId,
            ((Number) userId).longValue()
        );

        if (ownerCount == null || ownerCount == 0) {
            return ResponseEntity.status(403).body(
                Map.of("message",
                    "Property not found or upload not allowed")
            );
        }

        if (images == null || images.isEmpty()
                || images.size() > 5) {

            return ResponseEntity.badRequest().body(
                Map.of("message", "Select 1 to 5 images")
            );
        }

        Integer existingCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM property_image
            WHERE Property_ID = ?
            """,
            Integer.class,
            propertyId
        );

        if (existingCount + images.size() > 5) {
            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "A property can have a maximum of 5 images")
            );
        }

        // Validate all files before saving any
        for (MultipartFile file : images) {
            if (file.isEmpty() ||
                file.getSize() > 5L * 1024 * 1024) {

                return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid image size")
                );
            }

            byte[] header = new byte[12];

            try (var stream = file.getInputStream()) {
                int count = stream.read(header);

                if (count < 12) {
                    return ResponseEntity.badRequest().body(
                        Map.of("message", "Invalid image file")
                    );
                }
            }

            String detectedType = detectImageType(header);

            if (detectedType == null) {
                return ResponseEntity.badRequest().body(
                    Map.of("message",
                        "Only JPG, PNG and WEBP images are allowed")
                );
            }
        }

        List<String> uploadedUrls = new ArrayList<>();
        List<Path> savedFiles = new ArrayList<>();

        try {
            for (MultipartFile file : images) {
                byte[] header = new byte[12];

                try (var stream = file.getInputStream()) {
                    stream.readNBytes(header, 0, 12);
                }

                String type = detectImageType(header);

                String extension = switch (type) {
                    case "image/jpeg" -> ".jpg";
                    case "image/png" -> ".png";
                    case "image/webp" -> ".webp";
                    default -> throw new IOException("Invalid image");
                };

                String fileName =
                    UUID.randomUUID() + extension;

                Path destination =
                    uploadDirectory.resolve(fileName);

                Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
                );

                savedFiles.add(destination);

                String imageUrl =
                    "/api/property-images/file/" + fileName;

                jdbcTemplate.update(
                    """
                    INSERT INTO property_image
                    (Property_ID, Image_URL, Image_Type)
                    VALUES (?, ?, ?)
                    """,
                    propertyId,
                    imageUrl,
                    type
                );

                uploadedUrls.add(imageUrl);
            }

        } catch (Exception error) {
            // Clean up files if upload fails.
            // Database rollback is not yet implemented.
            for (Path path : savedFiles) {
                Files.deleteIfExists(path);
            }

            return ResponseEntity.internalServerError().body(
                Map.of("message", "Image upload failed")
            );
        }

        return ResponseEntity.ok(
            Map.of(
                "message", "Images uploaded successfully",
                "images", uploadedUrls
            )
        );
    }

    // ==================================
    // SERVE UPLOADED IMAGES
    // ==================================

    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String fileName
    ) throws IOException {

        if (!fileName.matches(
                "[a-fA-F0-9-]{36}\\.(jpg|png|webp)")) {
            return ResponseEntity.badRequest().build();
        }

        Path file = uploadDirectory.resolve(fileName)
                .normalize();

        if (!file.startsWith(uploadDirectory)
                || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(file.toUri());

        String contentType = Files.probeContentType(file);

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                HttpHeaders.CACHE_CONTROL,
                "public, max-age=3600"
            )
            .body(resource);
    }

    // ==================================
    // CHECK IMAGE FILE SIGNATURE
    // ==================================

    private String detectImageType(byte[] bytes) {

        if (bytes.length < 12) return null;

        // JPEG
        if ((bytes[0] & 0xFF) == 0xFF &&
            (bytes[1] & 0xFF) == 0xD8 &&
            (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG
        byte[] png = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
        };

        boolean isPng = true;

        for (int i = 0; i < png.length; i++) {
            if (bytes[i] != png[i]) {
                isPng = false;
                break;
            }
        }

        if (isPng) return "image/png";

        // WEBP: RIFF....WEBP
        String riff = new String(
            bytes, 0, 4, StandardCharsets.US_ASCII
        );

        String webp = new String(
            bytes, 8, 4, StandardCharsets.US_ASCII
        );

        if (riff.equals("RIFF") && webp.equals("WEBP")) {
            return "image/webp";
        }

        return null;
    }
}
