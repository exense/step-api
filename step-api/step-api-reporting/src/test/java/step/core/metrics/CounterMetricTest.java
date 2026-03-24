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
        CounterSnapshot snap = counter.flush();

        Assert.assertEquals("accumulatedDiff should be 5 after 1+4 increments", 5, snap.getAccumulatedDiff());
        Assert.assertEquals("longRunningTotal should be 5", 5, snap.getLongRunningTotal());
    }

    @Test
    public void flush_resetsDiff_butNotTotal() {
        CounterMetric counter = new CounterMetric("requests");
        counter.increment(10);
        CounterSnapshot snap1 = counter.flush();

        Assert.assertEquals(10, snap1.getAccumulatedDiff());
        Assert.assertEquals(10, snap1.getLongRunningTotal());

        // Second interval
        counter.increment(3);
        CounterSnapshot snap2 = counter.flush();

        Assert.assertEquals("accumulatedDiff should only reflect second interval", 3, snap2.getAccumulatedDiff());
        Assert.assertEquals("longRunningTotal should be cumulative", 13, snap2.getLongRunningTotal());
    }

    @Test
    public void flush_withNoIncrements_returnsZeroDiff() {
        CounterMetric counter = new CounterMetric("idle");
        CounterSnapshot snap = counter.flush();

        Assert.assertEquals(0, snap.getAccumulatedDiff());
        Assert.assertEquals(0, snap.getLongRunningTotal());
    }

    @Test
    public void flush_returnsCounterSnapshot() {
        CounterMetric counter = new CounterMetric("c");
        MetricSnapshot snap = counter.flush();
        Assert.assertNotNull(snap);
        Assert.assertTrue(snap instanceof CounterSnapshot);
    }

    @Test
    public void getType_returnsCounter() {
        Assert.assertEquals(MetricType.COUNTER, new CounterMetric("c").getType());
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
        CounterSnapshot snap = counter.flush();

        long expected = (long) threads * incrementsPerThread;
        Assert.assertEquals(expected, snap.getAccumulatedDiff());
        Assert.assertEquals(expected, snap.getLongRunningTotal());
    }
}
