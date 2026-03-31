package step.core.metrics;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GaugeMetricTest {

    @Test
    public void observeAndFlush_basicStats() {
        GaugeMetric gauge = new GaugeMetric("latency");
        gauge.observe(10);
        gauge.observe(20);
        gauge.observe(30);
        MetricSample snap = gauge.flush();

        Assert.assertEquals(MetricType.GAUGE, snap.getType());
        Assert.assertEquals(3, snap.getCount());
        Assert.assertEquals(60, snap.getSum());
        Assert.assertEquals(10, snap.getMin());
        Assert.assertEquals(30, snap.getMax());
        Assert.assertEquals(30, snap.getLast());
    }

    @Test
    public void flush_resetsAccumulators() {
        GaugeMetric gauge = new GaugeMetric("g");
        gauge.observe(100);
        gauge.flush();

        // Second interval with different values
        gauge.observe(5);
        MetricSample snap = gauge.flush();
        Assert.assertEquals(MetricType.GAUGE, snap.getType());

        Assert.assertEquals(1, snap.getCount());
        Assert.assertEquals(5, snap.getSum());
        Assert.assertEquals(5, snap.getMin());
        Assert.assertEquals(5, snap.getMax());
    }

    @Test
    public void flush_last_notReset() {
        GaugeMetric gauge = new GaugeMetric("g");
        gauge.observe(42);
        gauge.flush();
        // No new observations in second interval
        MetricSample snap = gauge.flush();

        Assert.assertEquals("last should persist across flushes", 42, snap.getLast());
        Assert.assertEquals("count should be 0 after empty interval", 0, snap.getCount());
    }

    @Test
    public void flush_returnsMetricSnapshot() {
        GaugeMetric gauge = new GaugeMetric("g");
        MetricSample snap = gauge.flush();
        Assert.assertNotNull(snap);
        Assert.assertTrue(snap instanceof MetricSample);
    }

    @Test
    public void getType_returnsGauge() {
        Assert.assertEquals(MetricType.GAUGE, new GaugeMetric("g").getType());
    }

    @Test
    public void distribution_populatedCorrectly() {
        GaugeMetric gauge = new GaugeMetric("g");
        gauge.observe(15); // bucket key: 10
        gauge.observe(18); // bucket key: 10
        gauge.observe(25); // bucket key: 20
        MetricSample snap = (MetricSample) gauge.flush();

        Assert.assertNotNull(snap.getDistribution());
        Assert.assertEquals(2, (long) snap.getDistribution().getOrDefault(10L, 0L));
        Assert.assertEquals(1, (long) snap.getDistribution().getOrDefault(20L, 0L));
    }

    @Test
    public void observe_threadSafety() throws InterruptedException {
        GaugeMetric gauge = new GaugeMetric("concurrent");
        int threads = 8;
        int observationsPerThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < observationsPerThread; j++) {
                    gauge.observe(1);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        MetricSample snap = (MetricSample) gauge.flush();

        Assert.assertEquals((long) threads * observationsPerThread, snap.getCount());
    }
}
