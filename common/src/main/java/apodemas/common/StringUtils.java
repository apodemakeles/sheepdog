package apodemas.common;

public class StringUtils {
    public static boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean notEmpty(String s) {
        return s != null && s.length() > 0;
    }
}
