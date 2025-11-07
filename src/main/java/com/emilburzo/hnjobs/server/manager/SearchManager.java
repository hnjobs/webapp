package com.emilburzo.hnjobs.server.manager;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.emilburzo.hnjobs.shared.rpc.JobRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchLogRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SearchManager {

    private static final String ELASTICSEARCH_HOST = "ELASTICSEARCH_HOST";
    private static final String ELASTICSEARCH_PORT = "ELASTICSEARCH_PORT";

    private static final String INDEX_HNJOBS = "hnjobs";

    private static final String FIELD_BODY = "body";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_BODY_HTML = "bodyHtml";
    private static final String FIELD_SCORE = "_score";

    private static final int MAX_SUGGESTIONS = 1;
    private static final int MAX_SEARCH_RESULTS = 100;

    private Logger log = Logger.getLogger(getClass().getSimpleName());

    private ElasticsearchClient client;
    private RestClient restClient;

    public SearchResultRPC search(String query) {
        try {
            // init connection to elasticsearch
            initEs();

            // get results
            SearchResultRPC results = getResults(query);

            // log query for relevance analysis
            log(query, results.jobs.size(), results.duration);

            // return results to the client
            return results;
        } catch (Exception e) {
            log.severe("Error during search: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // cleanup
            try {
                if (restClient != null) {
                    restClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private void log(String query, int results, long durationMs) {
        SearchLogRPC logEntry = new SearchLogRPC(query, results, durationMs);

        try {
            Map<String, Object> document = new HashMap<>();
            document.put("query", query);
            document.put("results", results);
            document.put("durationMs", durationMs);

            IndexResponse response = client.index(i -> i
                    .index(INDEX_HNJOBS)
                    .document(document)
            );
        } catch (IOException e) {
            log.warning("Failed to log search: " + e.getMessage());
        }
    }

    private SearchResultRPC getResults(String query) throws IOException {
        long start = new Date().getTime();

        SearchResultRPC rpc = new SearchResultRPC();

        // Build the search query
        SearchResponse<Map> response = client.search(s -> s
                        .index(INDEX_HNJOBS)
                        .query(q -> q
                                .simpleQueryString(sqs -> sqs
                                        .query(query)
                                        .fields(FIELD_BODY_HTML)
                                )
                        )
                        .sort(so -> so
                                .score(sc -> sc.order(SortOrder.Desc))
                        )
                        .sort(so -> so
                                .field(f -> f
                                        .field(FIELD_TIMESTAMP)
                                        .order(SortOrder.Desc)
                                )
                        )
                        .suggest(sugg -> sugg
                                .suggesters("default", sg -> sg
                                        .text(query)
                                        .phrase(p -> p
                                                .field(FIELD_BODY_HTML)
                                                .size(MAX_SUGGESTIONS)
                                        )
                                )
                        )
                        .highlight(h -> h
                                .fields(FIELD_BODY_HTML, hf -> hf)
                        )
                        .from(0)
                        .size(MAX_SEARCH_RESULTS),
                Map.class
        );

        // suggested search
        try {
            if (response.suggest() != null && response.suggest().get("default") != null) {
                var suggestions = response.suggest().get("default");
                if (!suggestions.isEmpty() && !suggestions.get(0).phrase().options().isEmpty()) {
                    rpc.suggestion = suggestions.get(0).phrase().options().get(0).text();
                }
            }
        } catch (Exception e) {
            // No suggestion available
        }

        // search hits
        for (Hit<Map> hit : response.hits().hits()) {
            JobRPC job = getResult(hit);
            rpc.jobs.add(job);
        }

        // benchmark query
        rpc.duration = (System.currentTimeMillis() - start);

        return rpc;
    }

    private JobRPC getResult(Hit<Map> hit) throws IOException {
        GetResponse<Map> response = client.get(g -> g
                        .index(INDEX_HNJOBS)
                        .id(hit.id()),
                Map.class
        );

        JobRPC job = new JobRPC();

        job.id = hit.id();
        if (response.source() != null) {
            job.timestamp = Long.valueOf(response.source().get(FIELD_TIMESTAMP).toString());
            job.author = response.source().get(FIELD_AUTHOR).toString();
        }
        job.bodyHtml = getBody(hit);
        job.score = hit.score() != null ? hit.score().floatValue() : 0.0f;

        return job;
    }

    /**
     * Returns the job post with the search query highlighted
     *
     * @param hit
     * @return
     */
    private String getBody(Hit<Map> hit) {
        if (hit == null || hit.source() == null) {
            return "";
        }

        // Check for highlights first
        if (hit.highlight() != null && hit.highlight().get(FIELD_BODY_HTML) != null) {
            List<String> fragments = hit.highlight().get(FIELD_BODY_HTML);
            StringBuilder body = new StringBuilder();
            for (String fragment : fragments) {
                body.append(fragment);
            }
            return body.toString();
        }

        // Return original body if no highlights
        Object bodyHtml = hit.source().get(FIELD_BODY_HTML);
        return bodyHtml != null ? bodyHtml.toString() : "";
    }

    private void initEs() throws IOException {
        String host = System.getenv().getOrDefault(ELASTICSEARCH_HOST, "hnjobs");
        int port = Integer.parseInt(System.getenv().getOrDefault(ELASTICSEARCH_PORT, "9200"));

        // Create the low-level client
        restClient = RestClient.builder(
                new HttpHost(host, port, "http")
        ).build();

        // Create the transport with a Jackson mapper
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );

        // Create the API client
        client = new ElasticsearchClient(transport);
    }
}
