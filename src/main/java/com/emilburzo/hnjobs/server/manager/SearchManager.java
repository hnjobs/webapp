package com.emilburzo.hnjobs.server.manager;

import com.emilburzo.hnjobs.shared.rpc.JobRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchLogRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.phrase.PhraseSuggestionBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class SearchManager {

    private static final String ELASTICSEARCH_HOST = "ELASTICSEARCH_HOST";
    private static final String ELASTICSEARCH_PORT = "ELASTICSEARCH_PORT";

    private static final String INDEX_HNJOBS = "hnjobs";
    private static final String TYPE_JOB = "job";
    private static final String TYPE_SEARCH = "search";

    private static final String FIELD_BODY = "body";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_BODY_HTML = "bodyHtml";
    private static final String FIELD_SCORE = "_score";

    private static final int MAX_SUGGESTIONS = 1;
    private static final int MAX_SEARCH_RESULTS = 100;

    private Logger log = Logger.getLogger(getClass().getSimpleName());

    private RestHighLevelClient client;

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
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        } finally {
            try {
                // cleanup
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private void log(String query, int results, long durationMs) {
        SearchLogRPC log = new SearchLogRPC(query, results, durationMs);

        try {
            IndexRequest request = new IndexRequest(INDEX_HNJOBS)
                    .source(new Gson().toJson(log), XContentType.JSON);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SearchResultRPC getResults(String query) {
        long start = new Date().getTime();

        SearchResultRPC rpc = new SearchResultRPC();

        try {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.simpleQueryStringQuery(query)
                            .field(FIELD_BODY_HTML)
            );
            searchSourceBuilder.sort(FIELD_SCORE, SortOrder.DESC);
            searchSourceBuilder.sort(FIELD_TIMESTAMP, SortOrder.DESC);

            SuggestBuilder suggestBuilder = new SuggestBuilder();
            suggestBuilder.addSuggestion("default",
                    new PhraseSuggestionBuilder(FIELD_BODY_HTML)
                            .text(query)
                            .size(MAX_SUGGESTIONS)
            );
            searchSourceBuilder.suggest(suggestBuilder);

            searchSourceBuilder.highlighter(new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder()
                    .field(FIELD_BODY_HTML)
            );
            searchSourceBuilder.from(0);
            searchSourceBuilder.size(MAX_SEARCH_RESULTS);
            searchSourceBuilder.explain(false);

            SearchRequest searchRequest = new SearchRequest(INDEX_HNJOBS);
            searchRequest.source(searchSourceBuilder);

            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            // suggested search
            try {
                // todo sanity checks instead of brute force
                rpc.suggestion = response.getSuggest().iterator().next().getEntries().get(0).getOptions().get(0).getText().string();
            } catch (Exception e) {
    //            e.printStackTrace();
            }

            // search hits
            for (SearchHit hit : response.getHits()) {
                JobRPC job = getResult(hit);
                rpc.jobs.add(job);
            }

            // benchmark query
            rpc.duration = (System.currentTimeMillis() - start);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rpc;
    }

    private JobRPC getResult(SearchHit hit) {
        try {
            GetRequest getRequest = new GetRequest(INDEX_HNJOBS, hit.getId());
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);

            JobRPC job = new JobRPC();

            job.id = hit.getId();
            job.timestamp = Long.valueOf(response.getSource().get(FIELD_TIMESTAMP).toString());
            job.author = response.getSource().get(FIELD_AUTHOR).toString();
            job.bodyHtml = getBody(hit);
            job.score = hit.getScore();

            return job;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the job post with the search query highlighted
     *
     * @param hit
     * @return
     */
    private String getBody(SearchHit hit) {
        if (hit == null || hit.getSource() == null) {
            return "";
        }

        if (hit.getHighlightFields() == null || hit.getHighlightFields().get(FIELD_BODY_HTML) == null) {
            return hit.getSource().get(FIELD_BODY_HTML).toString();
        }

        Text[] fragments = hit.getHighlightFields().get(FIELD_BODY_HTML).getFragments();

        String body = "";
        for (Text fragment : fragments) {
            body += fragment.string();
        }

        return body;
    }

    private void initEs() {
        String host = System.getenv().getOrDefault(ELASTICSEARCH_HOST, "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault(ELASTICSEARCH_PORT, "9200"));

        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, "http")
                )
        );
    }
}