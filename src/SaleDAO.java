import java.math.BigDecimal;
import java.sql.*;

public class SaleDAO {

    
        public static void completeSale(
                int bookingId,
                double finalPrice,
                String registrationNumber) {
    
            String checkBooking =
                "SELECT Buyer_ID, Property_ID, Booking_Type, Status " +
                "FROM booking WHERE Booking_ID = ? FOR UPDATE";
    
            String checkProperty =
                "SELECT Status FROM property " +
                "WHERE Property_ID = ? FOR UPDATE";
    
            String checkPayment =
                "SELECT Payment_ID FROM payment " +
                "WHERE Booking_ID = ? " +
                "AND Payment_Status = 'SUCCESSFUL' LIMIT 1";
    
            String insertSale =
                "INSERT INTO sale_transaction " +
                "(Booking_ID, Buyer_ID, Property_ID, Sale_Date, " +
                "Final_Price, Registration_Number, Sale_Status) " +
                "VALUES (?, ?, ?, CURDATE(), ?, ?, 'COMPLETED')";
    
            String updateProperty =
                "UPDATE property SET Status = 'SOLD' " +
                "WHERE Property_ID = ? AND Status = 'AVAILABLE'";
    
            String updateBooking =
                "UPDATE booking SET Status = 'COMPLETED' " +
                "WHERE Booking_ID = ? AND Status = 'CONFIRMED'";
    
            if (finalPrice <= 0 ||
                registrationNumber == null ||
                registrationNumber.trim().isEmpty()) {
    
                System.out.println("Invalid sale details!");
                return;
            }
    
            try (Connection con = DBConnection.getConnection()) {
    
                if (con == null) return;
    
                con.setAutoCommit(false);
    
                try {
                    int buyerId;
                    int propertyId;
    
                    // 1. Validate and lock booking
                    try (PreparedStatement stmt =
                            con.prepareStatement(checkBooking)) {
    
                        stmt.setInt(1, bookingId);
    
                        try (ResultSet rs = stmt.executeQuery()) {
    
                            if (!rs.next()) {
                                throw new SQLException("Booking not found!");
                            }
    
                            buyerId = rs.getInt("Buyer_ID");
                            propertyId = rs.getInt("Property_ID");
    
                            if (!"SALE".equals(rs.getString("Booking_Type"))) {
                                throw new SQLException(
                                    "This is not a SALE booking!");
                            }
    
                            if (!"CONFIRMED".equals(rs.getString("Status"))) {
                                throw new SQLException(
                                    "Booking must be CONFIRMED!");
                            }
                        }
                    }
    
                    // 2. Validate and lock property
                    try (PreparedStatement stmt =
                            con.prepareStatement(checkProperty)) {
    
                        stmt.setInt(1, propertyId);
    
                        try (ResultSet rs = stmt.executeQuery()) {
    
                            if (!rs.next() ||
                                !"AVAILABLE".equals(rs.getString("Status"))) {
    
                                throw new SQLException(
                                    "Property is not available for sale!");
                            }
                        }
                    }
    
                    // 3. Verify successful payment
                    try (PreparedStatement stmt =
                            con.prepareStatement(checkPayment)) {
    
                        stmt.setInt(1, bookingId);
    
                        try (ResultSet rs = stmt.executeQuery()) {
    
                            if (!rs.next()) {
                                throw new SQLException(
                                    "Successful payment not found!");
                            }
                        }
                    }
    
                    // 4. Insert sale transaction
                    try (PreparedStatement stmt =
                            con.prepareStatement(insertSale)) {
    
                        stmt.setInt(1, bookingId);
                        stmt.setInt(2, buyerId);
                        stmt.setInt(3, propertyId);
                        stmt.setBigDecimal(
                            4, BigDecimal.valueOf(finalPrice));
                        stmt.setString(5, registrationNumber.trim());
    
                        stmt.executeUpdate();
                    }
    
                    // 5. Update property status
                    try (PreparedStatement stmt =
                            con.prepareStatement(updateProperty)) {
    
                        stmt.setInt(1, propertyId);
    
                        if (stmt.executeUpdate() != 1) {
                            throw new SQLException(
                                "Property status update failed!");
                        }
                    }
    
                    // 6. Update booking status
                    try (PreparedStatement stmt =
                            con.prepareStatement(updateBooking)) {
    
                        stmt.setInt(1, bookingId);
    
                        if (stmt.executeUpdate() != 1) {
                            throw new SQLException(
                                "Booking status update failed!");
                        }
                    }
    
                    // 7. Commit all changes
                    con.commit();
    
                    System.out.println("Sale completed successfully!");
                    System.out.println("Property status updated to SOLD.");
                    System.out.println("Booking status updated to COMPLETED.");
    
                } catch (Exception e) {
    
                    con.rollback();
    
                    System.out.println("Sale transaction rolled back!");
                    System.out.println(e.getMessage());
                }
    
            } catch (Exception e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    
    
}