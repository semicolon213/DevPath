package com.devpath.identity.config;

import com.devpath.identity.adapter.in.security.AbsoluteSessionTimeoutFilter;
import com.devpath.identity.adapter.in.security.GitHubOAuth2UserService;
import com.devpath.identity.adapter.in.security.NonPersistingOAuth2AuthorizedClientRepository;
import com.devpath.shared.api.ApiErrorResponse;
import com.devpath.shared.api.RequestIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(DevPathSecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        GitHubOAuth2UserService oAuth2UserService,
        NonPersistingOAuth2AuthorizedClientRepository authorizedClientRepository,
        AbsoluteSessionTimeoutFilter absoluteSessionTimeoutFilter,
        DevPathSecurityProperties properties,
        ObjectMapper objectMapper,
        @Value("${server.servlet.session.cookie.name:DEVPATH_SESSION}") String sessionCookieName
    ) throws Exception {
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookiePath("/");
        csrfRepository.setHeaderName("X-CSRF-TOKEN");

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.migrateSession())
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/internal/health",
                    "/oauth2/authorization/github",
                    "/login/oauth2/code/github",
                    "/api/v1/integrations/github/callback",
                    "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/csrf").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                .authorizedClientRepository(authorizedClientRepository)
                .successHandler((request, response, authentication) ->
                    response.sendRedirect(properties.loginSuccessUrl()))
                .failureHandler((request, response, exception) ->
                    response.sendRedirect(properties.loginFailureUrl()))
            )
            .logout(logout -> logout
                .logoutUrl("/api/v1/session/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(sessionCookieName)
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    writeError(
                        response,
                        objectMapper,
                        ApiErrorResponse.of(
                            "AUTHENTICATION_REQUIRED",
                            "Authentication is required.",
                            RequestIds.resolve(request)
                        )
                    );
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    writeError(
                        response,
                        objectMapper,
                        ApiErrorResponse.of(
                            "ACCESS_DENIED",
                            "The request is not permitted.",
                            RequestIds.resolve(request)
                        )
                    );
                })
            )
            .addFilterBefore(absoluteSessionTimeoutFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(DevPathSecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.frontendOrigin()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
            "Accept",
            "Content-Type",
            "Idempotency-Key",
            "X-CSRF-TOKEN",
            "X-Request-Id"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(
        HttpServletResponse response,
        ObjectMapper objectMapper,
        ApiErrorResponse error
    ) throws java.io.IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
