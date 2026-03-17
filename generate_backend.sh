#!/bin/bash
BASE=/home/claude/uzima-mvp/backend/src/main/java/com/uzima

# Controllers
cat > $BASE/controller/AuthController.java << 'EOF'
package com.uzima.controller;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @PostMapping("/register")
    public String register(@RequestBody String body) {
        return "{\"message\": \"Register endpoint\"}";
    }
    @PostMapping("/login")
    public String login(@RequestBody String body) {
        return "{\"message\": \"Login endpoint\"}";
    }
}
EOF

cat > $BASE/controller/MessageController.java << 'EOF'
package com.uzima.controller;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {
    @GetMapping
    public String getMessages() {
        return "{\"messages\": []}";
    }
    @PostMapping
    public String sendMessage(@RequestBody String body) {
        return "{\"message\": \"sent\"}";
    }
}
EOF

# Repositories
cat > $BASE/repository/UserRepository.java << 'EOF'
package com.uzima.repository;
import com.uzima.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);
}
EOF

# Config
cat > $BASE/config/WebConfig.java << 'EOF'
package com.uzima.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
EOF

echo "Backend files generated successfully!"
