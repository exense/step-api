/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.core.metrics;

import java.util.Map;

/**
 * Immutable snapshot of a {@link Metric}'s accumulated state at a point in time,
 * produced by {@link Metric#flush()}.
 * <p>
 * {@code MetricSnapshot} is the wire format sent to the controller: it contains only
 * plain serializable fields with no accumulator state. Subclasses carry the
 * type-specific data ({@link CounterSnapshot}, {@link SampledSnapshot}).
 * <p>
 * Getters are exposed as bean properties for Jackson serialization.
 * Setters are provided for Jackson deserialization on the controller side
 * without requiring Jackson annotations in this module.
 */
public abstract class MetricSnapshot {

    private long snapshotTimestamp;
    private String name;
    private Map<String, String> labels;
    private MetricType type;


    /** Required for Jackson deserialization. */
    protected MetricSnapshot() {
    }

    protected MetricSnapshot(long snapshotTimestamp, String name, Map<String, String> labels, MetricType type) {
        this.snapshotTimestamp = snapshotTimestamp;
        this.name = name;
        this.labels = labels;
        this.type = type;
    }

    /** Return the timestamp of this snapshot **/
    public long getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    /** Required for Jackson deserialization. */
    public void setSnapshotTimestamp(long snapshotTimestamp) {
        this.snapshotTimestamp = snapshotTimestamp;
    }

    /** Returns the metric name. */
    public String getName() {
        return name;
    }

    /** Required for Jackson deserialization. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the labels attached to this metric. */
    public Map<String, String> getLabels() {
        return labels;
    }

    /** Required for Jackson deserialization. */
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     * Returns the metric type. Used as a JSON discriminator so the controller
     * can deserialize the correct subclass without requiring Jackson annotations
     * in this module.
     */
    public MetricType getType() {
        return type;
    }

    /** Required for Jackson deserialization. */
    public void setType(MetricType type) {
        this.type = type;
    }
}
