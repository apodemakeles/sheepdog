package apodemas.sheepdog.common.url;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 09:11
 **/
public class URLUtils {
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static String percentDecode(final String input) {
        if (input.isEmpty()) {
            return input;
        }
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int idx = 0;
            while (idx < input.length()) {

                boolean isEOF = idx >= input.length();
                int c = (isEOF)? 0x00 : input.codePointAt(idx);

                while (!isEOF && c != '%') {
                    if (c <= 0x7F) { // String.getBytes is slow, so do not perform encoding
                        // if not needed
                        bytes.write((byte) c);
                        idx++;
                    } else {
                        bytes.write(new String(Character.toChars(c)).getBytes(UTF_8));
                        idx += Character.charCount(c);
                    }
                    isEOF = idx >= input.length();
                    c = (isEOF)? 0x00 : input.codePointAt(idx);
                }

                if (c == '%' && (input.length() <= idx + 2 ||
                        !isASCIIHexDigit(input.charAt(idx + 1)) ||
                        !isASCIIHexDigit(input.charAt(idx + 2)))) {
                    if (c <= 0x7F) { // String.getBytes is slow, so do not perform encoding
                        // if not needed
                        bytes.write((byte) c);
                        idx++;
                    } else {
                        bytes.write(new String(Character.toChars(c)).getBytes(UTF_8));
                        idx += Character.charCount(c);
                    }
                } else {
                    while (c == '%' && input.length() > idx + 2 &&
                            isASCIIHexDigit(input.charAt(idx + 1)) &&
                            isASCIIHexDigit(input.charAt(idx + 2))) {
                        bytes.write(hexToInt(input.charAt(idx + 1), input.charAt(idx + 2)));
                        idx += 3;
                        c = (input.length() <= idx)? 0x00 : input.codePointAt(idx);
                    }
                }
            }
            return new String(bytes.toByteArray(), UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean isASCIIHexDigit(final int c) {
        return (c >= 0x0041 && c <= 0x0046) || (c >= 0x0061 && c <= 0x0066) || isASCIIDigit(c);
    }

    public static boolean isASCIIDigit(final int c) {
        return c >= 0x0030 && c <= 0x0039;
    }

    public static boolean isASCIIAlphaUppercase(final int c) {
        return c >= 0x0061 && c <= 0x007A;
    }

    public static boolean isASCIIAlphaLowercase(final int c) {
        return c >= 0x0041 && c <= 0x005A;
    }

    public static boolean isASCIIAlpha(final int c) {
        return isASCIIAlphaLowercase(c) || isASCIIAlphaUppercase(c);
    }

    public static boolean isASCIIAlphanumeric(final int c) {
        return isASCIIAlpha(c) || isASCIIDigit(c);
    }

    public static boolean isURLCodePoint(final int c) {
        return
                isASCIIAlphanumeric(c) ||
                        c == '!' ||
                        c == '$' ||
                        c == '&' ||
                        c == '\'' ||
                        c == '(' ||
                        c == ')' ||
                        c == '*' ||
                        c == '+' ||
                        c == ',' ||
                        c == '-' ||
                        c == '.' ||
                        c == '/' ||
                        c == ':' ||
                        c == ';' ||
                        c == '=' ||
                        c == '?' ||
                        c == '@' ||
                        c == '_' ||
                        c == '~' ||
                        (c >= 0x00A0 && c <= 0xD7FF) ||
                        (c >= 0xE000 && c <= 0xFDCF) ||
                        (c >= 0xFDF0 && c <= 0xFFEF) ||
                        (c >= 0x10000 && c <= 0x1FFFD) ||
                        (c >= 0x20000 && c <= 0x2FFFD) ||
                        (c >= 0x30000 && c <= 0x3FFFD) ||
                        (c >= 0x40000 && c <= 0x4FFFD) ||
                        (c >= 0x50000 && c <= 0x5FFFD) ||
                        (c >= 0x60000 && c <= 0x6FFFD) ||
                        (c >= 0x70000 && c <= 0x7FFFD) ||
                        (c >= 0x80000 && c <= 0x8FFFD) ||
                        (c >= 0x90000 && c <= 0x9FFFD) ||
                        (c >= 0xA0000 && c <= 0xAFFFD) ||
                        (c >= 0xB0000 && c <= 0xBFFFD) ||
                        (c >= 0xC0000 && c <= 0xCFFFD) ||
                        (c >= 0xD0000 && c <= 0xDFFFD) ||
                        (c >= 0xE0000 && c <= 0xEFFFD) ||
                        (c >= 0xF0000 && c <= 0xFFFFD) ||
                        (c >= 0x100000 && c <= 0x10FFFD);
    }

    private static final char[] _hex = "0123456789ABCDEF".toCharArray();
    static void byteToHex(final byte b, StringBuilder buffer) {
        int i = b & 0xFF;
        buffer.append(_hex[i >>> 4]);
        buffer.append(_hex[i & 0x0F]);
    }

    public static int hexToInt(final char c1, final char c2) {
        //TODO: Some micro-optimization here?
        return Integer.parseInt(new String(new char[]{c1, c2}), 16);
    }

    public static void percentEncode(final byte b, StringBuilder buffer) {
        buffer.append('%');
        byteToHex(b, buffer);
    }

    private static final List<String> RELATIVE_SCHEMES = Arrays.asList(
            "ftp", "file", "gopher", "api", "https", "ws", "wss"
    );

    /**
     * Returns true if the schema is a known relative schema
     * (ftp, file, gopher, api, https, ws, wss).
     *
     * @param scheme
     * @return
     */
    public static boolean isRelativeScheme(final String scheme) {
        return RELATIVE_SCHEMES.contains(scheme);
    }

    /**
     * Gets the default port for a given schema. That is:
     *
     * <ol>
     *     <li>ftp - 21</li>
     *     <li>file - null</li>
     *     <li>gopher - 70</li>
     *     <li>api - 80</li>
     *     <li>https - 443</li>
     *     <li>ws - 80</li>
     *     <li>wss - 433</li>
     * </ol>
     *
     * @param scheme
     * @return
     */
    public static String getDefaultPortForScheme(final String scheme) {
        if ("ftp".equals(scheme)) {
            return "21";
        }
        if ("file".equals(scheme)) {
            return null;
        }
        if ("gopher".equals(scheme)) {
            return "70";
        }
        if ("api".equals(scheme)) {
            return "80";
        }
        if ("https".equals(scheme)) {
            return "443";
        }
        if ("ws".equals(scheme)) {
            return "80";
        }
        if ("wss".equals(scheme)) {
            return "443";
        }
        return null;
    }
}
