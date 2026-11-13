
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class RentalDAO {

    public static void createRentalAgreement(
            int bookingId,
            String startDate,
            String endDate,
            double monthlyRent,
            double securityDeposit) {

        String checkBooking =
            "SELECT Buyer_ID, Property_ID, Booking_Type, Status " +
            "FROM booking WHERE Booking_ID = ? FOR UPDATE";

        String checkPayment =
            "SELECT Payment_ID FROM payment " +
            "WHERE Booking_ID = ? AND Payment_Status = 'SUCCESSFUL'";

        String insertRental =
            "INSERT INTO rental_agreement " +
            "(Booking_ID, Tenant_ID, Property_ID, Start_Date, " +
            "End_Date, Monthly_Rent, Security_Deposit, Agreement_Status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";

        String updateProperty =
            "UPDATE property SET Status = 'RENTED' " +
            "WHERE Property_ID = ? AND Status = 'AVAILABLE'";

        String updateBooking =
            "UPDATE booking SET Status = 'COMPLETED' " +
            "WHERE Booking_ID = ?";

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (!end.isAfter(start) ||
                monthlyRent <= 0 || securityDeposit < 0) {
                System.out.println("Invalid rental details!");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                if (con == null) return;
                con.setAutoCommit(false);

                try {
                    int tenantId;
                    int propertyId;

                    try (PreparedStatement stmt =
                            con.prepareStatement(checkBooking)) {

                        stmt.setInt(1, bookingId);

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) {
                                System.out.println("Booking not found!");
                                con.rollback();
                                return;
                            }

                            tenantId = rs.getInt("Buyer_ID");
                            propertyId = rs.getInt("Property_ID");

                            if (!"RENT".equals(rs.getString("Booking_Type")) ||
                                !"CONFIRMED".equals(rs.getString("Status"))) {
                                System.out.println(
                                    "A confirmed RENT booking is required!");
                                con.rollback();
                                return;
                            }
                        }
                    }

                    try (PreparedStatement stmt =
                            con.prepareStatement(checkPayment)) {

                        stmt.setInt(1, bookingId);

                        try (ResultSet rs = stmt.executeQuery()) {
                            if (!rs.next()) {
                                System.out.println(
                                    "Successful payment not found!");
                                con.rollback();
                                return;
                            }
                        }
                    }

                    try (PreparedStatement stmt =
                            con.prepareStatement(updateProperty)) {

                        stmt.setInt(1, propertyId);

                        if (stmt.executeUpdate() != 1) {
                            throw new SQLException(
                                "Property is not available!");
                        }
                    }

                    try (PreparedStatement stmt =
                            con.prepareStatement(insertRental)) {

                        stmt.setInt(1, bookingId);
                        stmt.setInt(2, tenantId);
                        stmt.setInt(3, propertyId);
                        stmt.setDate(4, Date.valueOf(start));
                        stmt.setDate(5, Date.valueOf(end));
                        stmt.setBigDecimal(6, BigDecimal.valueOf(monthlyRent));
                        stmt.setBigDecimal(7, BigDecimal.valueOf(securityDeposit));

                        stmt.executeUpdate();
                    }

                    try (PreparedStatement stmt =
                            con.prepareStatement(updateBooking)) {

                        stmt.setInt(1, bookingId);
                        stmt.executeUpdate();
                    }

                    con.commit();

                    System.out.println("Rental agreement created successfully!");
                    System.out.println("Property status updated to RENTED.");
                    System.out.println("Booking status updated to COMPLETED.");

                } catch (SQLException e) {
                    con.rollback();
                    System.out.println("Rental transaction rolled back!");
                    System.out.println(e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Error creating rental agreement!");
            System.out.println(e.getMessage());
        }
    }
}

