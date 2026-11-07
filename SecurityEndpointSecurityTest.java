public class SecurityEndpointSecurityTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    public static void main(String[] args) {
        SecurityTokenService tokens = new SecurityTokenService();
        AuthorizationService auth = new AuthorizationService();

        LedgerlyAccounting.User alice = new LedgerlyAccounting.User(
                "Alice", "alice@example.com", "hash", "Philippines",
                new LedgerlyAccounting.CurrencyInfo("PHP", "₱", "Philippine Peso"));

        LedgerlyAccounting.User bob = new LedgerlyAccounting.User(
                "Bob", "bob@example.com", "hash", "Philippines",
                new LedgerlyAccounting.CurrencyInfo("PHP", "₱", "Philippine Peso"));

        String aliceToken = tokens.generateToken(alice.email);

        check("Valid user with their own token is accepted",
                auth.isAuthenticated(alice, aliceToken, tokens));

        check("Null user is rejected",
                !auth.isAuthenticated(null, aliceToken, tokens));

        check("Null token is rejected",
                !auth.isAuthenticated(alice, null, tokens));

        check("Blank token is rejected",
                !auth.isAuthenticated(alice, "   ", tokens));

        check("Unknown token is rejected",
                !auth.isAuthenticated(alice, "not-a-real-token", tokens));

        check("A different user's token is rejected",
                !auth.isAuthenticated(bob, aliceToken, tokens));

        check("User can access their own records",
                auth.canAccessRecords(alice, aliceToken, tokens, alice.email));

        check("User cannot access another user's records",
                !auth.canAccessRecords(alice, aliceToken, tokens, bob.email));

        check("Missing record owner is rejected",
                !auth.canAccessRecords(alice, aliceToken, tokens, null));

        tokens.revokeToken(aliceToken);

        check("Revoked token is rejected",
                !auth.isAuthenticated(alice, aliceToken, tokens));

        check("Revoked token cannot access records",
                !auth.canAccessRecords(alice, aliceToken, tokens, alice.email));

        System.out.println();
        System.out.println("Test results: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
