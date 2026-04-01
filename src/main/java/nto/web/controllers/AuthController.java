package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthResponseDto;
import nto.application.dto.AuthTokensDto;
import nto.application.dto.UserDto;
import nto.application.interfaces.services.AuthService;
import nto.core.utils.exceptions.InvalidRefreshTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Auth")
public class AuthController {

    private final AuthService authService;

    @Value("${nto.app.jwtRefreshCookieName:nto_refresh}")
    private String refreshCookieName;

    @Value("${nto.app.jwtRefreshExpirationMs:2592000000}")
    private long refreshExpirationMs;

    @Value("${nto.app.jwtCookieSameSite:Lax}")
    private String cookieSameSite;

    @Value("${nto.app.jwtCookieSecure:true}")
    private boolean cookieSecure;

    @Value("${nto.app.jwtCookieDomain:}")
    private String cookieDomain;

    @Value("${nto.app.jwtCookiePath:/}")
    private String cookiePath;

    @PostMapping("/register")
    @Operation(
        summary = "Регистрация",
        description = "Создание нового пользователя"
    )
    public ResponseEntity<AuthResponseDto> register(@RequestBody @Valid UserDto dto) {
        AuthTokensDto tokens = authService.register(dto);
        return buildAuthResponse(tokens);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Логин",
        description = "Вход в систему от имени существующего пользователя"
    )
    public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid AuthRequestDto dto) {
        AuthTokensDto tokens = authService.login(dto);
        return buildAuthResponse(tokens);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Обновление access token",
        description = "Выдает новый access token по refresh cookie. Если refresh отсутствует или пустой - 401."
    )
    public ResponseEntity<AuthResponseDto> refresh(
        @CookieValue(name = "${nto.app.jwtRefreshCookieName:nto_refresh}", required = false)
        String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        AuthTokensDto tokens = authService.refresh(refreshToken);
        return buildAuthResponse(tokens);
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Выход из системы",
        description = "Инвалидирует refresh и access токены и очищает refresh cookie."
    )
    public ResponseEntity<Void> logout(
        @CookieValue(name = "${nto.app.jwtRefreshCookieName:nto_refresh}", required = false)
        String refreshToken,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
        String authHeader
    ) {
        String accessToken = extractBearerToken(authHeader);
        authService.logout(refreshToken, accessToken);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(null).toString())
            .build();
    }

    private ResponseEntity<AuthResponseDto> buildAuthResponse(AuthTokensDto tokens) {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken()).toString())
            .body(new AuthResponseDto(tokens.accessToken(), tokens.expiresIn()));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
                refreshCookieName,
                refreshToken == null ? "" : refreshToken
            )
            .httpOnly(true)
            .secure(cookieSecure)
            .path(cookiePath);

        if (cookieSameSite != null && !cookieSameSite.isBlank()) {
            builder.sameSite(cookieSameSite);
        }

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        if (refreshToken == null) {
            builder.maxAge(0);
        } else {
            builder.maxAge(Duration.ofMillis(refreshExpirationMs));
        }

        return builder.build();
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}

