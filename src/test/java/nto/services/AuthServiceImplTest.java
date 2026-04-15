package nto.services;

import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthTokensDto;
import nto.application.dto.UserDto;
import nto.application.interfaces.repositories.UserRepository;
import nto.core.entities.RefreshTokenEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.UserEntity;
import nto.core.utils.exceptions.DuplicateUsernameException;
import nto.core.utils.exceptions.InvalidRefreshTokenException;
import nto.infrastructure.repositories.JpaRefreshTokenRepository;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.security.JwtUtils;
import nto.infrastructure.services.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JpaServerGroupRepository serverGroupRepository;
    @Mock
    private JpaRefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=");
        ReflectionTestUtils.setField(authService, "jwtRefreshExpirationMs", 60000L);
    }

    @Test
    void registerShouldThrowWhenUsernameAlreadyExists() {
        UserDto dto = new UserDto(null, "alice", "pw");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(UserEntity.builder().build()));

        assertThrows(DuplicateUsernameException.class, () -> authService.register(dto));
    }

    @Test
    void registerShouldCreateUserDefaultGroupAndIssueTokens() {
        UserDto dto = new UserDto(null, "alice", "pw");
        UserEntity savedUser = UserEntity.builder().id(1L).username("alice").password("enc").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("enc");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(serverGroupRepository.findByOwnerUsernameAndName(anyString(), anyString()))
            .thenReturn(Optional.empty());
        when(serverGroupRepository.save(any(ServerGroupEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateAccessToken("alice")).thenReturn("access-token");
        when(jwtUtils.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokensDto result = authService.register(dto);

        assertEquals("access-token", result.accessToken());
        assertEquals(900L, result.expiresIn());
        assertNotNull(result.refreshToken());
        assertTrue(!result.refreshToken().isBlank());

        ArgumentCaptor<RefreshTokenEntity> tokenCaptor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertEquals("alice", tokenCaptor.getValue().getUser().getUsername());
        assertNotNull(tokenCaptor.getValue().getExpiresAt());

        verify(serverGroupRepository).save(any(ServerGroupEntity.class));
        verify(refreshTokenRepository).revokeAllActiveByUserId(any(), any(LocalDateTime.class));
    }

    @Test
    void loginShouldAuthenticateAndIssueTokens() {
        AuthRequestDto request = new AuthRequestDto("alice", "pw");
        UserEntity user = UserEntity.builder().id(3L).username("alice").password("enc").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtUtils.generateAccessToken("alice")).thenReturn("access");
        when(jwtUtils.getAccessTokenTtlSeconds()).thenReturn(600L);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthTokensDto result = authService.login(request);

        assertEquals("access", result.accessToken());
        verify(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(3L), any(LocalDateTime.class));
    }

    @Test
    void loginShouldThrowWhenAuthenticatedUserMissing() {
        AuthRequestDto request = new AuthRequestDto("alice", "pw");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> authService.login(request));
    }

    @Test
    void refreshShouldThrowWhenTokenMissing() {
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh(null));
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh(" "));
    }

    @Test
    void refreshShouldThrowWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh("raw-token"));
    }

    @Test
    void refreshShouldThrowWhenTokenHashingFails() {
        ReflectionTestUtils.setField(authService, "jwtSecret", "%%%");

        assertThrows(IllegalStateException.class, () -> authService.refresh("raw-token"));
    }

    @Test
    void refreshShouldThrowWhenTokenAlreadyRevoked() {
        UserEntity user = UserEntity.builder().id(5L).username("alice").build();
        RefreshTokenEntity revoked = RefreshTokenEntity.builder()
            .tokenHash("h")
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(1))
            .revokedAt(LocalDateTime.now().minusSeconds(5))
            .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh("raw-token"));
    }

    @Test
    void refreshShouldRevokeExpiredTokenAndThrow() {
        UserEntity user = UserEntity.builder().id(5L).username("alice").build();
        RefreshTokenEntity expired = RefreshTokenEntity.builder()
            .tokenHash("h")
            .user(user)
            .expiresAt(LocalDateTime.now().minusSeconds(5))
            .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh("raw-token"));
        verify(refreshTokenRepository).save(expired);
        assertNotNull(expired.getRevokedAt());
    }

    @Test
    void refreshShouldRotateTokenAndIssueNewTokens() {
        UserEntity user = UserEntity.builder().id(7L).username("alice").build();
        RefreshTokenEntity stored = RefreshTokenEntity.builder()
            .tokenHash("h")
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateAccessToken("alice")).thenReturn("access-new");
        when(jwtUtils.getAccessTokenTtlSeconds()).thenReturn(500L);

        AuthTokensDto result = authService.refresh("raw-token");

        assertEquals("access-new", result.accessToken());
        verify(refreshTokenRepository, times(2))
            .revokeAllActiveByUserId(eq(7L), any(LocalDateTime.class));
        assertNotNull(stored.getLastUsedAt());
        assertNotNull(stored.getRevokedAt());
    }

    @Test
    void logoutShouldResolveUserFromRefreshTokenFirst() {
        UserEntity user = UserEntity.builder().id(8L).username("alice").build();
        RefreshTokenEntity stored = RefreshTokenEntity.builder()
            .tokenHash("h")
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        authService.logout("refresh", "access");

        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(8L), any(LocalDateTime.class));
        verify(userRepository).save(user);
        verify(jwtUtils, never()).extractUsernameAllowExpired(anyString());
    }

    @Test
    void logoutShouldFallbackToAccessTokenWhenRefreshTokenUnknown() {
        UserEntity user = UserEntity.builder().id(9L).username("alice").build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        when(jwtUtils.extractUsernameAllowExpired("access")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        authService.logout("refresh", "access");

        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(9L), any(LocalDateTime.class));
        verify(userRepository).save(user);
    }

    @Test
    void logoutShouldDoNothingWhenRefreshTokenBlankAndAccessTokenMissing() {
        authService.logout(" ", null);

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(Long.class), any(LocalDateTime.class));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void logoutShouldDoNothingWhenAccessTokenResolvesNullUsername() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        when(jwtUtils.extractUsernameAllowExpired("access")).thenReturn(null);

        authService.logout("refresh", "access");

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(Long.class), any(LocalDateTime.class));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void logoutShouldDoNothingWhenUserCannotBeResolved() {
        when(jwtUtils.extractUsernameAllowExpired("access")).thenThrow(new RuntimeException("bad token"));

        authService.logout(null, "access");

        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(any(Long.class), any(LocalDateTime.class));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

}
