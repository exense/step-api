package step.reporting.fixme;

import step.core.reports.Measure;

import java.util.List;

@FunctionalInterface
public interface LiveMeasureContextListener {
    void accept(List<Measure> measures);
}
