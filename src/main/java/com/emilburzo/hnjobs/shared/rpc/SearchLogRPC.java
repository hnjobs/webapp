package com.emilburzo.hnjobs.shared.rpc;

import java.util.Date;

public class SearchLogRPC {

    public String query;
    public int results;
    public long durationMs;
    public long timestamp = new Date().getTime();

    public SearchLogRPC() {
    }

    public SearchLogRPC(String query, int results, long durationMs) {
        this.query = query;
        this.results = results;
        this.durationMs = durationMs;
    }
}
