package step.core.metrics;

import org.junit.Assert;
import org.junit.Test;

public class HistogramMetricTest {

    @Test
    public void getType_returnsHistogram() {
        Assert.assertEquals(MetricType.HISTOGRAM, new HistogramMetric("h").getType());
    }

    @Test
    public void observeAndFlush_sameSemanticAsGauge() {
        HistogramMetric histogram = new HistogramMetric("response_time");
        histogram.observe(100);
        histogram.observe(200);
        histogram.observe(150);
        SampledSnapshot snap = (SampledSnapshot) histogram.flush();

        Assert.assertEquals(3, snap.getCount());
        Assert.assertEquals(450, snap.getSum());
        Assert.assertEquals(100, snap.getMin());
        Assert.assertEquals(200, snap.getMax());
    }

    @Test
    public void flush_resetsAccumulators() {
        HistogramMetric histogram = new HistogramMetric("h");
        histogram.observe(500);
        histogram.flush();

        histogram.observe(10);
        SampledSnapshot snap = (SampledSnapshot) histogram.flush();

        Assert.assertEquals(1, snap.getCount());
        Assert.assertEquals(10, snap.getSum());
    }
}
