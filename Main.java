
public class Main {

    public static void main(String[] args) {

        AuthenticationService auth =
            new AuthenticationService();

        String email = "user@example.com";
        String password = "MyTestPassword123";

        boolean result = auth.verifyLogin(email, password);

        if (result) {
            System.out.println("Login successful!");
        } else {
            System.out.println("Invalid email or password.");
        }
    }
}
