package cz.semester.courseapp.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtTokenService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-expiration-seconds}") long expirationSeconds,
            ObjectMapper objectMapper) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
    }

    public String create(UserSession session) {
        long expiresAt = Instant.now().plusSeconds(expirationSeconds).getEpochSecond();
        Map<String, String> claims = new LinkedHashMap<>();
        claims.put("role", session.role().name());
        claims.put("displayName", session.displayName());
        claims.put("studentId", session.studentId() == null ? "" : session.studentId().toString());
        claims.put("instructorId", session.instructorId() == null ? "" : session.instructorId().toString());
        claims.put("exp", Long.toString(expiresAt));

        String header = base64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64(toJson(claims));
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public UserSession parse(String token) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw unauthorized();
        }
        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw unauthorized();
        }
        try {
            Map<String, String> claims = parseJson(new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8));
            long expiresAt = Long.parseLong(claims.getOrDefault("exp", "0"));
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Prihlaseni vyprselo nebo je neplatne.");
            }
            UserSession.Role role = UserSession.Role.valueOf(claims.get("role"));
            Long studentId = parseLongOrNull(claims.get("studentId"));
            Long instructorId = parseLongOrNull(claims.get("instructorId"));
            return new UserSession(token, role, studentId, instructorId, claims.get("displayName"));
        } catch (IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private String base64(String value) {
        return ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT podpis se nepodarilo vytvorit.", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private String toJson(Map<String, String> claims) {
        try {
            return objectMapper.writeValueAsString(claims);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JWT payload se nepodarilo vytvorit.", exception);
        }
    }

    private Map<String, String> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw unauthorized();
        }
    }

    private Long parseLongOrNull(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Prihlaseni vyprselo nebo je neplatne.");
    }
}
