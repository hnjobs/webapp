package com.emilburzo.hnjobs.client.search;

import com.emilburzo.hnjobs.shared.rpc.JobRPC;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.Heading;

import java.util.Date;

public class JobWidget extends SimplePanel {

    interface MyUiBinder extends UiBinder<Widget, JobWidget> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField
    Heading fieldHeading;
    @UiField
    HTML fieldBody;
    @UiField
    Anchor fieldFooter;

    private final JobRPC job;

    public JobWidget(JobRPC job) {
        this.job = job;

        initUi();

        loadValues();
    }

    private void initUi() {
        setWidget(uiBinder.createAndBindUi(this));
    }

    private void loadValues() {
        fieldHeading.setText(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_FULL).format(new Date(job.timestamp)));
        fieldHeading.setTitle(String.valueOf(job.score));

        fieldBody.setHTML(job.bodyHtml);

        fieldFooter.setText("Original post");
        fieldFooter.setHref("https://news.ycombinator.com/item?id=" + job.id);
        fieldFooter.setTarget("_blank");
    }
}
