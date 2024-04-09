package org.sagebionetworks.repo.model.dbo.file.google;

import java.util.Objects;

public class PartRange {

	private Long lowerBound;
	private Long upperBound;

	public Long getLowerBound() {
		return lowerBound;
	}

	public PartRange setLowerBound(Long lowerBound) {
		this.lowerBound = lowerBound;
		return this;
	}

	public Long getUpperBound() {
		return upperBound;
	}

	public PartRange setUpperBound(Long upperBound) {
		this.upperBound = upperBound;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lowerBound, upperBound);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PartRange other = (PartRange) obj;
		return lowerBound == other.lowerBound && upperBound == other.upperBound;
	}

	@Override
	public String toString() {
		return "PartRange [lowerBound=" + lowerBound + ", upperBound=" + upperBound + "]";
	}

}
