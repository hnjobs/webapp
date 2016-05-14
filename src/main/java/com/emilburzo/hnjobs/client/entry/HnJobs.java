package com.emilburzo.hnjobs.client.entry;

import com.emilburzo.hnjobs.client.search.SearchWidget;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class HnJobs implements EntryPoint {

    public void onModuleLoad() {
        final SearchWidget search = new SearchWidget();

        // setup history handler
        History.addValueChangeHandler(new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(ValueChangeEvent<String> event) {
                search.onHistoryTokenChanged();
            }
        });

        RootPanel.get("content").add(search);
    }
}
