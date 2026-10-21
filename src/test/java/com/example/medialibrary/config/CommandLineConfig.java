/**
 *
 * This class is for testing purposes only and would not be present in an actual production application
 *
 */

package com.example.medialibrary.config;

import com.example.medialibrary.model.User;
import com.example.medialibrary.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
@Profile("test")
public class CommandLineConfig {

    @Bean
    public CommandLineRunner createTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {

        return args -> {
            String username = "testuser";
            String password = "test";

            Optional<User> user = userRepository.findByUsername(username);

            if (user.isEmpty()) {
                User newUser = new User(username, passwordEncoder.encode(password), "USER");
                userRepository.save(newUser);
            }

            username = "testuser2";
            password = "test2";

            user = userRepository.findByUsername(username);

            if (user.isEmpty()) {
                User newUser = new User(username, passwordEncoder.encode(password), "USER");
                userRepository.save(newUser);
            }
        };
    }
}
