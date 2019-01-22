package apodemas.sheepdog.common.url;

import apodemas.sheepdog.common.Checks;

import java.util.Arrays;
import java.util.List;

import static apodemas.sheepdog.common.url.URLUtils.*;

/**
 * @author caozheng
 * @time 2019-01-19 09:14
 **/
public class URLParser {
    private final String input;
    private int startIdx;
    private int endIdx;
    private int idx;
    private boolean isEOF;
    private int c;
    private final ParsingState startState;

    private static final List<String> RELATIVE_SCHEMES = Arrays.asList(
            "ftp", "file", "gopher", "api", "https", "ws", "wss"
    );

    public URLParser(String input){
        this(input, ParsingState.SCHEME_START);
    }

    public URLParser(String input, ParsingState startState) {
        Checks.notNull(input, "input");
        Checks.notNull(startState, "startState");

        this.input = input;
        this.startState = startState;
    }

    public static URL parse(String input){
        Checks.notNull(input, "input");

        return new URLParser(input).parse();
    }

    public static URL parseFormPath(String input){
        Checks.notNull(input, "input");
        if(input == "") {
            throw new URLParseException("empty url");
        }

        return (input.charAt(0) == '/' ? new URLParser(input, ParsingState.RELATIVE_PATH_START):
                new URLParser(input, ParsingState.RELATIVE_PATH)).parse();
    }

    private void setIdx(final int i) {
        this.idx = i;
        this.isEOF = i >= endIdx;
        this.c = (isEOF || idx < startIdx)? 0x00 : input.codePointAt(i);
    }

    private void incIdx() {
        final int charCount = Character.charCount(this.c);
        setIdx(this.idx + charCount);
    }

    private void decrIdx() {
        if (idx <= startIdx) {
            setIdx(idx - 1);
            return;
        }
        final int charCount = Character.charCount(this.input.codePointBefore(idx));
        setIdx(this.idx - charCount);
    }

    private char at(final int i) {
        if (i >= endIdx) {
            return 0x00;
        }
        return input.charAt(i);
    }

    private void trim(){
        while (Character.isWhitespace(c)) {
            incIdx();
            startIdx++;
        }
        while (endIdx > startIdx && Character.isWhitespace(input.charAt(endIdx - 1))) {
            endIdx--;
        }
    }

