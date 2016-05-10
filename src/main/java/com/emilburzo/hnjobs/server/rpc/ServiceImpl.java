package com.emilburzo.hnjobs.server.rpc;

import com.emilburzo.hnjobs.client.rpc.Service;
import com.emilburzo.hnjobs.server.manager.SearchManager;
import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * The server-side implementation of the RPC service.
 */
@Singleton
public class ServiceImpl extends RemoteServiceServlet implements Service {

    public static final String ENDPOINT = "/hnjobs/rpc";

    @Inject
    Injector injector;

    @Override
    public SearchResultRPC search(String query) {
        SearchManager manager = injector.getInstance(SearchManager.class);
        return manager.search(query);
    }

}
