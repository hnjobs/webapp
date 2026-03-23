package com.emilburzo.hnjobs.server.health;

import com.google.inject.Singleton;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Singleton
public class ReadyServlet extends HttpServlet {

    private static final String ELASTICSEARCH_HOST = "ELASTICSEARCH_HOST";
    private static final String ELASTICSEARCH_PORT = "ELASTICSEARCH_PORT";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");

        String host = System.getenv().getOrDefault(ELASTICSEARCH_HOST, "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault(ELASTICSEARCH_PORT, "9200"));

        try (RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, port, "http")))) {
            if (client.ping(RequestOptions.DEFAULT)) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"status\":\"ok\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("{\"status\":\"elasticsearch unreachable\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().write("{\"status\":\"elasticsearch unreachable\",\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
