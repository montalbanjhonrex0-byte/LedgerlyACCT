# Ledgerly Endpoint Security Testing & Documentation

## 1. Scope

The supplied project is a Java Swing desktop application. The reviewed code does not define HTTP/REST routes or a separate web API server. For this task, "security routes" means the backend methods and screen actions that control login, account access, and financial-record access.

The project currently uses in-memory user and entry maps. The login screen, main workspace, and add-entry action are the relevant access paths.

## 2. Security checks reviewed

- Login creates a session token after the email and password match.
- The token service generates a random token and stores only its SHA-256 hash.
- Tokens have a 30-minute lifetime.
- Logout revokes the current token.
- The main workspace checks whether the current user has a valid session.
- The add-entry action checks that the signed-in user owns the records being changed.

## 3. Issue found and correction

The original `AuthorizationService.isAuthenticated` checked whether a token was valid, but did not check whether that token belonged to the `User` object passed into the method. A valid token issued to Alice could therefore be paired with Bob's user object and pass the authentication check.

The corrected service calls `tokenService.getUserEmail(token)` and compares the token owner's email with the supplied user's email. Record access is then allowed only when the requested record owner's email also matches the signed-in user.

## 4. Test results

Run `SecurityEndpointSecurityTest.java` to test these cases:

| Test | Expected result |
|---|---|
| Valid user with own token | Allow |
| Null user | Deny |
| Null or blank token | Deny |
| Unknown token | Deny |
| Token belonging to another user | Deny |
| User accessing own records | Allow |
| User accessing another user's records | Deny |
| Missing record owner | Deny |
| Revoked token | Deny |
| Revoked token accessing records | Deny |

The test program prints PASS/FAIL for each case and exits with an error code if any test fails.

## 5. Client screen connection notes

This project currently uses Swing screens, not HTTP requests. The following is the intended connection between screens and backend methods:

| Client screen/action | Backend method | Expected behavior |
|---|---|---|
| Log in button | `tokenService.generateToken(user.email)` after password verification | Create a session for the signed-in user |
| Open main workspace | `authorizationService.isAuthenticated(current, currentToken, tokenService)` | Show the workspace only for the matching active session |
| Add financial entry | `authorizationService.canAccessRecords(current, currentToken, tokenService, recordOwnerEmail)` | Allow only the owner to add records |
| Log out | `tokenService.revokeToken(currentToken)` | Invalidate the session |
| Open a user's records | `authorizationService.canAccessRecords(...)` | Check the session and record owner before displaying records |

For any future web/mobile frontend, the client should send its login/session credential to a backend endpoint over HTTPS. The backend must identify the user from the token itself, check record ownership on the server, and return only the data that user is allowed to see. The client must not be trusted to choose another user's email or role as proof of permission.

## 6. Limitations and further recommendations

- No HTTP endpoints are present in the supplied source, so this review does not claim that REST routes were tested.
- The `User` class has no role field and the project has no admin-only route. Admin-versus-user authorization could not be tested from these files.
- The app's user and financial-entry data are stored in memory and reset when it closes.
- The current token store is also in memory. Sessions will not survive an application restart.
- Keep authorization checks in backend methods for every sensitive operation, not only in screen navigation.
- For a production web service, use HTTPS, secure session handling, server-side ownership checks, password hashing designed for passwords (such as bcrypt, scrypt, or Argon2), and audit logging.

## 7. How to run the test

Place these files in the same project folder:
- `LedgerlyAccounting.java`
- `AuthorizationService.java` (use the corrected version supplied)
- `SecurityTokenService.java`
- `SecurityEndpointSecurityTest.java`

Open PowerShell in that folder and run:

```powershell
javac LedgerlyAccounting.java AuthorizationService.java SecurityTokenService.java SecurityEndpointSecurityTest.java
java SecurityEndpointSecurityTest
```

A successful run should end with `11 passed, 0 failed`.
