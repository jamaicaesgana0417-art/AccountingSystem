public class Session {
    private static String currentUser = null;

    public static void login(String username) { currentUser = username; }
    public static void logout() { currentUser = null; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static String getCurrentUser() { return currentUser; }
}
