package apodemas.sheepdog.http;

/**
 * @author caozheng
 * @time 2019-01-19 09:40
 **/
public class HttpUtils {
    public static String[] trySplitCharsetFromContentType(String contentType) {
        int charsetIndex = contentType.indexOf(";charset=");
        if (charsetIndex >= 0) {
            String charset = contentType.substring(charsetIndex + 9).toLowerCase();
            contentType = contentType.substring(0, charsetIndex);

            return new String[]{contentType, charset};
        }

        return new String[]{contentType, null};

    }

    public static String appendCharsetToContentType(String contentType, String charset) {
        return contentType + ";charset=" + charset;
    }
}
