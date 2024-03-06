
package org.sagebionetworks.table.cluster.stats;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;

public class ElementStats {
	private final Long maximumSize;
	private final Long maxListLength;
	private final String defaultValue;
	private final FacetType facetType;
	private final List<String> enumValues;
	private final List<JsonSubColumnModel> jsonSubColumns;
	
	
	private ElementStats(Long maximumSize, Long maxListLength, String defaultValue, FacetType facetType,
			List<String> enumValues, List<JsonSubColumnModel> jsonSubColumns) {
		this.maximumSize = maximumSize;
		this.maxListLength = maxListLength;
		this.defaultValue = defaultValue;
		this.facetType = facetType;
		this.enumValues = enumValues;
		this.jsonSubColumns = jsonSubColumns;
	}
	
	public Long getMaximumSize() {
		return maximumSize;
	}

	public Long getMaxListLength() {
		return maxListLength;
	}

	public String getDefaultValue() {
		return defaultValue;
	}
	
	public FacetType getFacetType() {
		return facetType;
	}

	public List<String> getEnumValues() {
		return enumValues;
	}

	public List<JsonSubColumnModel> getJsonSubColumns() {
		return jsonSubColumns;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(defaultValue, enumValues, facetType, jsonSubColumns, maxListLength, maximumSize);
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
		return Objects.equals(defaultValue, other.defaultValue) && Objects.equals(enumValues, other.enumValues)
				&& facetType == other.facetType && Objects.equals(jsonSubColumns, other.jsonSubColumns)
				&& Objects.equals(maxListLength, other.maxListLength) && Objects.equals(maximumSize, other.maximumSize);
	}

	@Override
	public String toString() {
		return "ElementStats [maximumSize=" + maximumSize + ", maxListLength=" + maxListLength + ", defaultValue="
				+ defaultValue + ", facetType=" + facetType + ", enumValues=" + enumValues + ", jsonSubColumns="
				+ jsonSubColumns + "]";
	}

	/**
	 * Create a new ElementStats with a sum of the first and second maximumSizes
	 * All other stats will try to find a non null value, tie breaker in favor of the second ElementStats
	 * 
	 * @param other
	 * @return
	 */
	public static ElementStats generateSumStats(ElementStats one, ElementStats two) {
		return new ElementStats.Builder()
				.setMaximumSize(addLongsWithNull(one.getMaximumSize(), two.getMaximumSize()))
				.setMaxListLength(lastNonNull(one.getMaxListLength(), two.getMaxListLength()))
				.setDefaultValue(lastNonNull(one.getDefaultValue(), two.getDefaultValue()))
				.setFacetType(lastNonNull(one.getFacetType(), two.getFacetType()))
				.setEnumValues(lastNonNull(one.getEnumValues(), two.getEnumValues()))
				.setJsonSubColumns(lastNonNull(one.getJsonSubColumns(), two.getJsonSubColumns()))
				.build();
	}
	
	/**
	 * Create a new ElementStats with a max of the first and second maximumSizes
	 * All other stats will try to find a non null value, tie breaker in favor of the second ElementStats
	 * 
	 * @param other
	 * @return
	 */
	public static ElementStats generateMaxStats(ElementStats one, ElementStats two) {
		return new ElementStats.Builder()
				.setMaximumSize(maxLongsWithNull(one.getMaximumSize(), two.getMaximumSize()))
				.setMaxListLength(lastNonNull(one.getMaxListLength(), two.getMaxListLength()))
				.setDefaultValue(lastNonNull(one.getDefaultValue(), two.getDefaultValue()))
				.setFacetType(lastNonNull(one.getFacetType(), two.getFacetType()))
				.setEnumValues(lastNonNull(one.getEnumValues(), two.getEnumValues()))
				.setJsonSubColumns(lastNonNull(one.getJsonSubColumns(), two.getJsonSubColumns()))
				.build();
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
	
	/**
	 * Max for Longs that can be null.
	 * 
	 * @param currentValue
	 * @param newValue
	 * @return
	 */
	public static Long maxLongsWithNull(Long one, Long two) {
		if(one == null) {
			return two;
		}
		if(two == null) {
			return one;
		}
		return Math.max(one, two);
	}
	
	public static <T> T lastNonNull(T one, T two) {
		return two != null ? two : one;
	}

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private Long maximumSize;
		private Long maxListLength;
		private String defaultValue;
		private FacetType facetType;
		private List<String> enumValues;
		private List<JsonSubColumnModel> jsonSubColumns;
		
		
		public Builder setMaximumSize(Long maximumSize) {
			this.maximumSize = maximumSize;
			return this;
		}
		
		public Builder setMaxListLength(Long maxListLength) {
			this.maxListLength = maxListLength;
			return this;
		}
		
		public Builder setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}
		
		public Builder setFacetType(FacetType facetType) {
			this.facetType = facetType;
			return this;
		}
		
		public Builder setEnumValues(List<String> enumValues) {
			this.enumValues = enumValues;
			return this;
		}
		
		public Builder setJsonSubColumns(List<JsonSubColumnModel> jsonSubColumns) {
			this.jsonSubColumns = jsonSubColumns;
			return this;
		}
		
		public ElementStats build() {
			return new ElementStats(maximumSize, maxListLength, defaultValue, facetType, enumValues, jsonSubColumns);
		}
	}
	
}
