
package com.realestate.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class TransactionController {

    private final JdbcTemplate jdbcTemplate;

    public TransactionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // FINALIZE A DEMO PROPERTY SALE
    @PostMapping("/sale/{bookingId}")
    @Transactional
    public ResponseEntity<?> finalizeSale(
            @PathVariable Long bookingId,
            @RequestHeader(
                value = "X-CSRF-Token",
                required = false
            ) String csrfToken,
            HttpSession session) {

        Object userId = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (!(userId instanceof Number) ||
                !"SELLER".equals(role)) {
            return ResponseEntity.status(403).body(
                Map.of("message", "Seller login required")
            );
        }

        String expectedToken =
            (String) session.getAttribute("csrfToken");

        if (expectedToken == null ||
                !expectedToken.equals(csrfToken)) {
            return ResponseEntity.status(403).body(
                Map.of("message", "Invalid CSRF token")
            );
        }

        if (bookingId == null || bookingId <= 0) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Invalid booking ID")
            );
        }

        long sellerId = ((Number) userId).longValue();

        // Lock booking and verify seller ownership
        var bookings = jdbcTemplate.queryForList("""
            SELECT
                b.Booking_ID,
                b.Buyer_ID,
                b.Property_ID,
                b.Booking_Amount,
                b.Booking_Type,
                b.Status,
                p.Status AS Property_Status
            FROM booking b
            JOIN property p
                ON b.Property_ID = p.Property_ID
            WHERE b.Booking_ID = ?
              AND p.Owner_ID = ?
            FOR UPDATE
            """, bookingId, sellerId);

        if (bookings.isEmpty()) {
            return ResponseEntity.status(404).body(
                Map.of("message", "Booking not found")
            );
        }

        Map<String, Object> booking = bookings.get(0);

        if (!"SALE".equals(booking.get("Booking_Type")) ||
            !"CONFIRMED".equals(booking.get("Status")) ||
            !"BOOKED".equals(booking.get("Property_Status"))) {

            return ResponseEntity.status(409).body(
                Map.of("message",
                    "Sale requires a confirmed booking and booked property")
            );
        }

        // Verify successful simulated payment
        Integer successfulPayments =
            jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM payment
                WHERE Booking_ID = ?
                  AND Payment_Status = 'SUCCESSFUL'
                """, Integer.class, bookingId);

        if (successfulPayments == null ||
                successfulPayments == 0) {
            return ResponseEntity.status(409).body(
                Map.of("message",
                    "Successful demo payment required")
            );
        }

        // Prevent duplicate sale transactions
        Integer existingSales =
            jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sale_transaction
                WHERE Booking_ID = ?
                """, Integer.class, bookingId);

        if (existingSales != null && existingSales > 0) {
            return ResponseEntity.status(409).body(
                Map.of("message",
                    "Sale transaction already exists")
            );
        }

        long buyerId =
            ((Number) booking.get("Buyer_ID")).longValue();

        long propertyId =
            ((Number) booking.get("Property_ID")).longValue();

        BigDecimal finalPrice =
            (BigDecimal) booking.get("Booking_Amount");

        String registrationNumber =
            "DEMO-SALE-" + UUID.randomUUID();

        // 1. Insert sale transaction
        jdbcTemplate.update("""
            INSERT INTO sale_transaction
            (Booking_ID, Buyer_ID, Property_ID,
             Sale_Date, Final_Price,
             Registration_Number, Sale_Status)
            VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED')
            """,
            bookingId,
            buyerId,
            propertyId,
            LocalDate.now(),
            finalPrice,
            registrationNumber
        );

        // 2. Complete booking
        int bookingUpdated = jdbcTemplate.update("""
            UPDATE booking
            SET Status = 'COMPLETED'
            WHERE Booking_ID = ?
              AND Status = 'CONFIRMED'
            """, bookingId);

        if (bookingUpdated != 1) {
            throw new IllegalStateException(
                "Unable to complete booking"
            );
        }

        // 3. Mark property SOLD
        int propertyUpdated = jdbcTemplate.update("""
            UPDATE property
            SET Status = 'SOLD'
            WHERE Property_ID = ?
              AND Status = 'BOOKED'
            """, propertyId);

        if (propertyUpdated != 1) {
            throw new IllegalStateException(
                "Unable to mark property sold"
            );
        }

        return ResponseEntity.ok(Map.of(
            "message", "Demo sale completed successfully",
            "registrationNumber", registrationNumber,
            "bookingStatus", "COMPLETED",
            "propertyStatus", "SOLD"
        ));
    }

/* CREATE DEMO RENTAL AGREEMENT */

