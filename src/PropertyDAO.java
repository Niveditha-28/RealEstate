import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
public class PropertyDAO {

    public static void viewAllProperties() {

        String sql = "SELECT Property_ID, Title, City, Price, Listing_Type, Status FROM property";

        try (
            Connection con = DBConnection.getConnection();
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {

            System.out.println("\n========== PROPERTY LIST ==========");

            while (rs.next()) {

                System.out.println(
                    "ID: " + rs.getInt("Property_ID")
                    + " | " + rs.getString("Title")
                    + " | City: " + rs.getString("City")
                    + " | Price: Rs." + rs.getBigDecimal("Price")
                    + " | Type: " + rs.getString("Listing_Type")
                    + " | Status: " + rs.getString("Status")
                );
            }

        } catch (Exception e) {
            System.out.println("Error retrieving properties!");
            e.printStackTrace();
        }
    }
    public static void searchByCity(String city) {

    String sql =
        "SELECT Property_ID, Title, City, Price, Listing_Type, Status " +
        "FROM property WHERE City = ?";

    try (
        Connection con = DBConnection.getConnection();
        PreparedStatement pstmt = con.prepareStatement(sql)
    ) {

        pstmt.setString(1, city);

        ResultSet rs = pstmt.executeQuery();

        System.out.println("\n===== PROPERTIES IN " +
                           city.toUpperCase() + " =====");

        boolean found = false;

        while (rs.next()) {

            found = true;

            System.out.println(
                "ID: " + rs.getInt("Property_ID")
                + " | " + rs.getString("Title")
                + " | Price: Rs." + rs.getBigDecimal("Price")
                + " | Type: " + rs.getString("Listing_Type")
                + " | Status: " + rs.getString("Status")
            );
        }

        if (!found) {
            System.out.println("No properties found.");
        }

    } catch (Exception e) {
        System.out.println("Error searching properties!");
        e.printStackTrace();
    }
}
public static void addProperty(
        int ownerId,
        int agentId,
        int propertyTypeId,
        String title,
        String description,
        String address,
        String city,
        double price,
        double area,
        int bedrooms,
        int bathrooms,
        String listingType) {

    String sql = "INSERT INTO property " +
            "(Owner_ID, Agent_ID, Property_Type_ID, Title, Description, " +
            "Address, City, Price, Area, Bedrooms, Bathrooms, " +
            "Listing_Type, Status, Listed_Date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'AVAILABLE', CURDATE())";

    try (
        Connection con = DBConnection.getConnection();
        PreparedStatement pstmt = con.prepareStatement(sql)
    ) {

        pstmt.setInt(1, ownerId);
        pstmt.setInt(2, agentId);
        pstmt.setInt(3, propertyTypeId);
        pstmt.setString(4, title);
        pstmt.setString(5, description);
        pstmt.setString(6, address);
        pstmt.setString(7, city);
        pstmt.setDouble(8, price);
        pstmt.setDouble(9, area);
        pstmt.setInt(10, bedrooms);
        pstmt.setInt(11, bathrooms);
        pstmt.setString(12, listingType);

        int rows = pstmt.executeUpdate();

        if (rows > 0) {
            System.out.println("Property added successfully!");
        }

    } catch (Exception e) {
        System.out.println("Error adding property!");
        System.out.println(e.getMessage());
    }
}
    public static void main(String[] args) {

        searchByCity("Chennai");

    }
}