    public URL parse() {
        endIdx = input.length();
        setIdx(startIdx);
        trim();
        ParsingState state = startState == null ? ParsingState.SCHEME_START : startState;
        String scheme = null;
        final StringBuilder buffer = new StringBuilder(input.length()*2);
        boolean terminate = false;
        boolean atFlag = false;
        String username = null;
        String password = null;
        String host = null;
        int port = -1;
        String path = null;
        final StringBuilder usernameBuffer = new StringBuilder(buffer.length());
        StringBuilder passwordBuffer = null;
        StringBuilder query = null;
        StringBuilder fragment = null;

        while (!terminate) {

            if (idx > endIdx) {
                break;
            }

            switch (state) {
                case SCHEME_START: {
                    if (isASCIIAlpha(c)) {
                        buffer.appendCodePoint(Character.toLowerCase(c));
                        state = ParsingState.SCHEME;
                    } else {
                        throw new URLParseException("Scheme must start with alpha character");
                    }

                    break;
                }
                case SCHEME: {
                    if (isASCIIAlphanumeric(c) || c == '+' || c == '-' || c == '.') {
                        buffer.appendCodePoint(Character.toLowerCase(c));
                    } else if (c == ':') {
                        incIdx();
                        if(c != '/'){
                            throw new URLParseException("Scheme must end with slash");
                        }
                        incIdx();
                        if(c != '/'){
                            throw new URLParseException("Scheme must end with slash");
                        }
                        scheme = buffer.toString();
                        buffer.setLength(0);

                        state = ParsingState.AUTHORITY;

                    } else if (isEOF) {
                        terminate = true;
                    } else {
                        throw new URLParseException("Illegal character in scheme", idx);
                    }

                    break;
                }

                case AUTHORITY: {
                    if (c == '@') {
                        if (atFlag) {
                            throw new URLParseException("User or password contains an at symbol (\"@\") not percent-encoded", idx);
                        }
                        atFlag = true;

                        for (int i = 0; i < buffer.codePointCount(0, buffer.length()); i++) {
                            final int otherChar = buffer.codePointAt(i);
                            if (otherChar == 0x0009 || otherChar == 0x000A || otherChar == 0x000D) {
                                throwIllegalWhitespaceError();
                            }
                            if (!isURLCodePoint(otherChar) && otherChar != '%') {
                                throw new URLParseException("Illegal character in user or password: not a URL code point", idx);
                            }
                            if (otherChar == '%') {
                                if (i + 2 >= buffer.length() || !isASCIIHexDigit(buffer.charAt(i + 1)) || !isASCIIHexDigit(buffer.charAt(i + 2))) {
                                    throw new URLParseException("Percentage (\"%\") is not followed by two hexadecimal digits", idx);
                                } else if (isASCIIHexDigit(buffer.charAt(i + 1)) && isASCIIHexDigit(buffer.charAt(i + 2))) {
                                    buffer.setCharAt(i + 1, Character.toUpperCase(buffer.charAt(i + 1)));
                                    buffer.setCharAt(i + 2, Character.toUpperCase(buffer.charAt(i + 2)));
                                }
                            }
                            if (otherChar == ':' && passwordBuffer == null) {
                                passwordBuffer = new StringBuilder(buffer.length() - i);
                                continue;
                            }
                            if (passwordBuffer != null) {
                                utf8PercentEncode(otherChar, EncodeSet.DEFAULT, passwordBuffer);
                            } else {
                                utf8PercentEncode(otherChar, EncodeSet.DEFAULT, usernameBuffer);
                            }

                        }

                        buffer.setLength(0);
                    } else if (isEOF || c == '/' || c == '\\' || c == '?' || c == '#') {
                        setIdx(idx - buffer.length() - 1);
                        if (atFlag) {
                            username = usernameBuffer.toString();
                            if (passwordBuffer != null) {
                                password = passwordBuffer.toString();
                            }
                        }
                        buffer.setLength(0);
                        state = ParsingState.HOST;
                    } else {
                        buffer.appendCodePoint(c);
                    }

                    break;
                }
                case HOST: {
                    if (c == ':'){
                        host = buffer.toString();
                        buffer.setLength(0);
                        state = ParsingState.PORT;
                    }else if(isEOF || c == '/' || c == '\\' || c == '?' || c == '#'){
                        decrIdx();
                        host = buffer.toString();
                        buffer.setLength(0);
                        state = ParsingState.RELATIVE_PATH_START;
                    }else if (c == 0x0009 || c == 0x000A || c == 0x000D) {
                        throwIllegalWhitespaceError();
                    } else {
                        buffer.appendCodePoint(c);
                    }

                    break;
                }
                case PORT:{
                    if (isASCIIDigit(c)) {
                        buffer.appendCodePoint(c);
                    } else if (isEOF || c == '/' || c == '\\' || c == '?' || c == '#') {
                        // Remove leading zeroes
                        while (buffer.length() > 0 && buffer.charAt(0) == 0x0030 && buffer.length() > 1) {
                            buffer.deleteCharAt(0);
                        }
                        //XXX: This is redundant with URL constructor
                        if (buffer.toString().equals(getDefaultPortForScheme(scheme))) {
                            buffer.setLength(0);
                        }
                        if (buffer.length() == 0) {
                            port = -1;
                        } else {
                            port = Integer.parseInt(buffer.toString());
                        }
                        buffer.setLength(0);
                        state = ParsingState.RELATIVE_PATH_START;
                        idx--;
                    } else if (c == 0x0009 || c == 0x000A || c == 0x000D) {
                        throwIllegalWhitespaceError();
                    } else {
                        throw new URLParseException("Illegal character in port", idx);
                    }

                    break;
                }
                case RELATIVE_PATH_START: {
                    if (c == '\\') {
                        throwBackslashAsDelimiterError();
                    }
                    buffer.append('/');
                    state = ParsingState.RELATIVE_PATH;
                    if (c != '/' && c != '\\') {
                        decrIdx();
                    }

                    break;
                }
                case RELATIVE_PATH: {
                    if (isEOF || c == '\\' ||  c == '?' || c == '#') {
                        if (c == '\\') {
                            throwBackslashAsDelimiterError();
                        }
                        path = buffer.toString();
                        buffer.setLength(0);
                        if (c == '?') {
                            query = new StringBuilder();
                            state = ParsingState.QUERY;
                        } else if (c == '#') {
                            fragment = new StringBuilder();
                            state = ParsingState.FRAGMENT;
                        }

                    } else if (c == 0x0009 || c == 0x000A || c == 0x000D) {
                        throwIllegalWhitespaceError();
                    } else {
                        if (!isURLCodePoint(c) && c != '%') {
                            throw new URLParseException("Illegal character in path segment: not a URL code point");
                        }

                        if (c == '%') {
                            if (!isASCIIHexDigit(at(idx+1)) || !isASCIIHexDigit(at(idx+2))) {
                                throwInvalidPercentEncodingError();
                            } else {
                                buffer.append((char) c)
                                        .append(Character.toUpperCase(input.charAt(idx + 1)))
                                        .append(Character.toUpperCase(input.charAt(idx + 2)));
                                setIdx(idx + 2);
                                break;
                            }
                        }

                        utf8PercentEncode(c, EncodeSet.DEFAULT, buffer);
                    }
                    break;
                }
                case QUERY: {

                    //XXX: When we come from stateOverride, query buffer is null
                    if (query == null) {
                        query = new StringBuilder();
                    }

                    if (isEOF || c == '#') {
                        final byte[] bytes = buffer.toString().getBytes(UTF_8);
                        for (int i = 0; i < bytes.length; i++) {
                            final byte b = bytes[i];
                            if (b < 0x21 || b > 0x7E || b == 0x22 || b == 0x23 || b == 0x3C || b == 0x3E || b == 0x60) {
                                percentEncode(b, query);
                            } else {
                                query.append((char) b);
                            }
                        }
                        buffer.setLength(0);
                        if (c == '#') {
                            fragment = new StringBuilder();
                            state = ParsingState.FRAGMENT;
                        }
                    } else if (c == 0x0009 || c == 0x000A || c == 0x000D) {
                        throwIllegalWhitespaceError();
                    } else {
                        if (!isURLCodePoint(c) && c != '%') {
                            throw new URLParseException("Illegal character in query: not a URL code point");
                        }
                        if (c == '%') {
                            if (!isASCIIHexDigit(at(idx + 1)) || !isASCIIHexDigit(at(idx + 2))) {
                                throwInvalidPercentEncodingError();
                            } else {
                                buffer.append((char) c)
                                        .append(Character.toUpperCase(input.charAt(idx + 1)))
                                        .append(Character.toUpperCase(input.charAt(idx + 2)));
                                setIdx(idx + 2);
                                break;
                            }
                        }
                        buffer.appendCodePoint(c);
                    }

                    break;
                }
                case FRAGMENT: {

                    //XXX: When we come from stateOverride, fragment buffer is null
                    if (fragment == null) {
                        fragment = new StringBuilder();
                    }

                    if (isEOF) {
                        // Do nothing
                    } else if (c == 0x0009 || c == 0x000A || c == 0x000D) {
                        throwIllegalWhitespaceError();
                    } else {
                        if (!isURLCodePoint(c) && c != '%') {
                            throw new URLParseException("Illegal character in path segment: not a URL code point", idx);
                        }
                        if (c == '%') {
                            if (!isASCIIHexDigit(at(idx+1)) || !isASCIIHexDigit(at(idx+2))) {
                                throwInvalidPercentEncodingError();
                            } else {
                                fragment.append((char)c)
                                        .append(Character.toUpperCase(input.charAt(idx+1)))
                                        .append(Character.toUpperCase(input.charAt(idx+2)));
                                setIdx(idx+2);
                                break;
                            }
                        }

                        utf8PercentEncode(c, EncodeSet.SIMPLE, fragment);

                    }
                    break;
                }
            }
            if (idx == -1) {
                setIdx(startIdx);
            } else {
                incIdx();
            }
        }

        return new URL(scheme,  username, password, host, port, path, (query == null)? null : query.toString(), (fragment == null)? null : fragment.toString());

    }


