package apodemas.sheepdog.common.url;

/**
 * @author caozheng
 * @time 2019-01-19 09:09
 **/
public class URL {
    private final String scheme;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String path;
    private final String query;
    private final String fragment;
    private URLParameters parameters;


    public URL(String scheme, String username, String password, String host, int port, String path, String query, String fragment) {
        this.scheme = scheme;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
    }

    public String scheme(){
        return scheme;
    }

    public String username(){
        return username;
    }

    public String password(){
        return password;
    }

    public String userInfo() {
        if (password == null) {
            return username;
        }
        return String.format("%s:%s", username, password);
    }

    public String host() {
        return host;
    }


    public int port() {
        return (port == -1)? defaultPort() : port;
    }

    private static int defaultPort(final String scheme) {
        String defaultPort = URLUtils.getDefaultPortForScheme(scheme);
        if (defaultPort == null) {
            return -1;
        }
        return Integer.parseInt(defaultPort);
    }

    public int defaultPort() {
        return defaultPort(scheme);
    }

    public String path() {
        return path;
    }

    private void buildPathQueryFragment(StringBuilder output){
        output.append(path);

        if (query != null) {
            output.append('?').append(query);
        }

        if (fragment != null) {
            output.append('#').append(fragment);
        }
    }

    public String pathQueryFragment(){
        StringBuilder sb = new StringBuilder();
        buildPathQueryFragment(sb);

        return sb.toString();
    }

    public String query() {
        return query;
    }

    public URLParameters parameters() {
        if(parameters == null){
            parameters = new URLParameters(query);
        }

        return parameters;
    }

    public String fragment() {
        return fragment;
    }

    @Override
    public String toString() {
        final StringBuilder output = new StringBuilder();

        if (scheme != null) {
            output.append(scheme).append(':');
            output.append("//");
        }
        final String userInfo = userInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            output.append(userInfo).append('@');
        }
        if (host != null) {
            output.append(host);
        }
        if (port != -1) {
            output.append(':').append(port);
        }

        buildPathQueryFragment(output);

        return output.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof URL)) {
            return false;
        }
        final URL other = (URL) obj;
        return  ((scheme == null)? other.scheme == null : scheme.equals(other.scheme)) &&
                ((username == null)? other.username == null : username.equals(other.username)) &&
                ((password == null)? other.password == null : password.equals(other.password)) &&
                ((host == null)? other.host == null : host.equals(other.host)) &&
                port == other.port &&
                ((path == null)? other.host == null : path.equals(other.path)) &&
                ((fragment == null)? other.fragment == null : fragment.equals(other.fragment)) &&
                ((query == null)? other.query == null : query.equals(other.query))
                ;
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != -1 ? port : 0);
        result = 31 * result + (path != null? path.hashCode() : 0);
        result = 31 * result + (query != null ? query.hashCode() + 1 : 0);
        result = 31 * result + (fragment != null ? fragment.hashCode() + 1 : 0);
        return result;
    }
}
