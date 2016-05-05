package com.emilburzo.hnjobs.client.entry;

import com.emilburzo.hnjobs.client.search.SearchWidget;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class HnJobs implements EntryPoint {

    public void onModuleLoad() {
        // setup history handlers
//        History.addValueChangeHandler(); todo

        RootPanel.get("content").add(new SearchWidget());
    }
}
