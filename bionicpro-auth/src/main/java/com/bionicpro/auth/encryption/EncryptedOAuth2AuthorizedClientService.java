package com.bionicpro.auth.encryption;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

@Service
public class EncryptedOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final InMemoryOAuth2AuthorizedClientService delegate;
    private final EncryptionService encryptionService;

    public EncryptedOAuth2AuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository,
            EncryptionService encryptionService) {
        this.delegate = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        this.encryptionService = encryptionService;
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        if (refreshToken != null) {
            String encryptedTokenValue = encryptionService.encrypt(refreshToken.getTokenValue());

            OAuth2RefreshToken encryptedRefreshToken = new OAuth2RefreshToken(
                    encryptedTokenValue,
                    refreshToken.getIssuedAt(),
                    refreshToken.getExpiresAt()
            );

            authorizedClient = new OAuth2AuthorizedClient(
                    authorizedClient.getClientRegistration(),
                    authorizedClient.getPrincipalName(),
                    authorizedClient.getAccessToken(),
                    encryptedRefreshToken
            );
        }

        delegate.saveAuthorizedClient(authorizedClient, principal);
    }

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {
        T client = delegate.loadAuthorizedClient(clientRegistrationId, principalName);
        if (client == null || client.getRefreshToken() == null) {
            return client;
        }

        String decryptedTokenValue = encryptionService.decrypt(client.getRefreshToken().getTokenValue());

        OAuth2RefreshToken decryptedRefreshToken = new OAuth2RefreshToken(
                decryptedTokenValue,
                client.getRefreshToken().getIssuedAt(),
                client.getRefreshToken().getExpiresAt()
        );

        return (T) new OAuth2AuthorizedClient(
                client.getClientRegistration(),
                client.getPrincipalName(),
                client.getAccessToken(),
                decryptedRefreshToken
        );
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        delegate.removeAuthorizedClient(clientRegistrationId, principalName);
    }
}
