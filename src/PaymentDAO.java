import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PaymentDAO {

    
public static void makePayment(
        int bookingId,
        double amount,
        String paymentMethod,
        String transactionId) {

    String checkBooking =
        "SELECT Status, Booking_Amount " +
        "FROM booking WHERE Booking_ID = ? FOR UPDATE";

    String checkPayment =
        "SELECT Payment_ID FROM payment " +
        "WHERE Booking_ID = ? " +
        "AND Payment_Status = 'SUCCESSFUL' LIMIT 1";

    String insertPayment =
        "INSERT INTO payment " +
        "(Booking_ID, Amount, Payment_Date, " +
        "Payment_Method, Transaction_ID, Payment_Status) " +
        "VALUES (?, ?, NOW(), ?, ?, 'SUCCESSFUL')";

    String updateBooking =
        "UPDATE booking SET Status = 'CONFIRMED' " +
        "WHERE Booking_ID = ? AND Status = 'PENDING'";

    paymentMethod = paymentMethod.trim().toUpperCase();

    if (!paymentMethod.equals("CASH") &&
        !paymentMethod.equals("CARD") &&
        !paymentMethod.equals("UPI") &&
        !paymentMethod.equals("BANK_TRANSFER")) {
        System.out.println("Invalid payment method!");
        return;
    }

    if (amount <= 0 ||
        transactionId == null ||
        transactionId.trim().isEmpty()) {
        System.out.println("Invalid payment details!");
        return;
    }

    try (Connection con = DBConnection.getConnection()) {

        if (con == null) return;

        con.setAutoCommit(false);

        try {
            try (PreparedStatement stmt =
                    con.prepareStatement(checkBooking)) {

                stmt.setInt(1, bookingId);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("Booking not found!");
                        con.rollback();
                        return;
                    }

                    if (!"PENDING".equals(rs.getString("Status"))) {
                        System.out.println(
                            "Only PENDING bookings can be paid!");
                        con.rollback();
                        return;
                    }

                    java.math.BigDecimal expected =
                        rs.getBigDecimal("Booking_Amount");

                    java.math.BigDecimal entered =
                        java.math.BigDecimal.valueOf(amount);

                    if (expected.compareTo(entered) != 0) {
                        System.out.println(
                            "Payment must match booking amount: Rs." +
                            expected);
                        con.rollback();
                        return;
                    }
                }
            }

            try (PreparedStatement stmt =
                    con.prepareStatement(checkPayment)) {

                stmt.setInt(1, bookingId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println(
                            "Successful payment already exists!");
                        con.rollback();
                        return;
                    }
                }
            }

            try (PreparedStatement stmt =
                    con.prepareStatement(insertPayment)) {

                stmt.setInt(1, bookingId);
                stmt.setBigDecimal(
                    2, java.math.BigDecimal.valueOf(amount));
                stmt.setString(3, paymentMethod);
                stmt.setString(4, transactionId.trim());

                stmt.executeUpdate();
            }

            try (PreparedStatement stmt =
                    con.prepareStatement(updateBooking)) {

                stmt.setInt(1, bookingId);

                if (stmt.executeUpdate() != 1) {
                    throw new SQLException(
                        "Booking status update failed!");
                }
            }

            con.commit();

            System.out.println("Payment completed successfully!");
            System.out.println("Booking status updated to CONFIRMED.");

        } catch (Exception e) {
            con.rollback();
            System.out.println("Payment rolled back!");
            System.out.println(e.getMessage());
        }

    } catch (Exception e) {
        System.out.println("Database error: " + e.getMessage());
    }
}

    public static void viewPayments() {

    String sql =
        "SELECT pay.Payment_ID, pay.Booking_ID, " +
        "u.Name AS Buyer_Name, p.Title AS Property_Name, " +
        "pay.Amount, pay.Payment_Date, pay.Payment_Method, " +
        "pay.Transaction_ID, pay.Payment_Status " +
        "FROM payment pay " +
        "JOIN booking b ON pay.Booking_ID = b.Booking_ID " +
        "JOIN users u ON b.Buyer_ID = u.User_ID " +
        "JOIN property p ON b.Property_ID = p.Property_ID " +
        "ORDER BY pay.Payment_ID";

    try (
        Connection con = DBConnection.getConnection();
        PreparedStatement pstmt = con.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()
    ) {

        System.out.println("\n================ PAYMENT LIST ================");

        while (rs.next()) {

            System.out.println(
                "Payment ID: " + rs.getInt("Payment_ID")
                + " | Booking ID: " + rs.getInt("Booking_ID")
                + " | Buyer: " + rs.getString("Buyer_Name")
                + " | Property: " + rs.getString("Property_Name")
                + " | Amount: Rs." + rs.getBigDecimal("Amount")
                + " | Method: " + rs.getString("Payment_Method")
                + " | Transaction: " + rs.getString("Transaction_ID")
                + " | Status: " + rs.getString("Payment_Status")
            );
        }

    } catch (Exception e) {
        System.out.println("Error retrieving payments!");
        System.out.println(e.getMessage());
    }
}
}
