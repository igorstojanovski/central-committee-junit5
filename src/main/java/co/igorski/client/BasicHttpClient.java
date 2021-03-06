package co.igorski.client;

import co.igorski.exceptions.SnitcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.StringJoiner;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This implementation uses only Java native classes to implement the {@link WebClient} interface.
 */
public class BasicHttpClient implements WebClient {

    private static final Logger LOG = LoggerFactory.getLogger(BasicHttpClient.class);

    @Override
    public int login(String target, Map<String, String> form) throws IOException, SnitcherException {

        if (target == null) {
            throw new SnitcherException("Target must not be null");
        }

        URL url;
        url = new URL(target);

        HttpURLConnection conn = getPostConnection(url, "application/x-www-form-urlencoded");
        try (OutputStream os = conn.getOutputStream();
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(os, UTF_8))) {
            writer.write(getPostDataString(form));
            writer.flush();
        }

        return conn.getResponseCode();
    }

    @Override
    public String post(String target, String body) throws IOException, SnitcherException {

        if (target == null) {
            throw new SnitcherException("Target must not be null");
        }

        URL url;
        url = new URL(target);

        HttpURLConnection conn = getPostConnection(url, "application/json");

        try (OutputStream os = conn.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, UTF_8))) {
            writer.write(body);
            writer.flush();
        }

        return getStringFromInputStream(conn.getInputStream());
    }

    private HttpURLConnection getPostConnection(URL url, String s) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(15000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", s);
        return conn;
    }

    private String getStringFromInputStream(InputStream inputStream) {

        String response = null;
        try (InputStream in = inputStream; ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            response = result.toString(UTF_8);
        } catch (IOException e) {
            LOG.error("Could not read response from server.", e);
        }

        return response;
    }

    private String getPostDataString(Map<String, String> params) {
        StringJoiner result = new StringJoiner("&");

        for (Map.Entry<String, String> entry : params.entrySet()) {

            result.add(URLEncoder.encode(entry.getKey(), UTF_8) + "=" +
                    URLEncoder.encode(entry.getValue(), UTF_8));
        }

        return result.toString();
    }
}
