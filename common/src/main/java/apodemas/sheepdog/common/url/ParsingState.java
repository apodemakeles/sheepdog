package apodemas.sheepdog.common.url;

/**
 * @author caozheng
 * @time 2019-01-19 09:12
 **/
public enum  ParsingState {
    SCHEME_START,
    SCHEME,
    AUTHORITY,
    HOST,
    PORT,
    RELATIVE_PATH_START,
    RELATIVE_PATH,
    QUERY,
    FRAGMENT
}
