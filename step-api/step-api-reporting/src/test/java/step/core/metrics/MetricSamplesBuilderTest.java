package step.core.metrics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetricSamplesBuilderTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a builder with a very large interval so intermediate observations never cross it —
     *  useful for isolating "within-interval" and "final flush only" behaviour. */
    private static MetricSamplesBuilder builderWithLargeInterval(List<MetricSample> streamed) {
        return new MetricSamplesBuilder(Integer.MAX_VALUE, streamed::add);
    }

    /** Creates a builder whose interval expires after a short {@code Thread.sleep(SLEEP_MS)}. */
    private static final long SHORT_INTERVAL_MS = 50;
    private static final long SLEEP_MS = 120; // generous margin over SHORT_INTERVAL_MS

    private static MetricSamplesBuilder builderWithShortInterval(List<MetricSample> streamed) {
        return new MetricSamplesBuilder(SHORT_INTERVAL_MS, streamed::add);
    }

    // -------------------------------------------------------------------------
    // First observation
    // -------------------------------------------------------------------------

    @Test
    public void firstObservation_startsClockWithoutFlushing() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(7);

        // First observation must only start the interval clock — no intermediate flush yet.
        // The accumulated value is captured by the final flush in getSamples().
        Assert.assertEquals("first observation must not produce an intermediate sample", 0, streamed.size());

        List<MetricSample> all = builder.getSamples();
        Assert.assertEquals("final flush must capture the first observation", 1, all.size());
        Assert.assertEquals(7, all.get(0).getSum());
    }

    // -------------------------------------------------------------------------
    // Rapid observations within the flush interval
    // -------------------------------------------------------------------------

    @Test
    public void rapidObservations_withinInterval_noneFlushIntermediate() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(1); // first → starts clock, no flush
        counter.increment(2); // within interval → accumulates
        counter.increment(3); // within interval → accumulates

        Assert.assertEquals("no intermediate samples within the flush interval", 0, streamed.size());
    }

    @Test
    public void rapidObservations_withinInterval_finalFlushCapturesAll() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(1);
        counter.increment(2);
        counter.increment(3);

        List<MetricSample> all = builder.getSamples(); // final flush: count=1+2+3=6

        Assert.assertEquals("one sample from the final flush only", 1, all.size());
        Assert.assertEquals("final flush captures all accumulated values", 6, all.get(0).getSum());
    }

    // -------------------------------------------------------------------------
    // Observations spanning multiple flush intervals  (the "> 5 s" scenario)
    // -------------------------------------------------------------------------

    @Test
    public void observationAfterIntervalElapses_createsNewSample_withAccumulatedValues()
            throws InterruptedException {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithShortInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(3); // starts clock, no flush
        counter.increment(2); // within interval → accumulates

        Assert.assertEquals("no intermediate flush yet", 0, streamed.size());

        Thread.sleep(SLEEP_MS); // interval elapses

        // Next observation crosses the interval boundary: flushes everything accumulated (3+2+5)
        counter.increment(5);

        Assert.assertEquals("flush triggered after interval", 1, streamed.size());
        Assert.assertEquals("sample holds all values accumulated since the clock started",
                10, streamed.get(0).getSum()); // 3+2+5
    }

    @Test
    public void multipleIntervals_correctSampleCountAndValues() throws InterruptedException {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithShortInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        // --- before first interval boundary ---
        counter.increment(3); // starts clock
        counter.increment(2); // accumulates

        Thread.sleep(SLEEP_MS);

        // --- interval boundary 1: flush includes everything accumulated so far + trigger ---
        counter.increment(7); // flush: count=3+2+7=12
        counter.increment(4); // accumulates in next window

        Thread.sleep(SLEEP_MS);

        // --- interval boundary 2 ---
        counter.increment(1); // flush: count=4+1=5

        Assert.assertEquals(2, streamed.size());
        Assert.assertEquals("window 1: all values up to and including the triggering observation",
                12, streamed.get(0).getSum());
        Assert.assertEquals("window 2: accumulated+trigger", 5, streamed.get(1).getSum());

        // Final flush: nothing left since last flush had no subsequent observations
        List<MetricSample> all = builder.getSamples();
        Assert.assertEquals("no extra sample when nothing accumulated since last flush",
                2, all.size());

        // Total increments must be conserved across all samples
        int expectedRunningTotal = 3 + 2 + 7 + 4 + 1;
        long totalCount = all.stream().mapToLong(MetricSample::getSum).sum();
        Assert.assertEquals("sum of all sample sum equals total increments",
            expectedRunningTotal, totalCount);
        // Running total final value should also match
        Assert.assertEquals(expectedRunningTotal, all.get(1).getLast());
        Assert.assertEquals(expectedRunningTotal, all.get(1).getMax());
    }

    @Test
    public void gauge_multipleIntervals_correctStatisticsPerSample() throws InterruptedException {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithShortInterval(streamed);
        GaugeMetric gauge = new GaugeMetric("latency");
        builder.register(gauge);

        // --- before first interval boundary ---
        gauge.observe(10); // starts clock, no flush
        gauge.observe(20); // accumulates

        Thread.sleep(SLEEP_MS);

        // --- interval boundary 1: flush includes 10, 20, and the triggering 30 ---
        gauge.observe(30); // flush: count=3, sum=60, min=10, max=30
        gauge.observe(40); // accumulates in next window

        Assert.assertEquals(1, streamed.size());

        MetricSample s1 = streamed.get(0);
        Assert.assertEquals(3,  s1.getCount());
        Assert.assertEquals(60, s1.getSum()); // 10+20+30
        Assert.assertEquals(10, s1.getMin());
        Assert.assertEquals(30, s1.getMax());

        // Final flush: observe(40) still accumulated
        List<MetricSample> all = builder.getSamples();
        Assert.assertEquals(2, all.size());
        MetricSample s2 = all.get(1);
        Assert.assertEquals(1,  s2.getCount());
        Assert.assertEquals(40, s2.getSum());

        // All observations accounted for
        long totalCount = all.stream().mapToLong(MetricSample::getCount).sum();
        Assert.assertEquals(4, totalCount);
    }

    // -------------------------------------------------------------------------
    // No duplicate samples when there are no new values
    // -------------------------------------------------------------------------

    @Test
    public void getSamples_noNewObservationsSinceLastFlush_noDuplicateSample() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(5); // starts clock, no intermediate flush

        // getSamples() performs the final flush: count=5 → published
        List<MetricSample> all = builder.getSamples();

        Assert.assertEquals(1, all.size());
        Assert.assertEquals(1, streamed.size()); // forwardConsumer called once via final flush

        // Second getSamples() call: count=0 — must not produce another sample
        List<MetricSample> second = builder.getSamples();
        Assert.assertEquals("idle final flush must not produce a duplicate sample", 1, second.size());
        Assert.assertEquals(1, streamed.size()); // forwardConsumer still called only once
    }

    @Test
    public void getSamples_calledTwice_noDuplicates() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(5);

        List<MetricSample> first  = builder.getSamples();
        List<MetricSample> second = builder.getSamples();

        Assert.assertEquals(1, first.size());
        Assert.assertEquals(1, second.size()); // second call adds nothing new
        Assert.assertEquals(1, streamed.size()); // forwardConsumer called exactly once
    }

    @Test
    public void getSamples_afterIntervalWithNoObservations_noDuplicateSample()
            throws InterruptedException {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithShortInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(5); // starts clock, no intermediate flush

        Thread.sleep(SLEEP_MS); // interval elapses — but no new observation happens

        // Final flush: count=5 → one sample (the elapsed interval alone does not create one)
        List<MetricSample> all = builder.getSamples();

        Assert.assertEquals("elapsed interval with no observations must not generate an extra sample",
                1, all.size());
    }

    // -------------------------------------------------------------------------
    // Batch mode and addSamples
    // -------------------------------------------------------------------------

    @Test
    public void batchMode_noForwardConsumer_samplesReturnedBySamples() {
        // No forward consumer: batch mode — all samples collected via getSamples()
        MetricSamplesBuilder builder = new MetricSamplesBuilder(Integer.MAX_VALUE, null);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(9); // starts clock, no flush
        counter.increment(1); // within interval, accumulates

        List<MetricSample> all = builder.getSamples(); // final flush: count=10

        Assert.assertEquals("all observations collected in the single final flush", 1, all.size());
        Assert.assertEquals(10, all.get(0).getSum());
    }

    @Test
    public void addSamples_mergesPreexistingSamplesIntoCollected() {
        MetricSamplesBuilder builder = new MetricSamplesBuilder();
        MetricSample pre = new MetricSample(System.currentTimeMillis(), "pre",
                Collections.emptyMap(), InstrumentType.COUNTER, 42, 42, 42, 42, 42, null);

        builder.addSamples(List.of(pre));

        List<MetricSample> all = builder.getSamples();
        Assert.assertEquals(1, all.size());
        Assert.assertSame(pre, all.get(0));
    }

    @Test
    public void addSamples_nullOrEmpty_ignored() {
        MetricSamplesBuilder builder = new MetricSamplesBuilder();
        builder.addSamples(null);
        builder.addSamples(Collections.emptyList());
        Assert.assertEquals(0, builder.getSamples().size());
    }
}
