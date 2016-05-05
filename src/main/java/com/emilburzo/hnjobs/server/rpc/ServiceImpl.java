package com.emilburzo.hnjobs.server.rpc;

import com.emilburzo.hnjobs.client.rpc.Service;
import com.emilburzo.hnjobs.shared.rpc.JobRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchLogRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gson.Gson;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;

/**
 * The server-side implementation of the RPC service.
 */
public class ServiceImpl extends RemoteServiceServlet implements Service {

    private static final String INDEX_HNJOBS = "hnjobs";
    private static final String TYPE_JOB = "job";
    private static final String TYPE_SEARCH = "search";

    private static final String FIELD_BODY = "body";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_AUTHOR = "author";
    private static final String FIELD_BODY_HTML = "bodyHtml";
    private static final String FIELD_SCORE = "_score";

    private TransportClient client;

    @Override
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
        } catch (UnknownHostException e) {
            // todo
            e.printStackTrace();
        } finally {
            // cleanup
            client.close();
        }

        return null;
    }

    private void log(String query, int results, long durationMs) {
        SearchLogRPC log = new SearchLogRPC(query, results, durationMs);

        IndexResponse response = client.prepareIndex(INDEX_HNJOBS, TYPE_SEARCH)
                .setSource(new Gson().toJson(log))
                .get();
    }

    private SearchResultRPC getResults(String query) {
        long start = new Date().getTime();

        SearchResultRPC rpc = new SearchResultRPC();

        SearchResponse response = client.prepareSearch(INDEX_HNJOBS)
                .setTypes(TYPE_JOB)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(QueryBuilders.matchQuery(FIELD_BODY_HTML, query))
                .addSort(FIELD_SCORE, SortOrder.DESC)
                .addSort(FIELD_TIMESTAMP, SortOrder.DESC)
                .addHighlightedField(FIELD_BODY_HTML, 0, 0)
                .setPostFilter(QueryBuilders.rangeQuery(FIELD_TIMESTAMP).gte(getMonthsAgo(2)))
                .setFrom(0).setSize(100).setExplain(false)
                .execute()
                .actionGet();

        for (SearchHit hit : response.getHits()) {
            JobRPC job = getResult(hit);
            rpc.jobs.add(job);
        }

        long stop = new Date().getTime();

        rpc.duration = (stop - start);

        return rpc;
    }

    private long getMonthsAgo(int months) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -months);

        return cal.getTimeInMillis();
    }

    private JobRPC getResult(SearchHit hit) {
        GetResponse response = client.prepareGet(INDEX_HNJOBS, TYPE_JOB, hit.getId()).get();

        JobRPC job = new JobRPC();

        job.id = hit.getId();
        job.timestamp = Long.valueOf(response.getSource().get(FIELD_TIMESTAMP).toString());
        job.author = response.getSource().get(FIELD_AUTHOR).toString();
        job.bodyHtml = getBody(hit);
        job.score = hit.getScore();

        return job;
    }

    /**
     * Returns the job post with the search query highlighted
     *
     * @param hit
     * @return
     */
    private String getBody(SearchHit hit) {
        Text[] fragments = hit.getHighlightFields().get(FIELD_BODY_HTML).getFragments();

        String body = "";
        for (Text fragment : fragments) {
            body += fragment.string();
        }

        return body;
    }

    private void initEs() throws UnknownHostException {
        client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("hnjobs"), 9300));
    }
}
