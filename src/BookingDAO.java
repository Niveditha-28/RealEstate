import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BookingDAO {

    
public static void createBooking(
        int buyerId,
        int propertyId,
        String bookingType,
        double bookingAmount) {

    String checkProperty =
        "SELECT Owner_ID, Status, Listing_Type " +
        "FROM property WHERE Property_ID = ? FOR UPDATE";

    String checkBooking =
        "SELECT Booking_ID FROM booking " +
        "WHERE Property_ID = ? " +
        "AND Status IN ('PENDING', 'CONFIRMED') LIMIT 1";

    String insertBooking =
        "INSERT INTO booking " +
        "(Buyer_ID, Property_ID, Booking_Date, " +
        "Booking_Type, Booking_Amount, Status) " +
        "VALUES (?, ?, CURDATE(), ?, ?, 'PENDING')";

    bookingType = bookingType.trim().toUpperCase();

    if (!bookingType.equals("SALE") &&
        !bookingType.equals("RENT")) {
        System.out.println("Invalid booking type!");
        return;
    }

    if (bookingAmount <= 0) {
        System.out.println("Booking amount must be positive!");
        return;
    }

    try (Connection con = DBConnection.getConnection()) {

        if (con == null) return;

        con.setAutoCommit(false);

        try {
            try (PreparedStatement stmt =
                    con.prepareStatement(checkProperty)) {

                stmt.setInt(1, propertyId);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("Property not found!");
                        con.rollback();
                        return;
                    }

                    if (!"AVAILABLE".equals(rs.getString("Status"))) {
                        System.out.println("Property is not available!");
                        con.rollback();
                        return;
                    }

                    if (!bookingType.equals(rs.getString("Listing_Type"))) {
                        System.out.println("Booking type does not match!");
                        con.rollback();
                        return;
                    }

                    if (buyerId == rs.getInt("Owner_ID")) {
                        System.out.println("Owner cannot book own property!");
                        con.rollback();
                        return;
                    }
                }
            }

            try (PreparedStatement stmt =
                    con.prepareStatement(checkBooking)) {

                stmt.setInt(1, propertyId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println(
                            "Property already has an active booking!");
                        con.rollback();
                        return;
                    }
                }
            }

            try (PreparedStatement stmt =
                    con.prepareStatement(
                        insertBooking,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {

                stmt.setInt(1, buyerId);
                stmt.setInt(2, propertyId);
                stmt.setString(3, bookingType);
                stmt.setBigDecimal(
                    4, java.math.BigDecimal.valueOf(bookingAmount));

                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        System.out.println(
                            "Booking created! Booking ID: " +
                            keys.getInt(1));
                    }
                }
            }

            con.commit();

        } catch (Exception e) {
            con.rollback();
            System.out.println("Booking failed: " + e.getMessage());
        }

    } catch (Exception e) {
        System.out.println("Database error: " + e.getMessage());
    }
}

    public static void viewBookings() {

    String sql =
        "SELECT b.Booking_ID, u.Name AS Buyer_Name, " +
        "p.Title AS Property_Name, b.Booking_Date, " +
        "b.Booking_Type, b.Booking_Amount, b.Status " +
        "FROM booking b " +
        "JOIN users u ON b.Buyer_ID = u.User_ID " +
        "JOIN property p ON b.Property_ID = p.Property_ID " +
        "ORDER BY b.Booking_ID";

    try (
        Connection con = DBConnection.getConnection();
        PreparedStatement pstmt = con.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()
    ) {

        System.out.println("\n================ BOOKING LIST ================");

        while (rs.next()) {

            System.out.println(
                "Booking ID: " + rs.getInt("Booking_ID")
                + " | Buyer: " + rs.getString("Buyer_Name")
                + " | Property: " + rs.getString("Property_Name")
                + " | Date: " + rs.getDate("Booking_Date")
                + " | Type: " + rs.getString("Booking_Type")
                + " | Amount: Rs." + rs.getBigDecimal("Booking_Amount")
                + " | Status: " + rs.getString("Status")
            );
        }

    } catch (Exception e) {
        System.out.println("Error retrieving bookings!");
        System.out.println(e.getMessage());
    }
}
}
