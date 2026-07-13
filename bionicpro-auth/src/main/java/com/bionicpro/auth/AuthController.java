package com.bionicpro.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final Logger log = LoggerFactory.getLogger(ClassUtils.getUserClass(getClass()));


    @GetMapping("/api/user-info")
    public Map<String, Object> getUserInfo(
            @AuthenticationPrincipal OidcUser oidcUser,
            HttpServletRequest request
    ){
        Map<String, Object> response = new HashMap<>();

        if (oidcUser == null) {
            response.put("authenticated", false);

            String baseUrl = ServletUriComponentsBuilder.fromContextPath(request)
                    .toUriString();

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
}
