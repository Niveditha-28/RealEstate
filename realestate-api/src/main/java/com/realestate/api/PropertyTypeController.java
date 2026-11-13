
package com.realestate.api;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/property-types")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class PropertyTypeController {

    private final JdbcTemplate jdbcTemplate;

    public PropertyTypeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getPropertyTypes() {

        String sql = """
            SELECT
                Property_Type_ID AS id,
                Type_Name AS name
            FROM property_type
            ORDER BY Type_Name
            """;

        return jdbcTemplate.queryForList(sql);
    }
}
