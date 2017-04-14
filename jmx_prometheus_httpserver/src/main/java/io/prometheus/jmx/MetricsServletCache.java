package io.prometheus.jmx;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

public class MetricsServletCache extends HttpServlet {

    public static final int DEFAULT_TTL_IN_SEC = 60 * 1;

    private CollectorRegistry registry;
    private final StringWriter writerCache;
    private final int ttl;
    private AtomicLong lastUpdateTs;
    private ReentrantReadWriteLock lock;

    /**
     * Construct a MetricsServletAccess for the default registry.
     */
    public MetricsServletCache() {
        this(CollectorRegistry.defaultRegistry, DEFAULT_TTL_IN_SEC);
    }

    /**
     * Construct a MetricsServletAccess for the given registry.
     */
    public MetricsServletCache(CollectorRegistry registry, int ttlInSec) {
        this.registry = registry;
        this.writerCache = new StringWriter();
        this.ttl = ttlInSec * 1000;
        this.lastUpdateTs = new AtomicLong();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(TextFormat.CONTENT_TYPE_004);

        final long now = System.currentTimeMillis();
        final long lastUpdate = lastUpdateTs.get();
        if (lastUpdate + ttl <= now && lastUpdateTs.compareAndSet(lastUpdate, now)) {
            // Atomic compare and set succeeded, so we will be the only one to update the cache
            Enumeration<Collector.MetricFamilySamples> samples = registry.metricFamilySamples();
            lock.writeLock().lock();
            try {
                System.out.println("Updating the cache");
                writerCache.getBuffer().setLength(0);
                TextFormat.write004(writerCache, samples);
                writerCache.flush();
            } finally {
                lock.writeLock().unlock();
            }
        }

        lock.readLock().lock();
        try (Writer writer = resp.getWriter()) {
            writer.write(writerCache.toString());
            writer.flush();
        } finally {
            lock.readLock().unlock();
        }

    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }

}
