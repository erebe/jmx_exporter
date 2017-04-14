package io.prometheus.jmx;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

public class MetricsServletAccess extends HttpServlet {

    private CollectorRegistry registry;
    private StringWriter writer;
    private int timestamp;

    /**
     * Construct a MetricsServletAccess for the default registry.
     */
    public MetricsServletAccess() {
        this(CollectorRegistry.defaultRegistry);
    }

    /**
     * Construct a MetricsServletAccess for the given registry.
     */
    public MetricsServletAccess(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        String token = req.getParameter("token");
        // Don't allow access if it doesn't provide a token
        if(!token.equalsIgnoreCase("ASK_THE_TEAM_FIRST_BEFORE_SCRAPING")) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType(TextFormat.CONTENT_TYPE_004);
            try (Writer writer = resp.getWriter()) {
                writer.write("Please contact the team to get access");
                writer.flush();
            }
            return;
        }

        // Allow safe passage
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(TextFormat.CONTENT_TYPE_004);

        try (Writer writer = resp.getWriter()) {
            TextFormat.write004(writer, registry.metricFamilySamples());
            writer.flush();
        }
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

}