@PostMapping("/rental/{bookingId}")
@Transactional
public ResponseEntity<?> createRentalAgreement(
        @PathVariable Long bookingId,
        @RequestBody Map<String, Object> request,
        @RequestHeader(
            value = "X-CSRF-Token",
            required = false
        ) String csrfToken,
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
            !"SELLER".equals(role)) {
        return ResponseEntity.status(403).body(
            Map.of("message", "Seller login required")
        );
    }

    String expectedToken =
        (String) session.getAttribute("csrfToken");

    if (expectedToken == null ||
            !Objects.equals(expectedToken, csrfToken)) {
        return ResponseEntity.status(403).body(
            Map.of("message", "Invalid CSRF token")
        );
    }

    if (bookingId == null || bookingId <= 0) {
        return ResponseEntity.badRequest().body(
            Map.of("message", "Invalid booking ID")
        );
    }

    // Validate rental agreement details
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal monthlyRent;
    BigDecimal securityDeposit;

    try {
        startDate = LocalDate.parse(
            request.get("startDate").toString()
        );

        endDate = LocalDate.parse(
            request.get("endDate").toString()
        );

        monthlyRent = new BigDecimal(
            request.get("monthlyRent").toString()
        );

        securityDeposit = new BigDecimal(
            request.get("securityDeposit").toString()
        );

    } catch (Exception e) {
        return ResponseEntity.badRequest().body(
            Map.of("message", "Invalid rental agreement details")
        );
    }

    if (!endDate.isAfter(startDate) ||
            monthlyRent.signum() <= 0 ||
            securityDeposit.signum() < 0 ||
            monthlyRent.scale() > 2 ||
            securityDeposit.scale() > 2 ||
            monthlyRent.precision() - monthlyRent.scale() > 10 ||
            securityDeposit.precision() - securityDeposit.scale() > 10) {

        return ResponseEntity.badRequest().body(
            Map.of("message", "Invalid dates or rental amounts")
        );
    }

    long sellerId = ((Number) userId).longValue();

    // Lock booking and verify property ownership
    var bookings = jdbcTemplate.queryForList("""
        SELECT
            b.Booking_ID,
            b.Buyer_ID,
            b.Property_ID,
            b.Booking_Type,
            b.Status,
            p.Status AS Property_Status
        FROM booking b
        JOIN property p
            ON b.Property_ID = p.Property_ID
        WHERE b.Booking_ID = ?
          AND p.Owner_ID = ?
        FOR UPDATE
        """, bookingId, sellerId);

    if (bookings.isEmpty()) {
        return ResponseEntity.status(404).body(
            Map.of("message", "Booking not found")
        );
    }

    Map<String, Object> booking = bookings.get(0);

    if (!"RENT".equals(booking.get("Booking_Type")) ||
            !"CONFIRMED".equals(booking.get("Status")) ||
            !"BOOKED".equals(booking.get("Property_Status"))) {

        return ResponseEntity.status(409).body(
            Map.of("message",
                "Rental requires a confirmed booking and booked property")
        );
    }

    // Require a successful demo payment
    Integer successfulPayments =
        jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM payment
            WHERE Booking_ID = ?
              AND Payment_Status = 'SUCCESSFUL'
            """, Integer.class, bookingId);

    if (successfulPayments == null ||
            successfulPayments == 0) {
        return ResponseEntity.status(409).body(
            Map.of("message", "Successful demo payment required")
        );
    }

    // Prevent duplicate rental agreements
    Integer existingAgreements =
        jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM rental_agreement
            WHERE Booking_ID = ?
            """, Integer.class, bookingId);

    if (existingAgreements != null &&
            existingAgreements > 0) {
        return ResponseEntity.status(409).body(
            Map.of("message", "Rental agreement already exists")
        );
    }

    long tenantId =
        ((Number) booking.get("Buyer_ID")).longValue();

    long propertyId =
        ((Number) booking.get("Property_ID")).longValue();

    // 1. Create rental agreement
    jdbcTemplate.update("""
        INSERT INTO rental_agreement
        (Booking_ID, Tenant_ID, Property_ID,
         Start_Date, End_Date,
         Monthly_Rent, Security_Deposit,
         Agreement_Status)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
        """,
        bookingId,
        tenantId,
        propertyId,
        startDate,
        endDate,
        monthlyRent,
        securityDeposit
    );

    // 2. Complete booking
    int bookingUpdated = jdbcTemplate.update("""
        UPDATE booking
        SET Status = 'COMPLETED'
        WHERE Booking_ID = ?
          AND Status = 'CONFIRMED'
        """, bookingId);

    if (bookingUpdated != 1) {
        throw new IllegalStateException(
            "Unable to complete rental booking"
        );
    }

    // 3. Mark property RENTED
    int propertyUpdated = jdbcTemplate.update("""
        UPDATE property
        SET Status = 'RENTED'
        WHERE Property_ID = ?
          AND Status = 'BOOKED'
        """, propertyId);

    if (propertyUpdated != 1) {
        throw new IllegalStateException(
            "Unable to mark property rented"
        );
    }

    return ResponseEntity.ok(Map.of(
        "message", "Demo rental agreement created successfully",
        "bookingStatus", "COMPLETED",
        "propertyStatus", "RENTED",
        "agreementStatus", "ACTIVE"
    ));
}

