package step.reporting.impl;

import step.core.reports.Measure;

public interface LiveMeasureDestination {
    void accept(Measure measure);
    default void close() {}
}
