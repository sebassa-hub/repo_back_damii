package com.rutasproyect.damii.security.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rutasproyect.damii.model.User;
import com.rutasproyect.damii.repository.UserRepository;

@Configuration
public class AdminSeeder {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@gmail.com";
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setName("Administrador");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123")); // Contraseña inicial: admin123
                admin.setRole("ADMIN");
                admin.setIsActive(true);
                userRepository.save(admin);
                System.out.println("Usuario administrador creado exitosamente: admin@gmail.com / admin123");
            }
        };
    }
}
