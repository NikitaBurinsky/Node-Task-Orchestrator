package nto.application.dto;

public record AuthTokensDto(
    String accessToken,
    String refreshToken,
    Long expiresIn
) {
}
