package com.bionicpro.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    private final Logger log = LoggerFactory.getLogger(ClassUtils.getUserClass(getClass()));

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final RestClient restClient;
    private final DefaultRefreshTokenTokenResponseClient refreshTokenClient = new DefaultRefreshTokenTokenResponseClient();

    public AuthController(
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${REPORTS_API_INTERNAL_URL:http://localhost:8082}") String reportsApiUrl
    ) {
        this.authorizedClientService = authorizedClientService;
        this.restClient = RestClient.builder()
                .baseUrl(reportsApiUrl)
                .build();
    }

    @GetMapping("/api/user-info")
    public Map<String, Object> getUserInfo(
            @AuthenticationPrincipal OidcUser oidcUser,
            HttpServletRequest request
    ){
        Map<String, Object> response = new HashMap<>();

        if (oidcUser == null) {
            response.put("authenticated", false);
            String baseUrl = ServletUriComponentsBuilder.fromContextPath(request).toUriString();
            String loginUrl = baseUrl + "/oauth2/authorization/keycloak";
            response.put("loginUrl", loginUrl);
            log.info("Ask to redirect to "+loginUrl);
            return response;
        }

        response.put("authenticated", true);
        response.put("name", oidcUser.getFullName());
        response.put("email", oidcUser.getEmail());
        response.put("roles", oidcUser.getClaim("roles"));
        return response;
    }

    @GetMapping("/reports")
    public List<String> proxyGetReportsList(OAuth2AuthenticationToken authentication) {
        String jwtToken = getAccessToken(authentication);

        return restClient.get()
                .uri("/reports")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});
    }

    @GetMapping("/reports/{day}")
    public ResponseEntity<Resource> proxyGetReportByDay(
            @PathVariable String day,
            OAuth2AuthenticationToken authentication
    ) {
        String jwtToken = getAccessToken(authentication);

        ResponseEntity<Resource> response = restClient.get()
                .uri("/reports/{day}", day)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .toEntity(Resource.class);

        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
    }

    private String getAccessToken(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Не удалось найти активный access token для текущей сессии.");
        }

        Instant expiresAt = client.getAccessToken().getExpiresAt();

        if (expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(10))) {
            log.info("Access token истек. Запускаем обновление через Refresh Token...");

            OAuth2RefreshToken refreshToken = client.getRefreshToken();
            if (refreshToken == null) {
                throw new IllegalStateException("Refresh token отсутствует. Требуется повторный вход.");
            }

            try {
                // запрос на обновление токена
                OAuth2RefreshTokenGrantRequest grantRequest = new OAuth2RefreshTokenGrantRequest(
                        client.getClientRegistration(),
                        client.getAccessToken(),
                        refreshToken
                );

                OAuth2AccessTokenResponse tokenResponse = refreshTokenClient.getTokenResponse(grantRequest);

                OAuth2AuthorizedClient updatedClient = new OAuth2AuthorizedClient(
                        client.getClientRegistration(),
                        client.getPrincipalName(),
                        tokenResponse.getAccessToken(),
                        tokenResponse.getRefreshToken() != null ? tokenResponse.getRefreshToken() : refreshToken
                );

                authorizedClientService.saveAuthorizedClient(updatedClient, authentication);
                client = updatedClient;
                log.info("Access token успешно обновлен в СУБД по цепочке Refresh.");

            } catch (Exception e) {
                log.error("Ошибка при обновлении токена через Keycloak: ", e);
                throw new IllegalStateException("Сессия устарела, требуется повторный вход.");
            }
        }

        return client.getAccessToken().getTokenValue();
    }
}
