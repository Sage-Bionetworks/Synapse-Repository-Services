package org.sagebionetworks.repo.model.upload;

public class PartRange {

	private long lowerBound;
	private long upperBound;
	private long numberOfParts;

	public PartRange(){}

	public long getUpperBound() {
		return upperBound;
	}

	public void setUpperBound(long upperBound) {
		this.upperBound = upperBound;
	}

	public long getLowerBound() {
		return lowerBound;
	}

	public void setLowerBound(long lowerBound) {
		this.lowerBound = lowerBound;
	}

	/**
	 * Returns the number of parts of the previous level needed to compose this part.
	 * 1 <= numberOfParts <= 32
	 */
	public long getNumberOfParts() {
		return numberOfParts;
	}

	public void setNumberOfParts(long numberOfParts) {
		this.numberOfParts = numberOfParts;
	}

	/**
	 * Returns the number of singleton parts contained in the range
	 * i.e. upperBound - lowerBound + 1
	 */
	public long getSize() {
		return upperBound - lowerBound + 1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) lowerBound;
		result = prime * result + (int) upperBound;
		result = prime * result + (int) numberOfParts;
		return result;
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
		if (lowerBound != other.lowerBound) return false;
		if (upperBound != other.upperBound) return false;
		if (numberOfParts != other.numberOfParts) return false;
		return true;
	}

	@Override
	public String toString() {
		return "PartRange ["
				+ "lowerBound=" + lowerBound
				+ ", upperBound=" + upperBound
				+ ", numberOfParts=" + numberOfParts
				+ "]";
	}
}
