package Security;

import java.util.regex.Pattern;

public class EnvUtil {
    public static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }

    public static String requireEnvWithPattern(String name, Pattern pattern) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        if (!pattern.matcher(value).matches()) {
            throw new IllegalStateException("env var: " + name + " must be of pattern: " + pattern);
        }
        return value;
    }
}
