package util;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FileNameSanitizerTest {

    @Test
    public void sanitizeFileName_returnsNullForNull_Windows() {
        FileNameSanitizer.setOsName("Windows 10");

        String originalFileName = null;
        String sanitizedFileName = FileNameSanitizer.sanitizeFileName(originalFileName, '_');
        assertThat(sanitizedFileName, equalTo(null));
    }

    @Test
    public void sanitizeFileName_shouldReplaceIllegalCharacters_Windows() {
        FileNameSanitizer.setOsName("Windows 10");

        String originalFileName = "illegal:/\\*?\"<>|.txt";
        String sanitizedFileName = FileNameSanitizer.sanitizeFileName(originalFileName, '_');
        assertThat(sanitizedFileName, equalTo("illegal_________.txt"));
    }

    @Test
    public void sanitizeFileName_shouldReplaceIllegalCharacters_Unix() {
        FileNameSanitizer.setOsName("Linux");

        String originalFileName = "illegal:/\\*?\"<>|.txt";
        String sanitizedFileName = FileNameSanitizer.sanitizeFileName(originalFileName, '_');
        assertThat(sanitizedFileName, equalTo("illegal:_\\*?\"<>|.txt"));
    }

    @Test
    public void sanitizeFileName_shouldHandleReservedNames_Windows() {
        FileNameSanitizer.setOsName("Windows 10");

        String originalFileName = "CON";
        String sanitizedFileName = FileNameSanitizer.sanitizeFileName(originalFileName, '_');
        assertThat(sanitizedFileName, equalTo("_CON_"));

        originalFileName = "prn";
        sanitizedFileName = FileNameSanitizer.sanitizeFileName(originalFileName, '_');
        assertThat(sanitizedFileName, equalTo("_PRN_"));
    }

    @Test
    public void sanitizeFileName_shouldNotChangeValidFileName() {
        String originalFileName = "valid_filename.txt";
        String sanitizedFileName = FileNameSanitizer.sanitizeFileName(originalFileName, '_');
        assertThat(sanitizedFileName, equalTo("valid_filename.txt"));
    }
}