    private void throwIllegalWhitespaceError(){
        throw new URLParseException("Tab, new line or carriage return found", idx);
    }

    private void throwBackslashAsDelimiterError(){
        throw new URLParseException("Backslash (\"\\\") used as path segment delimiter", idx);
    }

    private void throwInvalidPercentEncodingError(){
        throw new URLParseException("Percentage (\"%\") is not followed by two hexadecimal digits", idx);
    }


    private  enum EncodeSet {
        SIMPLE,
        DEFAULT,
        PASSWORD,
        USERNAME
    }

    private static void utf8PercentEncode(final int c, final EncodeSet encodeSet, final StringBuilder buffer) {
        if (encodeSet != null) {
            switch (encodeSet) {
                case SIMPLE:
                    if (!isInSimpleEncodeSet(c)) {
                        buffer.appendCodePoint(c);
                        return;
                    }
                    break;
                case DEFAULT:
                    if (!isInDefaultEncodeSet(c)) {
                        buffer.appendCodePoint(c);
                        return;
                    }
                    break;
                case PASSWORD:
                    if (!isInPasswordEncodeSet(c)) {
                        buffer.appendCodePoint(c);
                        return;
                    }
                    break;
                case USERNAME:
                    if (!isInUsernameEncodeSet(c)) {
                        buffer.appendCodePoint(c);
                        return;
                    }
                    break;
            }
        }
        final byte[] bytes = new String(Character.toChars(c)).getBytes(UTF_8);
        for (final byte b : bytes) {
            percentEncode(b, buffer);
        }
    }

    private static boolean isInSimpleEncodeSet(final int c) {
        return c < 0x0020 || c > 0x007E;
    }

    private static boolean isInDefaultEncodeSet(final int c) {
        return isInSimpleEncodeSet(c) || c == ' ' || c == '"' || c == '#' || c == '<' || c == '>' || c == '?' || c == '`';
    }

    private static boolean isInPasswordEncodeSet(final int c) {
        return isInDefaultEncodeSet(c) || c == '/' || c == '@' || c == '\\';
    }

    private static boolean isInUsernameEncodeSet(final int c) {
        return isInPasswordEncodeSet(c) || c == ':';
    }
}
