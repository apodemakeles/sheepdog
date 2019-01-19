package apodemas.sheepdog.http.server;

import apodemas.sheepdog.common.Checks;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author caozheng
 * @time 2019-01-18 10:23
 **/
public class PathParams implements Iterable<PathParams.Entry>{

    private volatile Entry[] entries = new Entry[4];
    private int used = 0;

    public void add(String key, String value){
        Checks.notNull(key, "key");
        Checks.notNull(value, "value");

        if(get(key) == null){
            ensureCapacity();
            Entry entry = new Entry(key, value);
            entries[used++] = entry;
        }else {
            throw new HttpServerExecption(String.format("Path parameter's key %s is duplicated", key));
        }
    }

    public String get(String key) {
        for (int i = 0; i < used; i++) {
            Entry entry = entries[i];
            if (entry.key.equals(key)) {
                return entry.value;
            }
        }

        return null;
    }

    public int size(){
        return used;
    }

    private void ensureCapacity(){
        if (used == entries.length){
            entries = Arrays.copyOf(entries, entries.length * 2);
        }
    }

    public Iterator<Entry> iterator(){
        return new PathParamsIterator(entries, used);
    }

    public static class PathParamsIterator implements Iterator<Entry>{
        private volatile Entry[] entries = new Entry[4];
        private int used = 0;
        private int cur = 0;

        public PathParamsIterator(Entry[] entries, int used) {
            this.entries = entries;
            this.used = used;
        }

        public boolean hasNext(){
            return used > cur;
        }

        public Entry next(){
            if (used <= cur){
                throw new NoSuchElementException();
            }

            return entries[cur++];
        }
    }


    public static class Entry{
        private String key;
        private String value;

        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
