package apodemas.sheepdog.common;

public class StringUtils {
    public static boolean empty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean notEmpty(String s) {
        return s != null && s.length() > 0;
    }

    public static boolean containsIgnoreCase(String s, String[] ss) {
        for (int i = 0; i < ss.length; i++) {
            if (ss[i].equalsIgnoreCase(s)) {
                return true;
            }
        }

        return false;
    }
}
