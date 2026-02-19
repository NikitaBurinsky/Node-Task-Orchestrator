package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthResponseDto;
import nto.application.dto.UserDto;
import nto.application.interfaces.repositories.UserRepository;
import nto.application.interfaces.services.AuthService;
import nto.core.entities.UserEntity;
import nto.infrastructure.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Override
    @Transactional
    public AuthResponseDto register(UserDto dto) {
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new RuntimeException("Username is already taken");
        }

        var user = UserEntity.builder()
            .username(dto.username())
            .password(passwordEncoder.encode(dto.password())) // Хэшируем пароль
            .build();

        userRepository.save(user);

        // Сразу генерируем токен для входа
        var userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        var token = jwtUtils.generateToken(userDetails);
        return new AuthResponseDto(token);
    }

    @Override
    public AuthResponseDto login(AuthRequestDto dto) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(dto.username(), dto.password())
        );

        var userDetails = userDetailsService.loadUserByUsername(dto.username());
        var token = jwtUtils.generateToken(userDetails);
        return new AuthResponseDto(token);
    }
}