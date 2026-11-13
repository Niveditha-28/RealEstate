
package com.realestate.api;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class PaymentController {

    private final JdbcTemplate jdbcTemplate;

    public PaymentController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record PaymentRequest(
        Long bookingId,
        String paymentMethod
    ) {}

    @PostMapping
    @Transactional
    public ResponseEntity<?> recordPayment(
            @RequestBody PaymentRequest request,
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

        if (request == null ||
                request.bookingId() == null ||
                request.bookingId() <= 0 ||
                request.paymentMethod() == null) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Invalid payment details"
                    ));
        }

        String method =
                request.paymentMethod().trim().toUpperCase();

        if (!method.equals("CASH") &&
            !method.equals("CARD") &&
            !method.equals("UPI") &&
            !method.equals("BANK_TRANSFER")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Invalid payment method"
                    ));
        }

        long buyerId = ((Number) userId).longValue();

        // Lock booking to prevent concurrent duplicate payments
        var bookings = jdbcTemplate.queryForList("""
            SELECT Booking_Amount, Status
            FROM booking
            WHERE Booking_ID = ?
              AND Buyer_ID = ?
            FOR UPDATE
            """, request.bookingId(), buyerId);

        if (bookings.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                        "message", "Booking not found"
                    ));
        }

        Map<String, Object> booking = bookings.get(0);

        if (!"CONFIRMED".equals(booking.get("Status"))) {
            return ResponseEntity.status(409)
                    .body(Map.of(
                        "message", "Booking must be confirmed"
                    ));
        }

        BigDecimal amount =
                (BigDecimal) booking.get("Booking_Amount");

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "message", "Invalid booking amount"
                    ));
        }

        // Prevent duplicate payment records
        Integer existing = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM payment
            WHERE Booking_ID = ?
              AND Payment_Status IN ('PENDING', 'SUCCESSFUL')
            """, Integer.class, request.bookingId());

        if (existing != null && existing > 0) {
            return ResponseEntity.status(409)
                    .body(Map.of(
                        "message",
                        "A payment record already exists for this booking"
                    ));
        }

        // Unique reference for demonstration only
        String transactionId =
                "DEMO-" + UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO payment
                (Booking_ID, Amount, Payment_Date,
                 Payment_Method, Transaction_ID,
                 Payment_Status)
            VALUES (?, ?, NOW(), ?, ?, 'PENDING')
            """,
            request.bookingId(),
            amount,
            method,
            transactionId
        );

        return ResponseEntity.status(201).body(Map.of(
            "message", "Demo payment record created successfully",
            "transactionId", transactionId,
            "paymentStatus", "PENDING"
        ));
    }

/* COMPLETE SIMULATED PAYMENT */

@PutMapping("/{paymentId}/complete")
@Transactional
public ResponseEntity<?> completeDemoPayment(
        @PathVariable Long paymentId,
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
                .body(Map.of("message", "Buyer login required"));
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

    if (paymentId == null || paymentId <= 0) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid payment ID"));
    }

    long buyerId = ((Number) userId).longValue();

    // Verify that payment belongs to the buyer
    var paymentRows = jdbcTemplate.queryForList("""
        SELECT pay.Booking_ID
        FROM payment pay
        JOIN booking b ON pay.Booking_ID = b.Booking_ID
        WHERE pay.Payment_ID = ?
          AND b.Buyer_ID = ?
        """, paymentId, buyerId);

    if (paymentRows.isEmpty()) {
        return ResponseEntity.status(404)
                .body(Map.of("message", "Payment not found"));
    }

    long bookingId =
            ((Number) paymentRows.get(0).get("Booking_ID"))
                    .longValue();

    // Lock booking before updating payment
    var bookingRows = jdbcTemplate.queryForList("""
        SELECT Status
        FROM booking
        WHERE Booking_ID = ?
          AND Buyer_ID = ?
        FOR UPDATE
        """, bookingId, buyerId);

    if (bookingRows.isEmpty() ||
            !"CONFIRMED".equals(bookingRows.get(0).get("Status"))) {
        return ResponseEntity.status(409)
                .body(Map.of("message", "Booking is not confirmed"));
    }

    // Only a pending payment can be completed
    int updated = jdbcTemplate.update("""
        UPDATE payment
        SET Payment_Status = 'SUCCESSFUL'
        WHERE Payment_ID = ?
          AND Booking_ID = ?
          AND Payment_Status = 'PENDING'
        """, paymentId, bookingId);

    if (updated == 0) {
        return ResponseEntity.status(409)
                .body(Map.of(
                    "message", "Payment is not pending"
                ));
    }

    return ResponseEntity.ok(Map.of(
        "message", "Demo payment marked successful",
        "paymentStatus", "SUCCESSFUL"
    ));
}

}
