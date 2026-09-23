package pe.edu.upc.legalai.securities;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtTokenUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;
    private final long expirationMs;
    private final ObjectMapper objectMapper;

    public JwtTokenUtil(@Value("${app.jwt.secret:${JWT_SECRET}}") String secret,
                        @Value("${app.jwt.expiration-ms:${JWT_EXPIRATION_MS:86400000}}") long expirationMs,
                        ObjectMapper objectMapper) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32 || expirationMs < 1000) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes y la duracion debe ser positiva");
        }
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.objectMapper = objectMapper;
    }

    public String generateToken(String email) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + (expirationMs / 1000);
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url(objectMapper.writeValueAsString(
                Map.of("sub", email, "iat", issuedAt, "exp", expiresAt)));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public String getEmailFromToken(String token) {
        try {
            if (token == null || token.length() > 8192) {
                return null;
            }
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                return null;
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expected, parts[2])) {
                return null;
            }
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!"HS256".equals(header.path("alg").asText()) || !"JWT".equals(header.path("typ").asText())
                    || !payload.path("sub").isString() || payload.path("sub").asText().isBlank()
                    || !payload.path("exp").isIntegralNumber() || !payload.path("exp").canConvertToLong()
                    || payload.path("exp").asLong() <= Instant.now().getEpochSecond()) {
                return null;
            }
            return payload.path("sub").asText();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        return getEmailFromToken(token) != null;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el token JWT", ex);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }
}
