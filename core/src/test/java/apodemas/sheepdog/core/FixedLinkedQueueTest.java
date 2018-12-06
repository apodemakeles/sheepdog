package apodemas.sheepdog.core;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author caozheng
 * @time 2018-12-06 15:34
 **/
public class FixedLinkedQueueTest {
    @Test
    public void testMethods(){
        FixedLinkedQueue<String> queue = new FixedLinkedQueue<>(5);
        assertNull(queue.enqueue("1"));
        assertNull(queue.enqueue("2"));
        assertNull(queue.enqueue("3"));
        assertNull(queue.enqueue("4"));
        assertNull(queue.enqueue("5"));
        assertEquals("1", queue.enqueue("6"));

        assertNotNull(queue.findFirst(s-> s.equals("6"), false));
        assertNull(queue.findFirst(s-> s.equals("7"), false));
        assertEquals("2", queue.dequeue());
        assertEquals("3", queue.dequeue());
        assertEquals("4", queue.dequeue());
        assertEquals("5", queue.dequeue());
        assertEquals("6", queue.dequeue());
        assertNull(queue.dequeue());
    }


}
