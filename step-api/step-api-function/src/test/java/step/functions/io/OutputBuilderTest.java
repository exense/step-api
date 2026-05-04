/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.functions.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import step.core.metrics.CounterMetric;
import step.core.metrics.GaugeMetric;
import step.core.metrics.HistogramMetric;
import step.core.metrics.InstrumentType;
import step.core.metrics.MetricSample;
import step.core.reports.Measure;

public class OutputBuilderTest {

    // -------------------------------------------------------------------------
    // Measure tests
    // -------------------------------------------------------------------------

    @Test
    public void startStop_measure_appearsInOutput() {
        OutputBuilder b = new OutputBuilder();
        b.startMeasure("step1");
        b.stopMeasure();
        List<Measure> measures = b.build().getMeasures();
        Assert.assertEquals(1, measures.size());
        Assert.assertEquals("step1", measures.get(0).getName());
        Assert.assertTrue(measures.get(0).getDuration() >= 0);
    }

    @Test
    public void startMeasure_withExplicitBegin_durationReflectsOffset() {
        OutputBuilder b = new OutputBuilder();
        long begin = System.currentTimeMillis() - 500;
        b.startMeasure("step2", begin);
        b.stopMeasure();
        Measure m = b.build().getMeasures().get(0);
        Assert.assertEquals("step2", m.getName());
        Assert.assertTrue("duration should reflect the explicit begin offset", m.getDuration() >= 500);
    }

    @Test
    public void addMeasure_byNameAndDuration_appearsInOutput() {
        OutputBuilder b = new OutputBuilder();
        b.addMeasure("step3", 200L);
        Measure m = b.build().getMeasures().get(0);
        Assert.assertEquals("step3", m.getName());
        Assert.assertEquals(200L, m.getDuration());
    }

    @Test
    public void addMeasure_withBegin_beginIsPreserved() {
        long begin = 12345L;
        OutputBuilder b = new OutputBuilder();
        b.addMeasure("step4", 300L, begin);
        Measure m = b.build().getMeasures().get(0);
        Assert.assertEquals(300L, m.getDuration());
        Assert.assertEquals(begin, m.getBegin());
    }

    @Test
    public void addMeasure_withData_dataIsPreserved() {
        OutputBuilder b = new OutputBuilder();
        b.addMeasure("step5", 100L, Map.of("key", "value"));
        Measure m = b.build().getMeasures().get(0);
        Assert.assertEquals("value", m.getData().get("key"));
    }

    @Test
    public void addMeasure_withBeginAndData_allFieldsPreserved() {
        long begin = 1000L;
        OutputBuilder b = new OutputBuilder();
        b.addMeasure("step6", 150L, begin, Map.of("k", "v"));
        Measure m = b.build().getMeasures().get(0);
        Assert.assertEquals("step6", m.getName());
        Assert.assertEquals(150L, m.getDuration());
        Assert.assertEquals(begin, m.getBegin());
        Assert.assertEquals("v", m.getData().get("k"));
    }

    @Test
    public void addMeasure_measureObject_addedDirectly() {
        Measure measure = new Measure("step7", 250L, 0L, null);
        OutputBuilder b = new OutputBuilder();
        b.addMeasure(measure);
        List<Measure> measures = b.build().getMeasures();
        Assert.assertEquals(1, measures.size());
        Assert.assertSame(measure, measures.get(0));
    }

    @Test
    public void stopMeasure_withFailedStatus_statusIsPreserved() {
        OutputBuilder b = new OutputBuilder();
        b.startMeasure("step8");
        b.stopMeasure(Measure.Status.FAILED);
        Assert.assertEquals(Measure.Status.FAILED, b.build().getMeasures().get(0).getStatus());
    }

    @Test
    public void stopMeasure_withTechnicalErrorStatus_statusIsPreserved() {
        OutputBuilder b = new OutputBuilder();
        b.startMeasure("step8b");
        b.stopMeasure(Measure.Status.TECHNICAL_ERROR);
        Assert.assertEquals(Measure.Status.TECHNICAL_ERROR, b.build().getMeasures().get(0).getStatus());
    }

