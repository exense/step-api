package step.reporting.impl;

import step.core.reports.Measure;

public interface LiveMeasureSink {
    void accept(Measure measure);
    void close();
}
