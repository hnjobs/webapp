package com.emilburzo.hnjobs.client.rpc;

import com.google.gwt.core.client.GWT;

public class RPC {

    public static final ServiceAsync service = GWT.create(Service.class);

}
