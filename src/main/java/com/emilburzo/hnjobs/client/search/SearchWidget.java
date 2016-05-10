package com.emilburzo.hnjobs.client.search;

import com.emilburzo.hnjobs.client.rpc.RPC;
import com.emilburzo.hnjobs.shared.rpc.JobRPC;
import com.emilburzo.hnjobs.shared.rpc.SearchResultRPC;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.constants.HeadingSize;
import org.gwtbootstrap3.client.ui.constants.ProgressBarType;
import org.gwtbootstrap3.client.ui.constants.ProgressType;
import org.gwtbootstrap3.extras.animate.client.ui.Animate;
import org.gwtbootstrap3.extras.animate.client.ui.constants.Animation;

import java.util.ArrayList;
import java.util.List;

public class SearchWidget extends SimplePanel {

    interface MyUiBinder extends UiBinder<Widget, SearchWidget> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField
    TextBox fieldInput;
    @UiField
    Button btnSearch;
    @UiField
    FlowPanel flow;
    @UiField
    PanelFooter panelSuggest;
    @UiField
    Button fieldSuggest;

    private ProgressBar progressBar;
    private Timer timer;

    public SearchWidget() {
        initUi();

        focusInput();

        handleToken();
    }

    private void initUi() {
        setWidget(uiBinder.createAndBindUi(this));

        showRandomSuggestion();
    }

    /**
     * When the page is done loading, focus the search input field
     */
    private void focusInput() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                fieldInput.setFocus(true);
            }
        });
    }

    private void handleToken() {
        // if there's a search fragment, do a search
        // e.g.: when giving someone a link with the search term already in the URL
        if (History.getToken() != null && !History.getToken().isEmpty()) {
            doSearch(History.getToken());
        }
    }

    private void showRandomSuggestion() {
        List<String> placeholders = new ArrayList<>();
        placeholders.add("remote Java");
        placeholders.add("remote Python");
        placeholders.add("remote Europe");
        placeholders.add("Mobile Software Engineer");
        placeholders.add("Technical Support Engineer");
        placeholders.add("UI Developer");
        placeholders.add("Full Stack Developer");
        placeholders.add("Technical Manager");
        placeholders.add("Frontend Developer");
        placeholders.add("Backend Developer");
        placeholders.add("Senior Software Engineer");
        placeholders.add("iOS Developer");
        placeholders.add("Data Scientist");
        placeholders.add("Product Owner");
        placeholders.add("Mobile App Designer");
        placeholders.add("Lead Front End Engineer");
        placeholders.add("DevOps Engineer");
        placeholders.add("US only");
        placeholders.add("EU timezones");
        placeholders.add("Android Engineer");
        placeholders.add("Backed by investors");
        placeholders.add("social VR");
        placeholders.add("healthcare startup");
        placeholders.add("remote only company");
        placeholders.add("distributed team");
        placeholders.add("healthcare");
        placeholders.add("onsite Rails");
        placeholders.add("VISA tranfers ok");
        placeholders.add("VISA sponsorship");
        placeholders.add("Information Security");
        placeholders.add("Android developer");
        placeholders.add("3D Developer");
        placeholders.add("fleet management");
        placeholders.add("self-driving cars");
        placeholders.add("voice fraud prevention");
        placeholders.add("fast growing startup");
        placeholders.add("Electrical Engineer");
        placeholders.add("system administration");
        placeholders.add("open-source");
        placeholders.add("experienced search engineer");
        placeholders.add("Hadoop Developer");
        placeholders.add("best places to work");
        placeholders.add("marketing platform");
        placeholders.add("ad tech company");
        placeholders.add("mobile gaming industry");

        // pick a random search suggestion
        fieldInput.setPlaceholder(placeholders.get(Random.nextInt(placeholders.size() - 1)));
    }

    @UiHandler("fieldSuggest")
    public void onSuggestion(ClickEvent event) {
        doSearch(fieldSuggest.getText());
    }

    private void doSearch(String query) {
        fieldInput.setValue(query);

        onSearch(null);
    }

    /**
     * Enable searching by pressing the "Enter" key
     *
     * @param event
     */
    @UiHandler("fieldInput")
    public void onEnter(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            onSearch(null);
        }
    }

    /**
     * Search icon event handler
     *
     * @param event
     */
    @UiHandler("btnSearch")
    public void onSearch(ClickEvent event) {
        String value = fieldInput.getValue().trim();

        // sanity check
        if (value.isEmpty()) {
            return;
        }

        // save the search query in the URL fragment
        History.newItem(value);

        // indicate to the user that we haven't frozen
        showProgressBar();

        // hide suggestion area
        panelSuggest.setVisible(false);

        // run the search query on the server
        RPC.service.search(value, new AsyncCallback<SearchResultRPC>() {

            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(SearchResultRPC result) {
                onSearchResults(result);
            }
        });

        // fake progress bar ... progress
        timer = new Timer() {
            @Override
            public void run() {
                if (progressBar.getPercent() >= 99) {
                    return;
                }

                progressBar.setPercent(progressBar.getPercent() + 1);
            }
        };

        timer.scheduleRepeating(10);
    }

    private void onSearchResults(SearchResultRPC result) {
        // stop the progress bar
        timer.cancel();
        progressBar.setPercent(100);

        // clear previous results
        flow.clear();

        // search suggestions
        if (result.suggestion != null) {
            fieldSuggest.setText(result.suggestion);
            panelSuggest.setVisible(true);
        }

        // load results
        for (JobRPC job : result.jobs) {
            JobWidget w = new JobWidget(job);

            flow.add(w);

            Animate.animate(w, Animation.FADE_IN_DOWN);
        }

        // prevent an empty page when there are no search results
        if (result.jobs.isEmpty()) {
            Jumbotron jumbo = new Jumbotron();
            jumbo.add(new Heading(HeadingSize.H3, "No results"));

            flow.add(jumbo);
        }
    }

    private void showProgressBar() {
        flow.clear();

        Progress progress = new Progress();
        progress.setType(ProgressType.STRIPED);

        progressBar = new ProgressBar();
        progressBar.setType(ProgressBarType.INFO);
        progressBar.setPercent(5);

        progress.add(progressBar);

        flow.add(progress);
    }
}