/* =====================================
   BUYER TRANSACTION HISTORY
===================================== */

@GetMapping("/my")
public ResponseEntity<?> getMyTransactions(
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
            !"BUYER".equals(role)) {
        return ResponseEntity.status(403).body(
            Map.of("message", "Buyer login required")
        );
    }

    long buyerId = ((Number) userId).longValue();

    // Completed sale transactions
    var sales = jdbcTemplate.queryForList("""
        SELECT
            s.Sale_ID AS transactionId,
            s.Booking_ID AS bookingId,
            p.Title AS propertyTitle,
            'SALE' AS transactionType,
            s.Sale_Date AS transactionDate,
            s.Final_Price AS amount,
            s.Registration_Number AS referenceNumber,
            s.Sale_Status AS status
        FROM sale_transaction s
        JOIN property p
            ON s.Property_ID = p.Property_ID
        WHERE s.Buyer_ID = ?
        """, buyerId);

    // Rental agreements
    var rentals = jdbcTemplate.queryForList("""
        SELECT
            r.Rental_ID AS transactionId,
            r.Booking_ID AS bookingId,
            p.Title AS propertyTitle,
            'RENT' AS transactionType,
            r.Start_Date AS startDate,
            r.End_Date AS endDate,
            r.Monthly_Rent AS monthlyRent,
            r.Security_Deposit AS securityDeposit,
            r.Agreement_Status AS status
        FROM rental_agreement r
        JOIN property p
            ON r.Property_ID = p.Property_ID
        WHERE r.Tenant_ID = ?
        """, buyerId);

    return ResponseEntity.ok(Map.of(
        "sales", sales,
        "rentals", rentals
    ));
}

/* =====================================
   SELLER TRANSACTION HISTORY
===================================== */

@GetMapping("/seller")
public ResponseEntity<?> getSellerTransactions(
        HttpSession session) {

    Object userId = session.getAttribute("userId");
    Object role = session.getAttribute("role");

    if (!(userId instanceof Number) ||
            !"SELLER".equals(role)) {

        return ResponseEntity.status(403).body(
            Map.of("message", "Seller login required")
        );
    }

    long sellerId = ((Number) userId).longValue();

    // ============================
    // COMPLETED PROPERTY SALES
    // ============================

    var sales = jdbcTemplate.queryForList("""
        SELECT
            s.Sale_ID AS transactionId,
            s.Booking_ID AS bookingId,
            p.Title AS propertyTitle,
            u.Name AS buyerName,
            u.Email AS buyerEmail,
            'SALE' AS transactionType,
            s.Sale_Date AS transactionDate,
            s.Final_Price AS amount,
            s.Registration_Number AS referenceNumber,
            s.Sale_Status AS status
        FROM sale_transaction s
        JOIN property p
            ON s.Property_ID = p.Property_ID
        JOIN users u
            ON s.Buyer_ID = u.User_ID
        WHERE p.Owner_ID = ?
        ORDER BY s.Sale_Date DESC, s.Sale_ID DESC
        """, sellerId);

    // ============================
    // RENTAL AGREEMENTS
    // ============================

    var rentals = jdbcTemplate.queryForList("""
        SELECT
            r.Rental_ID AS transactionId,
            r.Booking_ID AS bookingId,
            p.Title AS propertyTitle,
            u.Name AS tenantName,
            u.Email AS tenantEmail,
            'RENT' AS transactionType,
            r.Start_Date AS startDate,
            r.End_Date AS endDate,
            r.Monthly_Rent AS monthlyRent,
            r.Security_Deposit AS securityDeposit,
            r.Agreement_Status AS status
        FROM rental_agreement r
        JOIN property p
            ON r.Property_ID = p.Property_ID
        JOIN users u
            ON r.Tenant_ID = u.User_ID
        WHERE p.Owner_ID = ?
        ORDER BY r.Start_Date DESC, r.Rental_ID DESC
        """, sellerId);

    return ResponseEntity.ok(Map.of(
        "sales", sales,
        "rentals", rentals
    ));
}

}
