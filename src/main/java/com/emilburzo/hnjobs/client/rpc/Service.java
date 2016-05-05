package com.emilburzo.hnjobs.client.rpc;

import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client-side stub for the RPC service.
 */
@RemoteServiceRelativePath("rpc")
public interface Service extends RemoteService {

    SearchResultRPC search(String query);
}
