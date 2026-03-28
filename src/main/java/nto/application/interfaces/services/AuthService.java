package nto.application.interfaces.services;

import nto.application.dto.AuthRequestDto;
import nto.application.dto.AuthTokensDto;
import nto.application.dto.UserDto;

public interface AuthService {
    AuthTokensDto register(UserDto dto);

    AuthTokensDto login(AuthRequestDto dto);

    AuthTokensDto refresh(String refreshToken);

    void logout(String refreshToken, String accessToken);
}
