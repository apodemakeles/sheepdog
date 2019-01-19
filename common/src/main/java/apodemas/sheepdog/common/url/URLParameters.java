package apodemas.sheepdog.common.url;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 09:10
 **/
public class URLParameters implements Iterable<NameValue> {
    private static final List<NameValue> EMPTY_NAME_VALUES = Collections.unmodifiableList(new ArrayList<NameValue>(0));

    private final List<NameValue> nameValues;

    URLParameters(final String query) {
        if (query != null && !query.isEmpty()) {
            nameValues = Collections.unmodifiableList(FormURLEncodedParser.parse(query));
        } else {
            nameValues = EMPTY_NAME_VALUES;
        }
    }

    URLParameters(final List<NameValue> nameValues) {
        if (nameValues == null) {
            throw new NullPointerException("nameValues");
        }
        this.nameValues = Collections.unmodifiableList(nameValues);
    }

    public int size() {
        return nameValues == null ? 0 : nameValues.size();
    }

    public URLParameters withAppended(final String name, final String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        return withAppended(new NameValue(name, value));
    }

    public URLParameters withAppended(final NameValue nameValue) {
        if (nameValue == null) {
            throw new NullPointerException("nameValue");
        }
        final List<NameValue> newNameValuesList = new ArrayList<NameValue>(this.nameValues.size() + 1);
        for (final NameValue nv : nameValues) {
            newNameValuesList.add(nv);
        }
        newNameValuesList.add(nameValue);
        return new URLParameters(newNameValuesList);
    }

    public URLParameters with(final String name, final String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        return with(new NameValue(name, value));
    }

    public URLParameters with(final NameValue nameValue) {
        if (nameValue == null) {
            throw new NullPointerException("nameValue");
        }
        final List<NameValue> newNameValuesList = new ArrayList<NameValue>(this.nameValues.size() + 1);
        final String name = nameValue.name();
        for (final NameValue nv : nameValues) {
            if (!nv.name().equals(name)) {
                newNameValuesList.add(nv);
            }
        }
        newNameValuesList.add(nameValue);
        return new URLParameters(newNameValuesList);
    }

    public URLParameters without(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        final List<NameValue> newNameValuesList = new ArrayList<NameValue>(this.nameValues.size());
        for (final NameValue nv : nameValues) {
            if (!nv.name().equals(name)) {
                newNameValuesList.add(nv);
            }
        }
        return new URLParameters(newNameValuesList);
    }

    public String get(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        for (final NameValue nv : nameValues) {
            if (name.equals(nv.name())) {
                return nv.value();
            }
        }
        return null;
    }

    public List<String> getAll(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        final List<String> result = new ArrayList<String>();
        for (final NameValue nv : nameValues) {
            if (name.equals(nv.name())) {
                result.add(nv.value());
            }
        }
        return result;
    }

    public boolean has(final String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        for (final NameValue nv : nameValues) {
            if (name.equals(nv.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<NameValue> iterator() {
        return nameValues.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        URLParameters that = (URLParameters) o;

        if (!nameValues.equals(that.nameValues)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return nameValues.hashCode();
    }
}
