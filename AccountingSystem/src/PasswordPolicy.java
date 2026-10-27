import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PasswordPolicy {

    public static final String FAMILIAR_WARNING =
        "Can’t use this password. It’s one of the most commonly used. "
        + "Hackers will get you ahh.";

    private static final Set<String> COMMON = new HashSet<>();

    static {
        String words = """
            123456 password 12345678 qwerty 123456789 12345 1234 111111
            1234567 dragon 123123 baseball abc123 abc football monkey letmein
            696969 shadow master 666666 qwertyuiop 123321 mustang 1234567890
            michael 654321 superman 1qaz2wsx 7777777 121212 000000 qazwsx
            123qwe killer trustno1 jordan jennifer zxcvbnm asdfgh hunter
            buster soccer harley batman andrew tigger sunshine iloveyou
            charlie robert thomas hockey ranger daniel starwars 112233
            george computer michelle jessica pepper zxcvbn 555555 11111111
            131313 freedom 777777 pass maggie 159753 aaaaaa ginger princess
            joshua cheese amanda summer love ashley nicole chelsea biteme
            matthew access yankees 987654321 dallas austin thunder taylor
            matrix admin welcome password1 password12 password123 passw0rd
            p@ssw0rd qwerty123 qwerty12 1q2w3e4r 1q2w3e admin123 letmein1
            monkey123 dragon123 welcome1 welcome123 football1 superman1
            princess1 sunshine1 iloveyou1 abc12345 asdfghjkl zxcvbnm123
            accounting accounting1 accounting123 account login user
            username root test test123 guest default changeme secret
            qwertyui qwerty1 1qazxsw2 zaq12wsx 123abc 12341234 11223344
            987654321 0987654321 123454321 1234qwer qwer1234 asdf1234
            iloveu iloveu2 ilovegod godisgood jesus jesus1 blessed angel
            angel1 mahal mahalkita mahal123 pilipinas philippines manila
            cebu davao maria maria123 kuya ate pogi pogiako cutie
            barcelona liverpool arsenal chelsea1 ronaldo messi lakers
            naruto pokemon minecraft roblox fortnite gaming gamer
            cookie cookie1 hello hello123 hellohello lovely lovelove
            school student student1 teacher flower flower1 butterfly
            bubbles cutie123 sweetie nothing whatever apple banana
            orange purple yellow rainbow master123 admin1 abcd1234
            """;
        COMMON.addAll(Arrays.asList(words.trim().split("\\s+")));

        // OPTIONAL: drop a file named common-passwords.txt next to the
        // project (one password per line) to make the list much bigger.
        try {
            Path file = Path.of("common-passwords.txt");
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file)) {
                    String w = line.trim().toLowerCase();
                    if (!w.isEmpty()) COMMON.add(w);
                }
            }
        } catch (Exception ignored) {
            // the built-in list is still used
        }
    }

    private static final String[] KEYBOARD = { "qwer", "asdf", "zxcv" };

    // Returns a warning message, or null if the password is OK.
    public static String validate(char[] password, String username) {
        String raw = new String(password);
        String lower = raw.toLowerCase();

        // 1. Familiar passwords are checked FIRST, so the warning shows
        //    while the person is still typing.
        if (isFamiliar(lower)) {
            return FAMILIAR_WARNING;
        }

        if (password.length < 8) {
            return "Use at least 8 characters.";
        }

        if (classes(raw) < 3) {
            return "Mix at least 3 of: lowercase, UPPERCASE, numbers, symbols.";
        }

        String user = username == null ? "" : username.trim().toLowerCase();
        if (user.length() >= 3 && lower.contains(user)) {
            return "Your password can't contain your username.";
        }

        if (hasPattern(lower)) {
            return "Avoid patterns like 1234, abcd, qwer or aaaa.";
        }

        return null;
    }

    // 0 = empty, 1 = weak, 2 = fair, 3 = good, 4 = strong
    public static int score(char[] password, String username) {
        if (password.length == 0) return 0;
        if (validate(password, username) != null) return 1;

        String raw = new String(password);
        int score = 2;
        if (password.length >= 12) score++;
        if (password.length >= 16 || (password.length >= 12 && classes(raw) == 4)) {
            score++;
        }
        return Math.min(score, 4);
    }

    private static int classes(String s) {
        boolean lower = false, upper = false, digit = false, other = false;
        for (char c : s.toCharArray()) {
            if (Character.isLowerCase(c)) lower = true;
            else if (Character.isUpperCase(c)) upper = true;
            else if (Character.isDigit(c)) digit = true;
            else other = true;
        }
        int count = 0;
        if (lower) count++;
        if (upper) count++;
        if (digit) count++;
        if (other) count++;
        return count;
    }

    // Catches the password itself and the usual tricks:
    // P@ssw0rd, Password123!, Iloveyou2026 ...
    private static boolean isFamiliar(String lower) {
        String deleet = deleet(lower);
        String[] candidates = {
            lower,
            deleet,
            stripTrailing(lower),
            stripTrailing(deleet)
        };
        for (String c : candidates) {
            if (!c.isEmpty() && COMMON.contains(c)) return true;
        }
        return false;
    }

    private static String deleet(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '@': case '4': c = 'a'; break;
                case '0': c = 'o'; break;
                case '3': c = 'e'; break;
                case '$': case '5': c = 's'; break;
                case '7': c = 't'; break;
                default: break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // "password123!" -> "password"
    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && !Character.isLetter(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }

    private static boolean hasPattern(String lower) {
        for (String row : KEYBOARD) {
            if (lower.contains(row)) return true;
        }

        for (int i = 0; i + 3 < lower.length(); i++) {
            boolean up = true, down = true, same = true;
            for (int k = 0; k < 3; k++) {
                char a = lower.charAt(i + k);
                char b = lower.charAt(i + k + 1);
                if (!Character.isLetterOrDigit(a) || !Character.isLetterOrDigit(b)) {
                    up = down = same = false;
                    break;
                }
                if (b - a != 1) up = false;
                if (a - b != 1) down = false;
                if (a != b) same = false;
            }
            if (up || down || same) return true;
        }
        return false;
    }
}