package com.emilburzo.hnjobs.client.rpc;

import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface ServiceAsync {

    void search(String query, AsyncCallback<SearchResultRPC> callback);
}
