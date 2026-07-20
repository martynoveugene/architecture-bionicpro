package com.bionicpro.auth;

import com.bionicpro.auth.encryption.EncryptedOAuth2AuthorizedClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final EncryptedOAuth2AuthorizedClientService authorizedClientService;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository,
                          EncryptedOAuth2AuthorizedClientService authorizedClientService
    ){
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientService = authorizedClientService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        // Spring создаст сессию при OAuth-логине
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // Защита от фиксации сессии: при логине старый ID сессии уничтожается, создается новый
                        .sessionFixation(sessionFixation -> sessionFixation.newSession())
                        // один пользователь — одна активная сессия одновременно
                        .maximumSessions(1)
                )
                .authorizeHttpRequests(auth -> auth
                        // для проверки сессии и информации о юзере
                        .requestMatchers("/api/user-info").permitAll()
                        // Это будет проксировано в API-сервис
                        .requestMatchers("/reports/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(  // Authorization Code Flow + PKCE + Secret
                        oauth2 -> oauth2
                                .authorizedClientService(authorizedClientService)
                                .defaultSuccessUrl("http://localhost:3000/", true)
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                );

        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("http://localhost:3000");
        return oidcLogoutSuccessHandler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Value("${BPAPI_KEYCLOAK_INTERNAL_URI}")
    private String internalUri;

    @Value("${BPAPI_KEYCLOAK_REALM:reports-realm}")
    private String realm;

    @Bean
    public JwtDecoder jwtDecoder() {

        String jwkSetUri = String.format("%s/realms/%s/protocol/openid-connect/certs", internalUri, realm);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        jwtDecoder.setJwtValidator(token -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());

        return jwtDecoder;
    }
}
