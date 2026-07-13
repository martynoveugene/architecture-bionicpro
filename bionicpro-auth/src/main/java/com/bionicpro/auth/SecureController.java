package com.bionicpro.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SecureController {

    @GetMapping("/")
    public String publicPage() {
        return "<h1>Главная страница</h1><p>" +
                "Доступ к отчету <a href='/reports'>/reports</a> через Keycloak (PKCE).</p>";
    }

    @GetMapping("/reports")
    public String reportPage(@AuthenticationPrincipal OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername();
        String email = oidcUser.getEmail();
        Map<String, Object> allClaims = oidcUser.getClaims();

        return String.format(
                "<h1>Отчет</h1>" +
                        "<p><b>Содержит данные пользователя из JWT.</b></p>" +
                        "<p>Username из Keycloak: %s</p>" +
                        "<p>Email из Keycloak: %s</p>" +
                        "<h3>Полный состав JWT Claims:</h3>" +
                        "<pre>%s</pre>" +
                        "<br><a href='/logout'>Выйти</a>",
                username, email, allClaims.toString()
        );
    }

}

