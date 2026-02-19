package nto.application.interfaces.services;

import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthResponseDto;
import nto.application.dto.UserDto;

public interface AuthService {
    AuthResponseDto register(UserDto dto);

    AuthResponseDto login(AuthRequestDto dto);
}