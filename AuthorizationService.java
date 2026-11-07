
public class AuthorizationService {

    // Check if the user has a valid login session
    public boolean isAuthenticated(
            LedgerlyAccounting.User user,
            String token,
            SecurityTokenService tokenService) {

        if (user == null || token == null || token.isBlank()) {
            return false;
        }

        return tokenService.validateToken(token);
    }

    // Check if the user owns the records being accessed
    public boolean canAccessRecords(
            LedgerlyAccounting.User user,
            String token,
            SecurityTokenService tokenService,
            String recordOwnerEmail) {

        if (!isAuthenticated(user, token, tokenService)) {
            return false;
        }

        if (recordOwnerEmail == null) {
            return false;
        }

        return user.email.equalsIgnoreCase(recordOwnerEmail);
    }
}
