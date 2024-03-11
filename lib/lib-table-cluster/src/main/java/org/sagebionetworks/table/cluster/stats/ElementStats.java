package org.sagebionetworks.table.cluster.stats;

import java.util.Objects;


public class ElementStats {
	private final Long maximumSize;
	
	
	private ElementStats(Long maximumSize) {
		this.maximumSize = maximumSize;
	}
	
	public Long getMaximumSize() {
		return maximumSize;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(maximumSize);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ElementStats other = (ElementStats) obj;
		return Objects.equals(maximumSize, other.maximumSize);
	}

	@Override
	public String toString() {
		return "ElementStats [maximumSize=" + maximumSize + "]";
	}
	
	/**
	 * Addition for Longs that can be null.
	 * 
	 * @param currentValue
	 * @param newValue
	 * @return
	 */
	public static Long addLongsWithNull(Long one, Long two) {
		if(one == null) {
			return two;
		}
		if(two == null) {
			return one;
		}
		return one + two;
	}

	public static Builder builder() {
		return new Builder();
	}
	
	public Builder cloneBuilder() {
		return new Builder()
				.setMaximumSize(this.maximumSize);
	}
	
	public static class Builder {
		private Long maximumSize;		
		
		public Builder setMaximumSize(Long maximumSize) {
			this.maximumSize = maximumSize;
			return this;
		}

		public ElementStats build() {
			return new ElementStats(maximumSize);
		}
	}
	
}
