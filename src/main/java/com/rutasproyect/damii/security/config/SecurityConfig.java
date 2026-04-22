package com.rutasproyect.damii.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.rutasproyect.damii.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthFilter;

    // Inyectamos nuestro filtro personalizado que leerá el JWT del iPhone/Web
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Desactivar CSRF: Es obligatorio en APIs REST porque no usamos cookies
                // tradicionales
                .csrf(csrf -> csrf.disable())

                // 2. Stateless: Le decimos a Spring que no guarde sesiones en memoria.
                // Cada petición debe traer su propio JWT.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Reglas de Acceso (Autorización)
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (Login para que puedan obtener el token + archivos estáticos panel)
                        .requestMatchers("/api/v1/auth/**", "/admin/**", "/css/**", "/js/**").permitAll()

                        // Rutas del Panel Web: Estrictamente para administradores
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Rutas de la App iOS (Públicas)
                        .requestMatchers("/api/v1/mobile/public/**").permitAll()

                        // Rutas de la App iOS (Privadas: Favoritos, Comentarios, Reportes)
                        .requestMatchers("/api/v1/mobile/private/**").hasAnyRole("USER", "ADMIN")

                        // Cualquier otra petición no mapeada arriba, se bloquea por defecto
                        .anyRequest().authenticated())

                // 4. Inserción del Filtro
                // Colocamos nuestro filtro JWT ANTES del filtro estándar de usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
