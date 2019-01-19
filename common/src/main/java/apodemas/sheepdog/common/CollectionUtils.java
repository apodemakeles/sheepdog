package apodemas.sheepdog.common;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author caozheng
 * @time 2019-01-15 17:35
 **/
public class CollectionUtils {

    public static <T> Optional<T> findFirst(Collection<T> collection, Predicate<T> predicate) {
        for (T item : collection) {
            if (predicate.test(item)) {
                return Optional.of(item);
            }
        }

        return Optional.empty();
    }

    public static <T> int indexOf(T[] array, T value) {
        Checks.notNull(array, "array");
        Checks.notNull(value, "value");

        for (int i = 0; i < array.length; i++) {
            if (value.equals(array[i])) {
                return i;
            }
        }

        return -1;
    }
}
