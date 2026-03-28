package nto.infrastructure.services;

import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthTokensDto;
import nto.application.dto.UserDto;
import nto.application.interfaces.repositories.UserRepository;
import nto.application.interfaces.services.AuthService;
import nto.core.entities.RefreshTokenEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.UserEntity;
import nto.core.utils.ServerGroupDefaults;
import nto.core.utils.exceptions.InvalidRefreshTokenException;
import nto.infrastructure.repositories.JpaRefreshTokenRepository;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int REFRESH_TOKEN_BYTES = 64;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final JpaServerGroupRepository serverGroupRepository;
    private final JpaRefreshTokenRepository refreshTokenRepository;

    @Value("${nto.app.jwtRefreshExpirationMs:2592000000}")
    private long jwtRefreshExpirationMs;

    @Value("${nto.app.jwtSecret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public AuthTokensDto register(UserDto dto) {
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }

        var user = UserEntity.builder()
            .username(dto.username())
            .password(passwordEncoder.encode(dto.password()))
            .build();

        userRepository.save(user);
        ensureDefaultGroup(user);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthTokensDto login(AuthRequestDto dto) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
        );

        UserEntity user = userRepository.findByUsername(dto.username())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthTokensDto refresh(String refreshToken) {
        RefreshTokenEntity stored = getValidRefreshToken(refreshToken);
        UserEntity user = stored.getUser();

        LocalDateTime now = LocalDateTime.now();
        revokeAllActiveTokens(user.getId(), now);

        stored.setLastUsedAt(now);
        stored.setRevokedAt(now);
        refreshTokenRepository.save(stored);

        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        UserEntity user = resolveUserFromRefreshToken(refreshToken);
        if (user == null && accessToken != null) {
            user = resolveUserFromAccessToken(accessToken);
        }

        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        revokeAllActiveTokens(user.getId(), now);
        userRepository.save(user);
    }

    private void ensureDefaultGroup(UserEntity user) {
        serverGroupRepository.findByOwnerUsernameAndName(user.getUsername(),
                ServerGroupDefaults.DEFAULT_GROUP_NAME)
            .orElseGet(() -> serverGroupRepository.save(ServerGroupEntity.builder()
                .name(ServerGroupDefaults.DEFAULT_GROUP_NAME)
                .owner(user)
                .build()));
    }

    private AuthTokensDto issueTokens(UserEntity user) {
        LocalDateTime now = LocalDateTime.now();
        revokeAllActiveTokens(user.getId(), now);

        String refreshToken = createRefreshToken(user, now);
        String accessToken = jwtUtils.generateAccessToken(user.getUsername());

        return new AuthTokensDto(accessToken, refreshToken, jwtUtils.getAccessTokenTtlSeconds());
    }

    private RefreshTokenEntity getValidRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        LocalDateTime now = LocalDateTime.now();
        if (stored.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException("Refresh token is revoked");
        }
        if (stored.getExpiresAt().isBefore(now)) {
            stored.setRevokedAt(now);
            refreshTokenRepository.save(stored);
            throw new InvalidRefreshTokenException("Refresh token is expired");
        }

        return stored;
    }

    private void revokeAllActiveTokens(Long userId, LocalDateTime now) {
        refreshTokenRepository.revokeAllActiveByUserId(userId, now);
    }

    private String createRefreshToken(UserEntity user, LocalDateTime now) {
        String rawToken = generateRefreshToken();
        String tokenHash = hashToken(rawToken);

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
            .tokenHash(tokenHash)
            .user(user)
            .expiresAt(now.plus(Duration.ofMillis(jwtRefreshExpirationMs)))
            .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private String generateRefreshToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Decoders.BASE64.decode(jwtSecret), "HmacSHA256"));
            byte[] digest = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }

    private UserEntity resolveUserFromRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        String tokenHash = hashToken(refreshToken);
        return refreshTokenRepository.findByTokenHash(tokenHash)
            .map(RefreshTokenEntity::getUser)
            .orElse(null);
    }

    private UserEntity resolveUserFromAccessToken(String accessToken) {
        try {
            String username = jwtUtils.extractUsernameAllowExpired(accessToken);
            if (username == null) {
                return null;
            }
            return userRepository.findByUsername(username).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

}
