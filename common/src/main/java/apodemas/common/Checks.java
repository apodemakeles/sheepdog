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
}