    @Test
    public void stopMeasure_withData_dataIsPreserved() {
        OutputBuilder b = new OutputBuilder();
        b.startMeasure("step9");
        b.stopMeasure(Map.of("result", "ok"));
        Assert.assertEquals("ok", b.build().getMeasures().get(0).getData().get("result"));
    }

    @Test
    public void stopMeasure_withStatusAndData_bothPreserved() {
        OutputBuilder b = new OutputBuilder();
        b.startMeasure("step10");
        b.stopMeasure(Measure.Status.TECHNICAL_ERROR, Map.of("reason", "timeout"));
        Measure m = b.build().getMeasures().get(0);
        Assert.assertEquals(Measure.Status.TECHNICAL_ERROR, m.getStatus());
        Assert.assertEquals("timeout", m.getData().get("reason"));
    }

    @Test
    public void stopMeasureForAdditionalData_thenSetData_dataAppearsInMeasure() {
        OutputBuilder b = new OutputBuilder();
        b.startMeasure("step11");
        b.stopMeasureForAdditionalData();
        b.setLastMeasureAdditionalData(Map.of("post", "data"));
        Assert.assertEquals("data", b.build().getMeasures().get(0).getData().get("post"));
    }

    @Test
    public void multipleMeasures_allAppearInOutputInOrder() {
        OutputBuilder b = new OutputBuilder();
        b.addMeasure("m1", 100L);
        b.addMeasure("m2", 200L);
        b.addMeasure("m3", 300L);
        List<Measure> measures = b.build().getMeasures();
        Assert.assertEquals(3, measures.size());
        Assert.assertEquals("m1", measures.get(0).getName());
        Assert.assertEquals("m2", measures.get(1).getName());
        Assert.assertEquals("m3", measures.get(2).getName());
    }

