package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

public class MetricsServletRateLimit extends HttpServlet {

    public static final int DEFAULT_RATE_ONCE_EVERY = 60 * 1;

    private CollectorRegistry registry;
    private final int rate;
    private long lastUpdateTs;

    /**
     * Construct a MetricsServletAccess for the default registry.
     */
    public MetricsServletRateLimit() {
        this(CollectorRegistry.defaultRegistry, DEFAULT_RATE_ONCE_EVERY);
    }

    /**
     * Construct a MetricsServletAccess for the given registry.
     */
    public MetricsServletRateLimit(CollectorRegistry registry, int maxRateInSec) {
        this.registry = registry;
        this.rate = maxRateInSec * 1000;
        this.lastUpdateTs = 0;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {


        final long now = System.currentTimeMillis();
        if(lastUpdateTs + rate > now) {
            resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            resp.setContentType(TextFormat.CONTENT_TYPE_004);
            try (Writer writer = resp.getWriter()) {
                writer.write("Request failed due to rate limit, please try again in " + (rate / 1000) + " sec");
                writer.flush();
            }
            return;
        }

        lastUpdateTs = now;
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
