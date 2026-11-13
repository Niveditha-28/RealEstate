
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EnquiryDAO {

    // Create a new enquiry
    public static void createEnquiry(
            int buyerId,
            int propertyId,
            String message) {

        String sql =
            "INSERT INTO enquiry " +
            "(Buyer_ID, Property_ID, Message, Enquiry_Date, Status) " +
            "VALUES (?, ?, ?, NOW(), 'OPEN')";

        if (message == null || message.trim().isEmpty()) {
            System.out.println("Enquiry message cannot be empty!");
            return;
        }

        try (
            Connection con = DBConnection.getConnection()
        ) {
            if (con == null) return;

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {

                pstmt.setInt(1, buyerId);
                pstmt.setInt(2, propertyId);
                pstmt.setString(3, message);

                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("Enquiry submitted successfully!");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error creating enquiry!");
            System.out.println(e.getMessage());
        }
    }

    // View all enquiries using JOIN
    public static void viewEnquiries() {

        String sql =
            "SELECT e.Enquiry_ID, u.Name AS Buyer_Name, " +
            "p.Title AS Property_Name, e.Message, " +
            "e.Enquiry_Date, e.Status " +
            "FROM enquiry e " +
            "JOIN users u ON e.Buyer_ID = u.User_ID " +
            "JOIN property p ON e.Property_ID = p.Property_ID " +
            "ORDER BY e.Enquiry_ID";

        try (
            Connection con = DBConnection.getConnection()
        ) {
            if (con == null) return;

            try (
                PreparedStatement pstmt = con.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
            ) {
                System.out.println("\n========== ENQUIRY LIST ==========");

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println(
                        "Enquiry ID: " + rs.getInt("Enquiry_ID")
                        + " | Buyer: " + rs.getString("Buyer_Name")
                        + " | Property: " + rs.getString("Property_Name")
                        + " | Message: " + rs.getString("Message")
                        + " | Date: " + rs.getTimestamp("Enquiry_Date")
                        + " | Status: " + rs.getString("Status")
                    );
                }

                if (!found) {
                    System.out.println("No enquiries found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving enquiries!");
            System.out.println(e.getMessage());
        }
    }
}

