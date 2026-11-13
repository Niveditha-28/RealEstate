
package com.realestate.api;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/bookings")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class BookingController {

    private final JdbcTemplate jdbcTemplate;

    public BookingController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record BookingRequest(Long propertyId) {}

    @PostMapping
    @Transactional
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequest request,
            @RequestHeader(
                value = "X-CSRF-Token",
                required = false
            ) String csrfToken,
            HttpSession session) {

        Object userId = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (!(userId instanceof Number) ||
                !"BUYER".equals(role)) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                        "message", "Buyer login required"
                    ));
        }

        String expectedToken =
                (String) session.getAttribute("csrfToken");

        if (expectedToken == null || csrfToken == null ||
                !MessageDigest.isEqual(
                    expectedToken.getBytes(StandardCharsets.UTF_8),
                    csrfToken.getBytes(StandardCharsets.UTF_8))) {

            return ResponseEntity.status(403)
                    .body(Map.of(
                        "message", "Invalid CSRF token"
                    ));
        }

        if (request == null || request.propertyId() == null ||
                request.propertyId() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Valid property ID required"
                    ));
        }

        long buyerId = ((Number) userId).longValue();

        // Lock the property row during booking creation
        var properties = jdbcTemplate.queryForList("""
            SELECT Owner_ID, Listing_Type, Price, Status
            FROM property
            WHERE Property_ID = ?
            FOR UPDATE
            """, request.propertyId());

        if (properties.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Property not found"
                    ));
        }

        Map<String, Object> property = properties.get(0);

        if (!"AVAILABLE".equals(property.get("Status"))) {
            return ResponseEntity.status(409)
                    .body(Map.of(
                        "message", "Property is not available"
                    ));
        }

        long ownerId =
                ((Number) property.get("Owner_ID")).longValue();

        if (ownerId == buyerId) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Cannot book your own property"
                    ));
        }

        // Prevent duplicate active booking requests
        Integer existing = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM booking
            WHERE Property_ID = ?
              AND Status IN ('PENDING', 'CONFIRMED')
            """, Integer.class, request.propertyId());

        if (existing != null && existing > 0) {
            return ResponseEntity.status(409)
                    .body(Map.of(
                        "message",
                        "This property already has an active booking"
                    ));
        }

        String bookingType =
                (String) property.get("Listing_Type");

        BigDecimal bookingAmount =
                (BigDecimal) property.get("Price");

        jdbcTemplate.update("""
            INSERT INTO booking
                (Buyer_ID, Property_ID, Booking_Date,
                 Booking_Type, Booking_Amount, Status)
            VALUES (?, ?, NOW(), ?, ?, 'PENDING')
            """,
            buyerId,
            request.propertyId(),
            bookingType,
            bookingAmount
        );

        return ResponseEntity.status(201)
                .body(Map.of(
                    "message",
                    "Booking request submitted successfully"
                ));
    }

/* GET SELLER BOOKING REQUESTS */

@GetMapping("/seller")
public ResponseEntity<?> getSellerBookings(
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
            !"SELLER".equals(role)) {

        return ResponseEntity.status(403)
                .body(Map.of(
                    "message", "Seller login required"
                ));
    }

    long sellerId = ((Number) userId).longValue();

    String sql = """
        SELECT
            b.Booking_ID AS id,
            b.Property_ID AS propertyId,
            p.Title AS propertyTitle,
            u.Name AS buyerName,
            u.Email AS buyerEmail,
            b.Booking_Date AS bookingDate,
            b.Booking_Type AS bookingType,
            b.Booking_Amount AS bookingAmount,
            b.Status AS status
        FROM booking b
        JOIN property p
            ON b.Property_ID = p.Property_ID
        JOIN users u
            ON b.Buyer_ID = u.User_ID
        WHERE p.Owner_ID = ?
        ORDER BY b.Booking_Date DESC, b.Booking_ID DESC
        """;

    return ResponseEntity.ok(
        jdbcTemplate.queryForList(sql, sellerId)
    );
}

/* CONFIRM OR CANCEL PROPERTY BOOKING */

public record BookingStatusRequest(String status) {}

@PutMapping("/{bookingId}/status")
@Transactional
public ResponseEntity<?> updateBookingStatus(
        @PathVariable Long bookingId,
        @RequestBody BookingStatusRequest request,
        @RequestHeader(
            value = "X-CSRF-Token",
            required = false
        ) String csrfToken,
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
            !"SELLER".equals(role)) {
        return ResponseEntity.status(403)
                .body(Map.of("message", "Seller login required"));
    }

    String expectedToken =
            (String) session.getAttribute("csrfToken");

    if (expectedToken == null || csrfToken == null ||
            !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                csrfToken.getBytes(StandardCharsets.UTF_8))) {
        return ResponseEntity.status(403)
                .body(Map.of("message", "Invalid CSRF token"));
    }

    if (bookingId == null || bookingId <= 0 ||
            request == null || request.status() == null) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid booking request"));
    }

    String newStatus = request.status().trim().toUpperCase();

    if (!newStatus.equals("CONFIRMED") &&
            !newStatus.equals("CANCELLED")) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid booking status"));
    }

    long sellerId = ((Number) userId).longValue();

    // First find the property associated with this booking
    var bookingRows = jdbcTemplate.queryForList("""
        SELECT b.Property_ID
        FROM booking b
        JOIN property p ON b.Property_ID = p.Property_ID
        WHERE b.Booking_ID = ?
          AND p.Owner_ID = ?
        """, bookingId, sellerId);

    if (bookingRows.isEmpty()) {
        return ResponseEntity.status(404)
                .body(Map.of("message", "Booking not found"));
    }

    long propertyId =
            ((Number) bookingRows.get(0).get("Property_ID"))
                    .longValue();

    // Lock property first, following the same lock order
    // used when creating a booking
    var propertyRows = jdbcTemplate.queryForList("""
        SELECT Status
        FROM property
        WHERE Property_ID = ?
          AND Owner_ID = ?
        FOR UPDATE
        """, propertyId, sellerId);

    if (propertyRows.isEmpty()) {
        return ResponseEntity.status(404)
                .body(Map.of("message", "Property not found"));
    }

    // Lock booking and recheck its current status
    var currentBookings = jdbcTemplate.queryForList("""
        SELECT Status
        FROM booking
        WHERE Booking_ID = ?
          AND Property_ID = ?
        FOR UPDATE
        """, bookingId, propertyId);

    if (currentBookings.isEmpty() ||
            !"PENDING".equals(
                currentBookings.get(0).get("Status"))) {
        return ResponseEntity.status(409)
                .body(Map.of(
                    "message", "Booking is no longer pending"));
    }

    String propertyStatus =
            (String) propertyRows.get(0).get("Status");

    if (newStatus.equals("CONFIRMED") &&
            !"AVAILABLE".equals(propertyStatus)) {
        return ResponseEntity.status(409)
                .body(Map.of(
                    "message", "Property is no longer available"));
    }

    // Update booking status
    jdbcTemplate.update("""
        UPDATE booking
        SET Status = ?
        WHERE Booking_ID = ?
          AND Status = 'PENDING'
        """, newStatus, bookingId);

    if (newStatus.equals("CONFIRMED")) {
        // Reserve the property
        jdbcTemplate.update("""
            UPDATE property
            SET Status = 'BOOKED'
            WHERE Property_ID = ?
              AND Status = 'AVAILABLE'
            """, propertyId);
    }

    return ResponseEntity.ok(Map.of(
        "message",
        newStatus.equals("CONFIRMED")
            ? "Booking confirmed successfully"
            : "Booking cancelled successfully"
    ));
}

/* GET LOGGED-IN BUYER BOOKINGS */

@GetMapping("/my")
public ResponseEntity<?> getMyBookings(HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
            !"BUYER".equals(role)) {

        return ResponseEntity.status(403)
                .body(Map.of(
                    "message", "Buyer login required"
                ));
    }

    long buyerId = ((Number) userId).longValue();

    String sql = """
        SELECT
            b.Booking_ID AS id,
            b.Property_ID AS propertyId,
            p.Title AS propertyTitle,
            b.Booking_Type AS bookingType,
            b.Booking_Amount AS bookingAmount,
            b.Booking_Date AS bookingDate,
            b.Status AS status,
            pay.Payment_ID AS paymentId,
pay.Payment_Status AS paymentStatus,
pay.Transaction_ID AS transactionId
        FROM booking b
        JOIN property p
            ON b.Property_ID = p.Property_ID
        LEFT JOIN payment pay
            ON pay.Payment_ID = (
                SELECT MAX(pay2.Payment_ID)
                FROM payment pay2
                WHERE pay2.Booking_ID = b.Booking_ID
            )
        WHERE b.Buyer_ID = ?
        ORDER BY b.Booking_Date DESC, b.Booking_ID DESC
        """;

    return ResponseEntity.ok(
        jdbcTemplate.queryForList(sql, buyerId)
    );
}


}
