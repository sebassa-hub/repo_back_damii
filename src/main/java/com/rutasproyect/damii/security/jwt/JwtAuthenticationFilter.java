package com.rutasproyect.damii.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Dependencias que crearemos después para manejar la criptografía y la base de
    // datos
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraer la cabecera Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 2. Descartar peticiones sin token válido
        // Todo token estándar de OAuth2/JWT comienza con la palabra "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Aislar el token (cortamos los primeros 7 caracteres: "Bearer ")
        jwt = authHeader.substring(7);

        // 4. Extraer el identificador del usuario (usualmente el email) desde el token
        userEmail = jwtService.extractUsername(jwt);

        // 5. Validar que el usuario exista y que no esté ya autenticado en este hilo
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Buscamos al usuario en nuestra base de datos (MySQL)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Si el token no ha expirado y pertenece a este usuario...
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Creamos el objeto de autenticación oficial de Spring
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Credenciales nulas porque ya nos validó el token criptográfico
                        userDetails.getAuthorities() // Aquí van los roles (USER, ADMIN)
                );

                // Le añadimos detalles adicionales de la petición (IP, Sesión web)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Guardamos al usuario en el Contexto de Seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Pasamos el control al siguiente filtro (o al Controller si todo salió
        // bien)
        filterChain.doFilter(request, response);
    }
}
