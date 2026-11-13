
package com.realestate.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
@RequestMapping("/api/enquiries")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class EnquiryController {

    private final JdbcTemplate jdbcTemplate;

    public EnquiryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record EnquiryRequest(
        Long propertyId,
        String message
    ) {}

    @PostMapping
    public ResponseEntity<?> createEnquiry(
            @RequestBody EnquiryRequest request,
            @RequestHeader(
                value = "X-CSRF-Token",
                required = false
            ) String csrfToken,
            HttpSession session) {

        // 1. Verify buyer login
        Object userId = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (!(userId instanceof Number) ||
            !"BUYER".equals(role)) {

            return ResponseEntity.status(403).body(
                Map.of("message", "Buyer login required")
            );
        }

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

        // 3. Validate enquiry details
        if (request == null ||
            request.propertyId() == null ||
            request.message() == null ||
            request.message().isBlank() ||
            request.message().length() > 1000) {

            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "Enter a message between 1 and 1000 characters")
            );
        }

        // 4. Check property availability
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
            ((Number) userId).longValue()
        );

        if (propertyCount == null || propertyCount == 0) {
            return ResponseEntity.badRequest().body(
                Map.of("message",
                    "Property unavailable or enquiry not allowed")
            );
        }

        // 5. Save enquiry to existing table
        String sql = """
            INSERT INTO enquiry
            (Buyer_ID, Property_ID, Message,
             Enquiry_Date, Status)
            VALUES (?, ?, ?, NOW(), 'OPEN')
            """;

        jdbcTemplate.update(
            sql,
            ((Number) userId).longValue(),
            request.propertyId(),
            request.message().trim()
        );

        return ResponseEntity.status(201).body(
            Map.of("message", "Enquiry submitted successfully")
        );
    }
    
@GetMapping("/seller")
public ResponseEntity<?> getSellerEnquiries(
        HttpSession session) {

    // Verify seller login
    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
        !"SELLER".equals(role)) {

        return ResponseEntity.status(403).body(
            Map.of("message", "Seller login required")
        );
    }

    // Retrieve enquiries for seller-owned properties
    String sql = """
        SELECT
            e.Enquiry_ID AS id,
            e.Property_ID AS propertyId,
            p.Title AS propertyTitle,
            u.Name AS buyerName,
            e.Message AS message,
e.Enquiry_Date AS enquiryDate,
e.Seller_Reply AS sellerReply,
e.Replied_Date AS repliedDate,
e.Status AS status
        FROM enquiry e
        JOIN property p
            ON e.Property_ID = p.Property_ID
        JOIN users u
            ON e.Buyer_ID = u.User_ID
        WHERE p.Owner_ID = ?
        ORDER BY e.Enquiry_Date DESC, e.Enquiry_ID DESC
        """;

    return ResponseEntity.ok(
        jdbcTemplate.queryForList(
            sql,
            ((Number) userId).longValue()
        )
    );
}

// ======================================
// SELLER REPLY TO BUYER ENQUIRY
// ======================================

public record ReplyRequest(String reply) {}

@PutMapping("/{enquiryId}/reply")
public ResponseEntity<?> replyToEnquiry(
        @PathVariable Long enquiryId,
        @RequestBody ReplyRequest request,
        @RequestHeader(
            value = "X-CSRF-Token",
            required = false
        ) String csrfToken,
        HttpSession session) {

    // 1. Verify seller login
    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
        !"SELLER".equals(role)) {

        return ResponseEntity.status(403).body(
            Map.of("message", "Seller login required")
        );
    }

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

    // 3. Validate reply
    if (request == null ||
        request.reply() == null ||
        request.reply().isBlank() ||
        request.reply().length() > 1000) {

        return ResponseEntity.badRequest().body(
            Map.of("message",
                "Reply must contain 1 to 1000 characters")
        );
    }

    // 4. Update only enquiries belonging to this seller
    String sql = """
        UPDATE enquiry e
        JOIN property p
            ON e.Property_ID = p.Property_ID
        SET
            e.Seller_Reply = ?,
            e.Replied_Date = NOW(),
            e.Status = 'REPLIED'
        WHERE e.Enquiry_ID = ?
          AND p.Owner_ID = ?
          AND e.Status = 'OPEN'
        """;

    int updated = jdbcTemplate.update(
        sql,
        request.reply().trim(),
        enquiryId,
        ((Number) userId).longValue()
    );

    if (updated == 0) {
        return ResponseEntity.status(404).body(
            Map.of("message",
                "Open enquiry not found or access denied")
        );
    }

    return ResponseEntity.ok(
        Map.of("message", "Reply sent successfully")
    );
}

}
