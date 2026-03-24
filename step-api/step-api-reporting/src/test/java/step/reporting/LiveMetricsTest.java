package step.reporting;

import org.junit.Assert;
import org.junit.Test;
import step.core.metrics.CounterMetric;
import step.core.metrics.CounterSnapshot;
import step.core.metrics.GaugeMetric;
import step.core.metrics.Metric;
import step.core.metrics.MetricSnapshot;
import step.reporting.impl.LiveMetricDestination;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LiveMetricsTest {

    /** Captures accepted metrics for assertion. */
    private static class CapturingDestination implements LiveMetricDestination {
        final List<Metric> received = new ArrayList<>();

        @Override
        public synchronized void accept(Metric metric) {
            received.add(metric);
        }

        public List<MetricSnapshot> flushAll() {
            return received.stream().map(Metric::flush).collect(Collectors.toList());
        }
    }

    @Test
    public void flushAll_dispatchesAllRegisteredMetrics() {
        CapturingDestination dest = new CapturingDestination();
        LiveMetrics liveMetrics = new LiveMetrics(dest);

        CounterMetric counter = new CounterMetric("requests");
        counter.increment(5);
        liveMetrics.register(counter);

        List<MetricSnapshot> snapshots = dest.flushAll();

        Assert.assertEquals(1, dest.received.size());
        Assert.assertSame(counter, dest.received.get(0));
        CounterSnapshot snap = (CounterSnapshot) snapshots.get(0);
        Assert.assertEquals(5, snap.getAccumulatedDiff());
    }

    @Test
    public void nullDestination_usesDiscardingDefault_noException() {
        LiveMetrics liveMetrics = new LiveMetrics(null);
        CounterMetric counter = new CounterMetric("c");
        counter.increment();
        liveMetrics.register(counter); // should not throw
    }

    @Test
    public void multipleMetrics_allFlushed() {
        CapturingDestination dest = new CapturingDestination();
        LiveMetrics liveMetrics = new LiveMetrics(dest);

        CounterMetric c = new CounterMetric("c");
        GaugeMetric g = new GaugeMetric("g");
        c.increment(3);
        g.observe(99);
        liveMetrics.register(c);
        liveMetrics.register(g);

        List<MetricSnapshot> snapshots = dest.flushAll();

        Assert.assertEquals(2, snapshots.size());
    }

    @Test
    public void flushAll_resetsAccumulators_betweenIntervals() {
        CapturingDestination dest = new CapturingDestination();
        LiveMetrics liveMetrics = new LiveMetrics(dest);

        CounterMetric counter = new CounterMetric("c");
        counter.increment(10);
        liveMetrics.register(counter);

        List<MetricSnapshot> snapshots1 = dest.flushAll();
        CounterSnapshot snap1 = (CounterSnapshot) snapshots1.get(0);
        Assert.assertEquals(10, snap1.getAccumulatedDiff());
        Assert.assertEquals(10, snap1.getLongRunningTotal());
        Assert.assertEquals(1, dest.received.size()); // registered metrics

        // No new increments — diff should be zero on next flush
        List<MetricSnapshot> snapshots2 = dest.flushAll();
        CounterSnapshot snap2 = (CounterSnapshot) snapshots2.get(0);
        Assert.assertEquals(0, snap2.getAccumulatedDiff());
        Assert.assertEquals(10, snap2.getLongRunningTotal());
        Assert.assertEquals(1, dest.received.size()); // registered metrics
    }
}
