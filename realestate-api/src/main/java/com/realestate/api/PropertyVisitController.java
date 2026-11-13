
package com.realestate.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
@RequestMapping("/api/visits")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class PropertyVisitController {

    private final JdbcTemplate jdbcTemplate;

    public PropertyVisitController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record VisitRequest(
        Long propertyId,
        LocalDate visitDate,
        LocalTime visitTime,
        String remarks
    ) {}

    @PostMapping
    public ResponseEntity<?> bookVisit(
            @RequestBody VisitRequest request,
            @RequestHeader(
                value = "X-CSRF-Token",
                required = false
            ) String csrfToken,
            HttpSession session) {
                

        // 1. Verify buyer authentication
        Object userId = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (!(userId instanceof Number) ||
            !"BUYER".equals(role)) {

            return ResponseEntity.status(403).body(
                Map.of("message", "Buyer login required")
            );
        }

        long buyerId = ((Number) userId).longValue();

        // 2. Verify CSRF token
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

        // 3. Validate request
        if (request == null ||
            request.propertyId() == null ||
            request.visitDate() == null ||
            request.visitTime() == null) {

            return ResponseEntity.badRequest().body(
                Map.of("message", "Property, date and time are required")
            );
        }

        LocalDateTime requestedDateTime =
            LocalDateTime.of(
                request.visitDate(),
                request.visitTime()
            );

        if (!requestedDateTime.isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Select a future date and time")
            );
        }

        String remarks = request.remarks() == null
            ? ""
            : request.remarks().trim();

        if (remarks.length() > 500) {
            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "Remarks cannot exceed 500 characters")
            );
        }

        // 4. Verify property is available
        Integer propertyCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM property
            WHERE Property_ID = ?
              AND Status = 'AVAILABLE'
              AND Owner_ID <> ?
            """,
            Integer.class,
            request.propertyId(),
            buyerId
        );

        if (propertyCount == null || propertyCount == 0) {
            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "Property unavailable or visit not allowed")
            );
        }

        // 5. Prevent duplicate visits at the same time
        Integer duplicateCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM property_visit
            WHERE Buyer_ID = ?
              AND Property_ID = ?
              AND Visit_Date = ?
              AND Visit_Time = ?
              AND Status = 'SCHEDULED'
            """,
            Integer.class,
            buyerId,
            request.propertyId(),
            request.visitDate(),
            request.visitTime()
        );

        if (duplicateCount != null && duplicateCount > 0) {
            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "You already booked this visit")
            );
        }

        // 6. Insert visit booking
        String sql = """
            INSERT INTO property_visit
            (Buyer_ID, Property_ID, Agent_ID,
             Visit_Date, Visit_Time, Status, Remarks)
            VALUES (?, ?, NULL, ?, ?, 'SCHEDULED', ?)
            """;

        jdbcTemplate.update(
            sql,
            buyerId,
            request.propertyId(),
            request.visitDate(),
            request.visitTime(),
            remarks
        );

        return ResponseEntity.status(201).body(
            Map.of("message", "Property visit booked successfully")
        );
    }

@GetMapping("/seller")
public ResponseEntity<?> getSellerVisits(HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    // Only logged-in sellers can view visit requests
    if (!(userId instanceof Number) || !"SELLER".equals(role)) {
        return ResponseEntity.status(403)
                .body(Map.of("message", "Seller login required"));
    }

    long sellerId = ((Number) userId).longValue();

    String sql = """
        SELECT
            pv.Visit_ID AS id,
            pv.Property_ID AS propertyId,
            p.Title AS propertyTitle,
            u.Name AS buyerName,
            u.Email AS buyerEmail,
            pv.Visit_Date AS visitDate,
            pv.Visit_Time AS visitTime,
            pv.Status AS status,
            pv.Remarks AS remarks
        FROM property_visit pv
        JOIN property p
            ON pv.Property_ID = p.Property_ID
        JOIN users u
            ON pv.Buyer_ID = u.User_ID
        WHERE p.Owner_ID = ?
        ORDER BY pv.Visit_Date DESC, pv.Visit_Time DESC
        """;

    return ResponseEntity.ok(
        jdbcTemplate.queryForList(sql, sellerId)
    );
}

/* UPDATE PROPERTY VISIT STATUS */

public record VisitStatusRequest(String status) {}

@PutMapping("/{visitId}/status")
public ResponseEntity<?> updateVisitStatus(
        @PathVariable Long visitId,
        @RequestBody VisitStatusRequest request,
        @RequestHeader(
            value = "X-CSRF-Token",
            required = false
        ) String csrfToken,
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    // Only sellers can update visit status
    if (!(userId instanceof Number) || !"SELLER".equals(role)) {
        return ResponseEntity.status(403)
                .body(Map.of("message", "Seller login required"));
    }

    // Validate CSRF token
    String expectedToken =
            (String) session.getAttribute("csrfToken");

    if (expectedToken == null || csrfToken == null ||
            !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                csrfToken.getBytes(StandardCharsets.UTF_8))) {

        return ResponseEntity.status(403)
                .body(Map.of("message", "Invalid CSRF token"));
    }

    if (request == null || request.status() == null) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Status is required"));
    }

    String newStatus = request.status().trim().toUpperCase();

    if (!newStatus.equals("COMPLETED") &&
        !newStatus.equals("CANCELLED")) {

        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid visit status"));
    }

    long sellerId = ((Number) userId).longValue();

    // Update only scheduled visits belonging to this seller
    String sql = """
        UPDATE property_visit pv
        JOIN property p
            ON pv.Property_ID = p.Property_ID
        SET pv.Status = ?
        WHERE pv.Visit_ID = ?
          AND p.Owner_ID = ?
          AND pv.Status = 'SCHEDULED'
        """;

    int updated = jdbcTemplate.update(
            sql, newStatus, visitId, sellerId);

    if (updated == 0) {
        return ResponseEntity.status(409)
                .body(Map.of(
                    "message",
                    "Visit not found, not owned by seller, or already processed"
                ));
    }

    return ResponseEntity.ok(
            Map.of("message", "Visit status updated successfully")
    );
}

}