    @Test
    public void noMeasures_getMeasuresReturnsEmptyList() {
        OutputBuilder b = new OutputBuilder();
        List<Measure> measures = b.build().getMeasures();
        Assert.assertNotNull(measures);
        Assert.assertTrue(measures.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Metric factory method tests
    // -------------------------------------------------------------------------

    @Test
    public void newCounter_incrementsAppearInOutput() {
        OutputBuilder b = new OutputBuilder();
        CounterMetric c = b.newCounter("hits");
        c.increment(3);
        c.increment(2);
        List<MetricSample> metrics = b.build().getMetrics();
        Assert.assertNotNull(metrics);
        Assert.assertEquals(1, metrics.size());
        MetricSample s = metrics.get(0);
        Assert.assertEquals("hits", s.getName());
        Assert.assertEquals(InstrumentType.COUNTER, s.getType());
        Assert.assertEquals(5, s.getSum());
    }

    @Test
    public void newCounter_withLabels_labelsPreserved() {
        OutputBuilder b = new OutputBuilder();
        CounterMetric c = b.newCounter("req", Map.of("env", "prod"));
        c.increment();
        MetricSample s = b.build().getMetrics().get(0);
        Assert.assertEquals("req", s.getName());
        Assert.assertEquals("prod", s.getLabels().get("env"));
    }

    @Test
    public void newGauge_observationsAppearInOutput() {
        OutputBuilder b = new OutputBuilder();
        GaugeMetric g = b.newGauge("cpu");
        g.observe(40);
        g.observe(60);
        MetricSample s = b.build().getMetrics().get(0);
        Assert.assertEquals("cpu", s.getName());
        Assert.assertEquals(InstrumentType.GAUGE, s.getType());
        Assert.assertEquals(2, s.getCount());
        Assert.assertEquals(100, s.getSum());
        Assert.assertEquals(40, s.getMin());
        Assert.assertEquals(60, s.getMax());
    }

    @Test
    public void newGauge_withLabels_labelsPreserved() {
        OutputBuilder b = new OutputBuilder();
        GaugeMetric g = b.newGauge("mem", Map.of("host", "srv1"));
        g.observe(1024);
        Assert.assertEquals("srv1", b.build().getMetrics().get(0).getLabels().get("host"));
    }

    @Test
    public void newHistogram_observationsAppearInOutput() {
        OutputBuilder b = new OutputBuilder();
        HistogramMetric h = b.newHistogram("latency");
        h.observe(100);
        h.observe(200);
        h.observe(150);
        MetricSample s = b.build().getMetrics().get(0);
        Assert.assertEquals("latency", s.getName());
        Assert.assertEquals(InstrumentType.HISTOGRAM, s.getType());
        Assert.assertEquals(3, s.getCount());
        Assert.assertEquals(450, s.getSum());
    }

    @Test
    public void newHistogram_withLabels_labelsPreserved() {
        OutputBuilder b = new OutputBuilder();
        HistogramMetric h = b.newHistogram("rt", Map.of("region", "eu"));
        h.observe(50);
        Assert.assertEquals("eu", b.build().getMetrics().get(0).getLabels().get("region"));
    }

    @Test
    public void addMetric_preBuilt_appearsInOutput() {
        OutputBuilder b = new OutputBuilder();
        GaugeMetric g = new GaugeMetric("temp");
        g.observe(37);
        b.addMetric(g);
        MetricSample s = b.build().getMetrics().get(0);
        Assert.assertEquals("temp", s.getName());
        Assert.assertEquals(37, s.getSum());
    }

    @Test
    public void noMetrics_getMetricsReturnsNull() {
        OutputBuilder b = new OutputBuilder();
        Assert.assertNull(b.build().getMetrics());
    }

    @Test
    public void multipleMetricTypes_allAppearInOutput() {
        OutputBuilder b = new OutputBuilder();
        b.newCounter("c1").increment(1);
        b.newGauge("g1").observe(10);
        b.newHistogram("h1").observe(50);
        List<MetricSample> metrics = b.build().getMetrics();
        Assert.assertEquals(3, metrics.size());
    }

    // -------------------------------------------------------------------------
    // mergeOutput tests
    // -------------------------------------------------------------------------

    @Test
    public void mergeOutput_mergesPayload() {
        OutputBuilder source = new OutputBuilder();
        source.add("key1", "value1");
        Output<JsonObject> sourceOutput = source.build();

        OutputBuilder target = new OutputBuilder();
        target.add("key2", "value2");
        target.mergeOutput(sourceOutput);

        JsonObject payload = target.build().getPayload();
        Assert.assertEquals("value1", payload.getString("key1"));
        Assert.assertEquals("value2", payload.getString("key2"));
    }

    @Test
    public void mergeOutput_mergesMeasures() {
        OutputBuilder source = new OutputBuilder();
        source.addMeasure("m1", 100L);
        Output<JsonObject> sourceOutput = source.build();

        OutputBuilder target = new OutputBuilder();
        target.addMeasure("m2", 200L);
        target.mergeOutput(sourceOutput);

        Assert.assertEquals(2, target.build().getMeasures().size());
    }

    @Test
    public void mergeOutput_mergesMetrics() {
        OutputBuilder source = new OutputBuilder();
        source.newCounter("c1").increment(5);
        Output<JsonObject> sourceOutput = source.build();

        OutputBuilder target = new OutputBuilder();
        target.mergeOutput(sourceOutput);

        List<MetricSample> metrics = target.build().getMetrics();
        Assert.assertNotNull(metrics);
        Assert.assertEquals(1, metrics.size());
        Assert.assertEquals("c1", metrics.get(0).getName());
        Assert.assertEquals(5, metrics.get(0).getSum());
    }

    @Test
    public void mergeOutput_mergesError() {
        OutputBuilder source = new OutputBuilder();
        source.setError("error from source");
        Output<JsonObject> sourceOutput = source.build();

        OutputBuilder target = new OutputBuilder();
        target.mergeOutput(sourceOutput);

        Assert.assertNotNull(target.build().getError());
        Assert.assertEquals("error from source", target.build().getError().getMsg());
    }
}
