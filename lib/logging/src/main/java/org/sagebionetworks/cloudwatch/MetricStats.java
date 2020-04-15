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

	public void setMaximum(Double maximum) {
		this.maximum = maximum;
	}

	public Double getMinimum() {
		return minimum;
	}

	public void setMinimum(Double minimum) {
		this.minimum = minimum;
	}

	public Double getCount() {
		return count;
	}

	public void setCount(Double count) {
		this.count = count;
	}

	public Double getSum() {
		return sum;
	}

	public void setSum(Double sum) {
		this.sum = sum;
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

}
