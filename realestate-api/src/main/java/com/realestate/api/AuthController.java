
package com.realestate.api;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = "http://localhost:5500",
    allowCredentials = "true"
)
public class AuthController {
    
@GetMapping("/csrf")
public ResponseEntity<?> getCsrfToken(
        HttpSession session) {

    Object userId = session.getAttribute("userId");

    if (userId == null) {
        return ResponseEntity.status(401).body(
            Map.of("message", "Not signed in")
        );
    }

    String token = (String) session.getAttribute("csrfToken");

    if (token == null) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);

        token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        session.setAttribute("csrfToken", token);
    }

    return ResponseEntity.ok(Map.of("csrfToken", token));
}


    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record SignupRequest(
            String name,
            String email,
            String phone,
            String password,
            String role
    ) {}

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody SignupRequest request) {

        if (request.name() == null ||
            request.name().isBlank() ||
            request.email() == null ||
            request.email().isBlank() ||
            request.password() == null ||
            request.password().length() < 8 ||
            request.role() == null) {

            return ResponseEntity.badRequest().body(
                Map.of("message", "Invalid registration details")
            );
        }

        String role = request.role().toUpperCase();

        if (!role.equals("BUYER") && !role.equals("SELLER")) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Select BUYER or SELLER")
            );
        }

        String email = request.email().trim().toLowerCase();
        String hashedPassword =
                passwordEncoder.encode(request.password());

        try {
            String sql = """
                INSERT INTO users
                    (Name, Email, Phone, Password_Hash,
                     User_Type, Created_Date)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;

            jdbcTemplate.update(
                sql,
                request.name().trim(),
                email,
                request.phone(),
                hashedPassword,
                role
            );

            return ResponseEntity.status(201).body(
                Map.of("message", "Account created successfully")
            );

        } catch (DuplicateKeyException ex) {
            return ResponseEntity.status(409).body(
                Map.of("message", "Email already registered")
            );
        }
        
    }
    
public record LoginRequest(
        String email,
        String password
) {}

@PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody LoginRequest request,
        HttpSession session,
        HttpServletRequest httpRequest) {

    if (request.email() == null ||
        request.email().isBlank() ||
        request.password() == null ||
        request.password().isBlank()) {

        return ResponseEntity.badRequest().body(
            Map.of("message", "Email and password are required")
        );
    }

    String sql = """
        SELECT User_ID, Name, Email,
               Password_Hash, User_Type
        FROM users
        WHERE Email = ?
        """;

    var users = jdbcTemplate.queryForList(
        sql,
        request.email().trim().toLowerCase()
    );

    if (users.isEmpty()) {
        return ResponseEntity.status(401).body(
            Map.of("message", "Invalid email or password")
        );
    }

    Map<String, Object> user = users.get(0);

    String storedHash =
        (String) user.get("Password_Hash");

    if (storedHash == null ||
        !passwordEncoder.matches(
            request.password(), storedHash)) {

        return ResponseEntity.status(401).body(
            Map.of("message", "Invalid email or password")
        );
    }

    String role = user.get("User_Type").toString();

if (!role.equals("BUYER") && !role.equals("SELLER")) {
    return ResponseEntity.status(403).body(
        Map.of("message", "Unsupported account role")
    );
}
HttpSession existingSession =
        httpRequest.getSession(false);

if (existingSession != null) {
    httpRequest.changeSessionId();
} else {
    session = httpRequest.getSession(true);
}
session.setAttribute("userId", user.get("User_ID"));
session.setAttribute("name", user.get("Name"));
session.setAttribute("role", role);

return ResponseEntity.ok(
    Map.of(
        "message", "Login successful",
        "name", user.get("Name"),
        "role", role
    )
);
}

@GetMapping("/me")
public ResponseEntity<?> currentUser(HttpSession session) {

    Object userId = session.getAttribute("userId");

    if (userId == null) {
        return ResponseEntity.status(401).body(
            Map.of("message", "Not signed in")
        );
    }

    return ResponseEntity.ok(
        Map.of(
            "userId", userId,
            "name", session.getAttribute("name"),
            "role", session.getAttribute("role")
        )
    );
}

@PostMapping("/logout")
public ResponseEntity<?> logout(HttpSession session) {

    session.invalidate();

    return ResponseEntity.ok(
        Map.of("message", "Logged out successfully")
    );
}

}
