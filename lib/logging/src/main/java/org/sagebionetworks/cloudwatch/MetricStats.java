package org.sagebionetworks.cloudwatch;

import java.util.Objects;

public class MetricStats {
	private Double maximum;
	private Double minimum;
	private Double count;
	private Double sum;

	public Double getMaximum() {
		return maximum;
	}

	public MetricStats setMaximum(Double maximum) {
		this.maximum = maximum;
		return this;
	}

	public Double getMinimum() {
		return minimum;
	}

	public MetricStats setMinimum(Double minimum) {
		this.minimum = minimum;
		return this;
	}

	public Double getCount() {
		return count;
	}

	public MetricStats setCount(Double count) {
		this.count = count;
		return this;
	}

	public Double getSum() {
		return sum;
	}

	public MetricStats setSum(Double sum) {
		this.sum = sum;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(count, maximum, minimum, sum);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MetricStats other = (MetricStats) obj;
		return Objects.equals(count, other.count) && Objects.equals(maximum, other.maximum)
				&& Objects.equals(minimum, other.minimum) && Objects.equals(sum, other.sum);
	}

	@Override
	public String toString() {
		return "MetricStats [maximum=" + maximum + ", minimum=" + minimum + ", count=" + count + ", sum=" + sum + "]";
	}
}
