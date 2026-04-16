package step.core.metrics;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CounterMetricTest {

    @Test
    public void incrementAndFlush_singleThread() {
        CounterMetric counter = new CounterMetric("requests");
        counter.increment();
        counter.increment(4);
        MetricSample snap = counter.flush();

        Assert.assertEquals("accumulatedDiff should be 5 after 1+4 increments", 5, snap.getCount());
        Assert.assertEquals("longRunningTotal should be 5", 5, snap.getMax());
    }

    @Test
    public void incrementAndFlush_negativeValue() {
        CounterMetric counter = new CounterMetric("requests");
        Assert.assertThrows(IllegalArgumentException.class, ()->counter.increment(-4));
    }

    @Test
    public void flush_resetsDiff_butNotTotal() {
        CounterMetric counter = new CounterMetric("requests");
        counter.increment(10);
        MetricSample snap1 = counter.flush();

        Assert.assertEquals(10, snap1.getCount());
        Assert.assertEquals(10, snap1.getMax());

        // Second interval
        counter.increment(3);
        MetricSample snap2 = counter.flush();

        Assert.assertEquals("accumulatedDiff should only reflect second interval", 3, snap2.getCount());
        Assert.assertEquals("longRunningTotal should be cumulative", 13, snap2.getMax());
    }

    @Test
    public void flush_withNoIncrements_returnsZeroDiff() {
        CounterMetric counter = new CounterMetric("idle");
        MetricSample snap = counter.flush();

        Assert.assertEquals(0, snap.getCount());
        Assert.assertEquals(0, snap.getMax());
    }

    @Test
    public void flush_returnsMetricSnapshot() {
        CounterMetric counter = new CounterMetric("c");
        MetricSample snap = counter.flush();
        Assert.assertNotNull(snap);
        Assert.assertTrue(snap instanceof MetricSample);
    }

    @Test
    public void getType_returnsCounter() {
        Assert.assertEquals(InstrumentType.COUNTER, new CounterMetric("c").getType());
    }

    @Test
    public void increment_threadSafety() throws InterruptedException {
        CounterMetric counter = new CounterMetric("concurrent");
        int threads = 10;
        int incrementsPerThread = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        MetricSample snap = counter.flush();

        long expected = (long) threads * incrementsPerThread;
        Assert.assertEquals(expected, snap.getCount());
        Assert.assertEquals(expected, snap.getMax());
    }
}
