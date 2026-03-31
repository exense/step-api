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

    /** Creates a builder with a very large interval so only the very first observation ever
     *  triggers a rate-limited flush — useful for isolating "within-interval" behaviour. */
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
    public void firstObservation_alwaysCreatesASample() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(7);

        Assert.assertEquals("first observation must produce exactly one sample", 1, streamed.size());
        Assert.assertEquals(7, streamed.get(0).getCount());
    }

    // -------------------------------------------------------------------------
    // Rapid observations within the flush interval
    // -------------------------------------------------------------------------

    @Test
    public void rapidObservations_withinInterval_onlyFirstFlushes() {
        List<MetricSample> streamed = new ArrayList<>();
        // Use a very large interval so subsequent observations never cross it
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(1); // first → flush immediately (sample 1: count=1)
        counter.increment(2); // within interval → accumulates
        counter.increment(3); // within interval → accumulates

        Assert.assertEquals("only one sample from the first observation", 1, streamed.size());
        Assert.assertEquals(1, streamed.get(0).getCount());
    }

    @Test
    public void rapidObservations_withinInterval_finalFlushCapturesAccumulated() {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithLargeInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(1); // flush: count=1
        counter.increment(2); // accumulate
        counter.increment(3); // accumulate

        List<MetricSample> all = builder.getSamples(); // final flush: count=5 (2+3)

        Assert.assertEquals("two samples: first observation + final flush", 2, all.size());
        Assert.assertEquals("first sample carries the first observation only", 1, all.get(0).getCount());
        Assert.assertEquals("final flush carries all accumulated values", 5, all.get(1).getCount());
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

        counter.increment(3); // flush: sample 1 (count=3)
        counter.increment(2); // within interval → accumulates

        Assert.assertEquals(1, streamed.size());

        Thread.sleep(SLEEP_MS); // interval elapses

        // Next observation triggers a flush of everything accumulated since last flush (2+5)
        counter.increment(5);

        Assert.assertEquals("second flush triggered after interval", 2, streamed.size());
        Assert.assertEquals("second sample holds values accumulated since the previous flush",
                7, streamed.get(1).getCount()); // 2 accumulated + 5 new
    }

    @Test
    public void multipleIntervals_correctSampleCountAndValues() throws InterruptedException {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithShortInterval(streamed);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        // --- interval 1 ---
        counter.increment(3); // flush: sample 1 (count=3)
        counter.increment(2); // accumulates in interval 1

        Thread.sleep(SLEEP_MS);

        // --- interval 2 ---
        counter.increment(7); // flush: sample 2 (count=2+7=9)
        counter.increment(4); // accumulates in interval 2

        Thread.sleep(SLEEP_MS);

        // --- interval 3 ---
        counter.increment(1); // flush: sample 3 (count=4+1=5)

        Assert.assertEquals(3, streamed.size());
        Assert.assertEquals("interval 1", 3, streamed.get(0).getCount());
        Assert.assertEquals("interval 2: accumulated+trigger", 9, streamed.get(1).getCount());
        Assert.assertEquals("interval 3: accumulated+trigger", 5, streamed.get(2).getCount());

        // Final flush: nothing left since interval 3 already flushed
        List<MetricSample> all = builder.getSamples();
        Assert.assertEquals("no extra sample when nothing accumulated since last flush",
                3, all.size());

        // Total increments must be conserved across all samples
        long totalCount = all.stream().mapToLong(MetricSample::getCount).sum();
        Assert.assertEquals("sum of all sample counts equals total increments",
                3 + 2 + 7 + 4 + 1, totalCount);
    }

    @Test
    public void gauge_multipleIntervals_correctStatisticsPerSample() throws InterruptedException {
        List<MetricSample> streamed = new ArrayList<>();
        MetricSamplesBuilder builder = builderWithShortInterval(streamed);
        GaugeMetric gauge = new GaugeMetric("latency");
        builder.register(gauge);

        // --- interval 1 ---
        gauge.observe(10); // flush: sample 1 (count=1, sum=10, min=10, max=10)
        gauge.observe(20); // accumulates

        Thread.sleep(SLEEP_MS);

        // --- interval 2 ---
        gauge.observe(30); // flush: sample 2 (count=2, sum=50, min=20, max=30)
        gauge.observe(40); // accumulates

        Assert.assertEquals(2, streamed.size());

        MetricSample s1 = streamed.get(0);
        Assert.assertEquals(1,  s1.getCount());
        Assert.assertEquals(10, s1.getSum());
        Assert.assertEquals(10, s1.getMin());
        Assert.assertEquals(10, s1.getMax());

        MetricSample s2 = streamed.get(1);
        Assert.assertEquals(2,  s2.getCount());
        Assert.assertEquals(50, s2.getSum()); // 20+30
        Assert.assertEquals(20, s2.getMin());
        Assert.assertEquals(30, s2.getMax());

        // Final flush: observe(40) still accumulated
        List<MetricSample> all = builder.getSamples();
        Assert.assertEquals(3, all.size());
        MetricSample s3 = all.get(2);
        Assert.assertEquals(1,  s3.getCount());
        Assert.assertEquals(40, s3.getSum());

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

        counter.increment(5); // sample 1

        // getSamples() performs a final flush — counter has count=0, must be filtered
        List<MetricSample> all = builder.getSamples();

        Assert.assertEquals("final flush on idle metric must not produce a sample", 1, all.size());
        Assert.assertEquals(1, streamed.size()); // forwardConsumer not called again
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

        counter.increment(5); // sample 1

        Thread.sleep(SLEEP_MS); // interval elapses — but no new observation happens

        // Final flush: nothing accumulated → count=0 → filtered
        List<MetricSample> all = builder.getSamples();

        Assert.assertEquals("elapsed interval with no observations must not generate a sample",
                1, all.size());
    }

    // -------------------------------------------------------------------------
    // Batch mode and addSamples
    // -------------------------------------------------------------------------

    @Test
    public void batchMode_noForwardConsumer_samplesReturnedBySamples() {
        // No forward consumer: batch mode
        MetricSamplesBuilder builder = new MetricSamplesBuilder(Integer.MAX_VALUE, null);
        CounterMetric counter = new CounterMetric("c");
        builder.register(counter);

        counter.increment(9);
        counter.increment(1); // within interval, accumulates

        List<MetricSample> all = builder.getSamples();

        Assert.assertEquals(2, all.size()); // first observation + final flush
        Assert.assertEquals(9, all.get(0).getCount());
        Assert.assertEquals(1, all.get(1).getCount());
    }

    @Test
    public void addSamples_mergesPreexistingSamplesIntoCollected() {
        MetricSamplesBuilder builder = new MetricSamplesBuilder();
        MetricSample pre = new MetricSample(System.currentTimeMillis(), "pre",
                Collections.emptyMap(), MetricType.COUNTER, 42, 42, 42, 42, 42, null);

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
