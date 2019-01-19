package apodemas.sheepdog.common.url;

/**
 * @author caozheng
 * @time 2019-01-19 09:12
 **/
public class NameValue {
    private final String name;
    private final String value;

    public NameValue(final String name, final String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameValue that = (NameValue) o;
        return name.equals(that.name) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + name.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "NameValuePair{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
