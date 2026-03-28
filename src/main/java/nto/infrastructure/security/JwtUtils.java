package nto.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtils {
    @Value("${nto.app.jwtSecret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${nto.app.jwtAccessExpirationMs:900000}")
    private long jwtAccessExpirationMs;

    @Value("${nto.app.jwtIssuer:nto}")
    private String jwtIssuer;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsernameAllowExpired(String token) {
        try {
            return extractUsername(token);
        } catch (ExpiredJwtException ex) {
            return ex.getClaims().getSubject();
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(String username) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("typ", "access");

        long now = System.currentTimeMillis();

        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(username)
            .setId(UUID.randomUUID().toString())
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + jwtAccessExpirationMs))
            .setIssuer(jwtIssuer)
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        String type = extractClaim(token, claims -> claims.get("typ", String.class));
        return "access".equals(type);
    }

    public long getAccessTokenTtlSeconds() {
        return jwtAccessExpirationMs / 1000;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSignInKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
