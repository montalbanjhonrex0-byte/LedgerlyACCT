
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthenticationService {

    public boolean verifyLogin(String email, String password) {

        if (email == null || password == null
                || email.isBlank() || password.isEmpty()) {
            return false;
        }

        String sql =
            "SELECT password_hash FROM accounts WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    String storedHash =
                        rs.getString("password_hash");

                    return PasswordUtil.verifyPassword(
                        password, storedHash);
                }

                return false;
            }

        } catch (SQLException e) {
            System.err.println(
                "Database error during login verification.");
            return false;
        }
    }
}
