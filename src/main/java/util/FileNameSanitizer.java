package util;

import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNameSanitizer {

    private static Pattern ILLEGAL_CHARACTERS;
    private static final Set<String> RESERVED_NAMES = new HashSet<>();

    static {
        setOsName(System.getProperty("os.name").toLowerCase());
    }

    @VisibleForTesting
    public static void setOsName(String osName) {
        RESERVED_NAMES.clear();
        if (osName.toLowerCase().contains("win")) {
            ILLEGAL_CHARACTERS = Pattern.compile("[<>:\"/\\\\|?*]");
            String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
            };
            RESERVED_NAMES.addAll(Arrays.asList(reservedNames));
        } else {
            ILLEGAL_CHARACTERS = Pattern.compile("[/]");
        }
    }

    public static String sanitizeFileName(String fileName, char replacement) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        String sanitizedFileName = ILLEGAL_CHARACTERS.matcher(fileName).replaceAll(Matcher.quoteReplacement(String.valueOf(replacement)));

        if (RESERVED_NAMES.contains(sanitizedFileName.toUpperCase())) {
            sanitizedFileName = "_" + sanitizedFileName.toUpperCase() + "_";
        }

        if (sanitizedFileName.isEmpty()) {
            throw new IllegalArgumentException("Sanitized file name cannot be empty");
        }

        return sanitizedFileName;
    }
}
