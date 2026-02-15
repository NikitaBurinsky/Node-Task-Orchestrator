package nto.application.dto;

public record ServerDto(Long id, String hostname, String ipAddress, Integer port) {
}