package webserver.dat.sem2;

/**
 * Adapted from https://github.com/eguahlak/thin-web
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

class HttpRequest {

    private final String method;
    private final String path;
    private final String protocol;
    private int contentLength = 0;
    private final byte[] body;

    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> cookies = new HashMap<>();

    private String readLine_old(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        do {
            // all characters in the header is ASCII = single byte characters
            char c = (char) in.read(); // returns -1 when end of stream is reached.
            //System.out.print(c == 0 ? '#' : c);
            if (c == '\r') {
                continue; // standard line break on the internet is \r\n while on Unix system itś only \n
            }
            if (c == '\n' || c == 65535) break; //char is 16bit type. 65,535 -- that's 2^16 - 1 which is 0xFFFF in hexadecimal. It's what you get if you cast -1 (which is 0xFFFFFFFF) to a char.

            builder.append(c);
        } while (true);
        return builder.toString();
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            // all characters in the header is ASCII = single byte characters
            int nextByte = in.read(); // returns -1 when end of stream is reached.
            if (nextByte == -1) {
                break;
            }
            char c = (char) nextByte;
            if (c == '\r') {
                continue; // standard line break on the internet is \r\n while on Unix system itś only \n
            }
            if (c == '\n') {
                break; //char is 16bit type. 65,535 -- that's 2^16 - 1 which is 0xFFFF in hexadecimal. It's what you get if you cast -1 (which is 0xFFFFFFFF) to a char.
            }
            builder.append(c);
        }
        return builder.toString();
    }

    private byte[] read(InputStream in, int number) throws IOException {
        byte[] buffer = new byte[number];
        int count = in.read(buffer);
        return buffer;
    }

    private void setQuery(String query) throws UnsupportedEncodingException {
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            parameters.put(
                    URLDecoder.decode(pair[0], "UTF-8"),
                    URLDecoder.decode(pair[1], "UTF-8")
            );
        }
    }

    HttpRequest(InputStream in) throws IOException {
        String firstLine = readLine(in);
        System.out.println("First line in the request: "+firstLine);
        String[] parts = firstLine.split(" ");
        if (parts.length != 3) {
            throw new IOException("Bad request");
        }
        method = parts[0].toLowerCase();
        String[] resourceParts = parts[1].split("\\?", 2);
        path = resourceParts[0];
        if (resourceParts.length == 2) {
            setQuery(resourceParts[1]);
        }
        protocol = parts[2];

        do {
            String line = readLine(in).trim();
            if (line.isEmpty()) {
                break;
            }
            String[] pair = line.split(":");
            String key = pair[0].trim();
            String value = pair[1].trim();
            headers.put(key, value);
            if (key.equalsIgnoreCase("Content-Length")) {
                contentLength = Integer.valueOf(value);
            } else if (key.equals("Cookie")) {
                String[] cookieParts = value.split(";");
                for (String cookiePart : cookieParts) {
                    String[] cookiePair = cookiePart.split("=", 2);
                    cookies.put(cookiePair[0].trim(), cookiePair[1].trim());
                }
            }
        } while (true);
        byte[] buffer = read(in, contentLength);
        if ("application/x-www-form-urlencoded".equals(getContentType())) {
            setQuery(new String(buffer, "UTF-8"));
            body = new byte[0];
        } else {
            body = buffer;
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getParameter(String key) {
        return parameters.get(key);
    }

    public String getCookie(String key) {
        return cookies.get(key);
    }

    public String getSessionId() {
        return cookies.get("SID");
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public byte[] getBody() {
        return body;
    }

    public boolean hasBody() {
        return contentLength > 0;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getContentType() {
        String mime = headers.get("Content-Type");
        if (mime == null) {
            mime = "text/plain";
        }
        int pos = mime.indexOf(';');
        if (pos >= 0) {
            mime = mime.substring(0, pos - 1);
        }
        return mime;
    }

}
