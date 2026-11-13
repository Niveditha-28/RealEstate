
package com.realestate.api;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/seller")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class SellerController {

    private final JdbcTemplate jdbcTemplate;

    public SellerController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PropertyRequest(
            String title,
            Integer propertyTypeId,
            String listingType,
            BigDecimal price,
            String city,
            BigDecimal area,
            Integer bedrooms,
            Integer bathrooms,
            String address,
            String description
    ) {}

    @PostMapping("/properties")
    public ResponseEntity<?> createProperty(
            @RequestBody PropertyRequest request,
            @RequestHeader(
                value = "X-CSRF-Token",
                required = false
            ) String csrfToken,
            HttpSession session) {

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

        if (request.title() == null ||
            request.title().isBlank() ||
            request.city() == null ||
            request.city().isBlank() ||
            request.address() == null ||
            request.address().isBlank() ||
            request.description() == null ||
            request.description().isBlank() ||
            request.propertyTypeId() == null ||
            request.listingType() == null ||
            (!request.listingType().equals("SALE") &&
             !request.listingType().equals("RENT")) ||
            request.price() == null ||
            request.price().signum() <= 0 ||
            request.area() == null ||
            request.area().signum() <= 0 ||
            request.bedrooms() == null ||
            request.bedrooms() < 0 ||
            request.bathrooms() == null ||
            request.bathrooms() < 0) {

            return ResponseEntity.badRequest().body(
                Map.of("message", "Please enter valid property details")
            );
        }

        Integer typeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM property_type WHERE Property_Type_ID = ?",
            Integer.class,
            request.propertyTypeId()
        );

        if (typeCount == null || typeCount == 0) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Invalid property type")
            );
        }

        String sql = """
            INSERT INTO property
            (Owner_ID, Property_Type_ID, Title,
             Description, Address, City, Price,
             Area, Bedrooms, Bathrooms,
             Listing_Type, Status, Listed_Date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, 'AVAILABLE', CURDATE())
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
            );

            ps.setInt(1, ((Number) userId).intValue());
            ps.setInt(2, request.propertyTypeId());
            ps.setString(3, request.title().trim());
            ps.setString(4, request.description().trim());
            ps.setString(5, request.address().trim());
            ps.setString(6, request.city().trim());
            ps.setBigDecimal(7, request.price());
            ps.setBigDecimal(8, request.area());
            ps.setInt(9, request.bedrooms());
            ps.setInt(10, request.bathrooms());
            ps.setString(11, request.listingType());

            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        return ResponseEntity.status(201).body(
            Map.of(
                "message", "Property listed successfully",
                "propertyId", generatedId.longValue()
            )
        );
    }
    
@GetMapping("/properties")
public ResponseEntity<?> getMyProperties(
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (userId == null || !"SELLER".equals(role)) {
        return ResponseEntity.status(403).body(
            Map.of("message", "Seller login required")
        );
    }

    String sql = """
        SELECT
            p.Property_ID AS id,
            p.Title AS title,
            p.Description AS description,
            p.Address AS address,
            p.City AS city,
            p.Price AS price,
            p.Area AS area,
            p.Bedrooms AS bedrooms,
            p.Bathrooms AS bathrooms,
            p.Listing_Type AS type,
            p.Status AS status,
            p.Property_Type_ID AS propertyTypeId,
            pt.Type_Name AS propertyType,
            (
                SELECT pi.Image_URL
                FROM property_image pi
                WHERE pi.Property_ID = p.Property_ID
                ORDER BY pi.Image_ID
                LIMIT 1
            ) AS image
        FROM property p
        JOIN property_type pt
            ON p.Property_Type_ID = pt.Property_Type_ID
        WHERE p.Owner_ID = ?
        ORDER BY p.Property_ID DESC
        """;

    return ResponseEntity.ok(
        jdbcTemplate.queryForList(sql, userId)
    );
}

@PutMapping("/properties/{id}")
public ResponseEntity<?> updateProperty(
        @PathVariable Long id,
        @RequestBody PropertyRequest request,
        @RequestHeader(
            value = "X-CSRF-Token",
            required = false
        ) String csrfToken,
        HttpSession session) {

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

    if (request.title() == null || request.title().isBlank() ||
        request.propertyTypeId() == null ||
        request.listingType() == null ||
        (!request.listingType().equals("SALE") &&
         !request.listingType().equals("RENT")) ||
        request.price() == null || request.price().signum() <= 0 ||
        request.area() == null || request.area().signum() <= 0 ||
        request.city() == null || request.city().isBlank() ||
        request.address() == null || request.address().isBlank() ||
        request.description() == null || request.description().isBlank() ||
        request.bedrooms() == null || request.bedrooms() < 0 ||
        request.bathrooms() == null || request.bathrooms() < 0) {

        return ResponseEntity.badRequest().body(
            Map.of("message", "Invalid property details")
        );
    }

    Integer typeCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM property_type WHERE Property_Type_ID = ?",
        Integer.class,
        request.propertyTypeId()
    );

    if (typeCount == null || typeCount == 0) {
        return ResponseEntity.badRequest().body(
            Map.of("message", "Invalid property type")
        );
    }

    String sql = """
        UPDATE property
        SET Title = ?,
            Property_Type_ID = ?,
            Listing_Type = ?,
            Price = ?,
            City = ?,
            Area = ?,
            Bedrooms = ?,
            Bathrooms = ?,
            Address = ?,
            Description = ?
        WHERE Property_ID = ?
          AND Owner_ID = ?
          AND Status = 'AVAILABLE'
        """;

    int updated = jdbcTemplate.update(
        sql,
        request.title().trim(),
        request.propertyTypeId(),
        request.listingType(),
        request.price(),
        request.city().trim(),
        request.area(),
        request.bedrooms(),
        request.bathrooms(),
        request.address().trim(),
        request.description().trim(),
        id,
        ((Number) userId).longValue()
    );

    if (updated == 0) {
        return ResponseEntity.status(404).body(
            Map.of("message",
                "Property not found, not yours, or not available for editing")
        );
    }

    return ResponseEntity.ok(
        Map.of("message", "Property updated successfully")
    );
}

}
