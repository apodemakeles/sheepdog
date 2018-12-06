package apodemas.sheepdog.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author caozheng
 * @time 2018-12-06 14:53
 **/
public class FixedLinkedQueue<T> implements Iterable<T> {
    private final int capacity;
    private List<T> queue = new LinkedList<>();

    public FixedLinkedQueue(int capacity){
        this.capacity = capacity;
    }

    public T findFirst(Predicate<T> predicate, boolean remove) {
        T value = null;
        final Iterator<T> each = queue.iterator();
        while (each.hasNext()) {
            T v = each.next();
            if (predicate.test(v)) {
                value = v;
                if(remove) {
                    each.remove();
                }
            }
        }

        return value;
    }

    public T enqueue(T value){
        T oldValue = ensureCapacity();
        queue.add(value);

        return oldValue;
    }

    public T ensureCapacity(){
        if(queue.size() >= capacity) {
            return dequeue();
        }

        return null;
    }

    public T dequeue(){
        if(queue.size() == 0){
            return null;
        }

        return queue.remove(0);
    }

    public int size(){
        return queue.size();
    }

    public Iterator<T> iterator(){
        return queue.iterator();
    }

}
