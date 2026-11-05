
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class PasswordUtil {

    public static boolean verifyPassword(
            String enteredPassword, String storedHash) {

        if (enteredPassword == null || storedHash == null) {
            return false;
        }

        try {
            MessageDigest md =
                MessageDigest.getInstance("SHA-256");

            byte[] hash = md.digest(
                enteredPassword.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                result.toString().getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            return false;
        }
    }
}
