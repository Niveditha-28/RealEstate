
package com.realestate.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController

@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)

@RequestMapping("/api/properties")
public class PropertyController {

    private final JdbcTemplate jdbcTemplate;

    public PropertyController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getAllProperties() {

        String sql = """
            SELECT
                p.Property_ID AS id,
                p.Title AS title,
                p.Description AS description,
                p.Address AS address,
                p.City AS city,
                p.Price AS price,
                p.Area AS area,
                p.Bedrooms AS bedrooms,
                p.Bathrooms AS bathrooms,
                p.Listing_Type AS type,
                p.Status AS status,
                u.Name AS sellerName,
                
COALESCE(
    (
        SELECT pi.Image_URL
        FROM property_image pi
        WHERE pi.Property_ID = p.Property_ID
        ORDER BY pi.Image_ID
        LIMIT 1
    ),
    '/images/apartment.jpg'
) AS image

            
FROM property p
LEFT JOIN users u
    ON p.Owner_ID = u.User_ID
ORDER BY p.Property_ID

            """;

        return jdbcTemplate.queryForList(sql);
    }
    
@GetMapping("/{propertyId}/images")
public ResponseEntity<?> getPropertyImages(
        @PathVariable Long propertyId) {

    Integer propertyCount = jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*)
        FROM property
        WHERE Property_ID = ?
        """,
        Integer.class,
        propertyId
    );

    if (propertyCount == null || propertyCount == 0) {
        return ResponseEntity.status(404).body(
            Map.of("message", "Property not found")
        );
    }

    String sql = """
        SELECT
            Image_ID AS id,
            Image_URL AS imageUrl,
            Image_Type AS imageType
        FROM property_image
        WHERE Property_ID = ?
        ORDER BY Image_ID
        """;

    return ResponseEntity.ok(
        jdbcTemplate.queryForList(sql, propertyId)
    );
}

}
