
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityTokenService {

    private static final long TOKEN_LIFETIME_SECONDS = 30 * 60;

    private final SecureRandom random = new SecureRandom();

    private final Map<String, Session> sessions =
            new ConcurrentHashMap<>();

    private static class Session {
        String userEmail;
        Instant expiresAt;

        Session(String email, Instant expiration) {
            this.userEmail = email;
            this.expiresAt = expiration;
        }
    }

    // Generate a secure random token
    public String generateToken(String userEmail) {

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        String tokenHash = hashToken(token);

        Instant expiration = Instant.now()
                .plusSeconds(TOKEN_LIFETIME_SECONDS);

        sessions.put(
                tokenHash,
                new Session(userEmail, expiration)
        );

        return token;
    }

    // Validate the token
    public boolean validateToken(String token) {

        if (token == null || token.isEmpty()) {
            return false;
        }

        String tokenHash = hashToken(token);

        Session session = sessions.get(tokenHash);

        if (session == null) {
            return false;
        }

        if (Instant.now().isAfter(session.expiresAt)) {
            sessions.remove(tokenHash);
            return false;
        }

        return true;
    }

    // Identify the user associated with the token
    public String getUserEmail(String token) {

        if (!validateToken(token)) {
            return null;
        }

        Session session = sessions.get(hashToken(token));

        return session.userEmail;
    }

    // Revoke token during logout
    public void revokeToken(String token) {

        if (token == null || token.isEmpty()) {
            return;
        }

        sessions.remove(hashToken(token));
    }

    // Hash token before storing it
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to hash security token.", e
            );
        }
    }
}
