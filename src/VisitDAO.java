
import java.sql.*;

public class VisitDAO {

    // Schedule a property visit
    public static void scheduleVisit(
            int buyerId,
            int propertyId,
            int agentId,
            String visitDate,
            String visitTime) {

        String sql =
            "INSERT INTO property_visit " +
            "(Buyer_ID, Property_ID, Agent_ID, Visit_Date, " +
            "Visit_Time, Status, Remarks) " +
            "VALUES (?, ?, ?, ?, ?, 'SCHEDULED', NULL)";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) return;

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {

                pstmt.setInt(1, buyerId);
                pstmt.setInt(2, propertyId);
                pstmt.setInt(3, agentId);
                pstmt.setDate(4, Date.valueOf(visitDate));
                pstmt.setTime(5, Time.valueOf(visitTime));

                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("Property visit scheduled successfully!");
                }
            }

        } catch (Exception e) {
            System.out.println("Error scheduling visit!");
            System.out.println(e.getMessage());
        }
    }

    // View all property visits
    public static void viewVisits() {

        String sql =
            "SELECT v.Visit_ID, u.Name AS Buyer_Name, " +
            "p.Title AS Property_Name, a.Name AS Agent_Name, " +
            "v.Visit_Date, v.Visit_Time, v.Status, v.Remarks " +
            "FROM property_visit v " +
            "JOIN users u ON v.Buyer_ID = u.User_ID " +
            "JOIN property p ON v.Property_ID = p.Property_ID " +
            "LEFT JOIN users a ON v.Agent_ID = a.User_ID " +
            "ORDER BY v.Visit_Date, v.Visit_Time";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) return;

            try (
                PreparedStatement pstmt = con.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
            ) {
                System.out.println("\n========== PROPERTY VISITS ==========");

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println(
                        "Visit ID: " + rs.getInt("Visit_ID")
                        + " | Buyer: " + rs.getString("Buyer_Name")
                        + " | Property: " + rs.getString("Property_Name")
                        + " | Agent: " + rs.getString("Agent_Name")
                        + " | Date: " + rs.getDate("Visit_Date")
                        + " | Time: " + rs.getTime("Visit_Time")
                        + " | Status: " + rs.getString("Status")
                        + " | Remarks: " + rs.getString("Remarks")
                    );
                }

                if (!found) {
                    System.out.println("No property visits found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error retrieving visits!");
            System.out.println(e.getMessage());
        }
    }

    // Update visit status
    public static void updateVisitStatus(
            int visitId,
            String status,
            String remarks) {

        status = status.toUpperCase();

        if (!status.equals("SCHEDULED") &&
            !status.equals("COMPLETED") &&
            !status.equals("CANCELLED")) {

            System.out.println("Invalid visit status!");
            return;
        }

        String sql =
            "UPDATE property_visit SET Status = ?, Remarks = ? " +
            "WHERE Visit_ID = ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) return;

            try (PreparedStatement pstmt = con.prepareStatement(sql)) {

                pstmt.setString(1, status);
                pstmt.setString(2, remarks);
                pstmt.setInt(3, visitId);

                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("Visit status updated successfully!");
                } else {
                    System.out.println("Visit ID not found!");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error updating visit!");
            System.out.println(e.getMessage());
        }
    }
}
