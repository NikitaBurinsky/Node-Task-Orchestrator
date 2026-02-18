package nto.web.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthResponseDto;
import nto.application.dto.UserDto;
import nto.application.interfaces.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Авторизация и регистрация")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя")
    public ResponseEntity<AuthResponseDto> register(@RequestBody UserDto dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Возвращает JWT токен")
    public ResponseEntity<AuthResponseDto> login(@RequestBody AuthRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

}