package apodemas.common;

public class Checks {
    public static <T> T notNull(T value, String name){
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        if (value == null) {
            throw new IllegalArgumentException(name + " is null");
        }

        return value;
    }

    public static String notEmpty(String value, String name){
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        if(StringUtils.empty(value)){
            throw new IllegalArgumentException(name + " is empty");
        }

        return value;
    }
}
