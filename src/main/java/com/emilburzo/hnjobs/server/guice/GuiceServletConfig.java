package com.emilburzo.hnjobs.server.guice;

import com.emilburzo.hnjobs.server.filter.NoCacheFilter;
import com.emilburzo.hnjobs.server.health.LiveServlet;
import com.emilburzo.hnjobs.server.health.ReadyServlet;
import com.emilburzo.hnjobs.server.rpc.ServiceImpl;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

public class GuiceServletConfig extends GuiceServletContextListener {

    @Override
    protected Injector getInjector() {
        ServletModule servletModule = new ServletModule() {

            @Override
            protected void configureServlets() {
                // filters
                filter("/*").through(NoCacheFilter.class);

                // health checks
                serve("/healthz/live").with(LiveServlet.class);
                serve("/healthz/ready").with(ReadyServlet.class);

                // GWT RPC
                serve(ServiceImpl.ENDPOINT).with(ServiceImpl.class);
            }
        };

        return Guice.createInjector(Stage.PRODUCTION, servletModule);

    }
}
