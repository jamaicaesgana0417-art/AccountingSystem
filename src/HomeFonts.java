import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HomeFonts {

    private static Font poppinsSemiBold;
    private static Font nunitoSansRegular;

    private HomeFonts() {}

    public static synchronized void initialize() {
        if (poppinsSemiBold != null && nunitoSansRegular != null) {
            return;
        }

        poppinsSemiBold = load(
            "/resources/fonts/Poppins-SemiBold.ttf",
            "src\\resources\\fonts\\Poppins-SemiBold.ttf"
        );
        nunitoSansRegular = load(
            "/resources/fonts/NunitoSans-Regular.ttf",
            "src\\resources\\fonts\\NunitoSans-Regular.ttf"
        );

        GraphicsEnvironment environment =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        environment.registerFont(poppinsSemiBold);
        environment.registerFont(nunitoSansRegular);
    }

    public static Font poppinsSemiBold(float size) {
        ensureInitialized();
        return poppinsSemiBold.deriveFont(Font.PLAIN, size);
    }

    public static Font nunitoSansRegular(float size) {
        ensureInitialized();
        return nunitoSansRegular.deriveFont(Font.PLAIN, size);
    }

    private static void ensureInitialized() {
        if (poppinsSemiBold == null || nunitoSansRegular == null) {
            initialize();
        }
    }

    private static Font load(String resource, String sourcePath) {
        try (InputStream input = HomeFonts.class.getResourceAsStream(resource)) {
            if (input != null) {
                return Font.createFont(Font.TRUETYPE_FONT, input);
            }
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException(
                "Could not load font resource " + resource,
                exception
            );
        }

        Path path = Path.of(sourcePath);
        try (InputStream input = Files.newInputStream(path)) {
            return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (FontFormatException | IOException exception) {
            throw new IllegalStateException(
                "Could not load font file " + path,
                exception
            );
        }
    }
}
