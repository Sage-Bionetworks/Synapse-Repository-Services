package org.sagebionetworks.repo.model.file;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.EntityType;

/**
 * Request for statistics for a given parent object.
 *
 */
public class ChildStatsRequest {

	private String parentId;
	private List<EntityType> includeTypes;
	private Set<Long> childIdsToExclude;
	private Boolean includeTotalChildCount;
	private Boolean includeSumFileSizes;

	/**
	 * The statistics for this parentId.
	 * 
	 * @return
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * The statistics for this parentId.
	 * 
	 * @param parentId
	 * @return
	 */
	public ChildStatsRequest withParentId(String parentId) {
		this.parentId = parentId;
		return this;
	}

	/**
	 * Filter the child types to be included.
	 * 
	 * @return
	 */
	public List<EntityType> getIncludeTypes() {
		return includeTypes;
	}

	/**
	 * Filter the child types to be included.
	 * 
	 * @param includeTypes
	 * @return
	 */
	public ChildStatsRequest withIncludeTypes(List<EntityType> includeTypes) {
		this.includeTypes = includeTypes;
		return this;
	}

	/**
	 * Children ID that should be excluded from the results.
	 * 
	 * @return
	 */
	public Set<Long> getChildIdsToExclude() {
		return childIdsToExclude;
	}

	/**
	 * Children ID that should be excluded from the results.
	 * 
	 * @param childIdsToExclude
	 * @return
	 */
	public ChildStatsRequest withChildIdsToExclude(Set<Long> childIdsToExclude) {
		this.childIdsToExclude = childIdsToExclude;
		return this;
	}

	/**
	 * When true the total child count is included. Defaults to false.
	 * 
	 * @return
	 */
	public Boolean getIncludeTotalChildCount() {
		return includeTotalChildCount;
	}

	/**
	 * When true the total child count is included. Defaults to false.
	 * 
	 * @param includeTotalChildCount
	 * @return
	 */
	public ChildStatsRequest withIncludeTotalChildCount(Boolean includeTotalChildCount) {
		this.includeTotalChildCount = includeTotalChildCount;
		return this;
	}

	/**
	 * When true the sum of the file sizes is included. Defaults to false.
	 * 
	 * @return
	 */
	public Boolean getIncludeSumFileSizes() {
		return includeSumFileSizes;
	}

	/**
	 * When true the sum of the file sizes is included. Defaults to false.
	 * 
	 * @param includeSumFileSizes
	 * @return
	 */
	public ChildStatsRequest withIncludeSumFileSizes(Boolean includeSumFileSizes) {
		this.includeSumFileSizes = includeSumFileSizes;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((childIdsToExclude == null) ? 0 : childIdsToExclude.hashCode());
		result = prime * result + ((includeSumFileSizes == null) ? 0 : includeSumFileSizes.hashCode());
		result = prime * result + ((includeTotalChildCount == null) ? 0 : includeTotalChildCount.hashCode());
		result = prime * result + ((includeTypes == null) ? 0 : includeTypes.hashCode());
		result = prime * result + ((parentId == null) ? 0 : parentId.hashCode());
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
		ChildStatsRequest other = (ChildStatsRequest) obj;
		if (childIdsToExclude == null) {
			if (other.childIdsToExclude != null)
				return false;
		} else if (!childIdsToExclude.equals(other.childIdsToExclude))
			return false;
		if (includeSumFileSizes == null) {
			if (other.includeSumFileSizes != null)
				return false;
		} else if (!includeSumFileSizes.equals(other.includeSumFileSizes))
			return false;
		if (includeTotalChildCount == null) {
			if (other.includeTotalChildCount != null)
				return false;
		} else if (!includeTotalChildCount.equals(other.includeTotalChildCount))
			return false;
		if (includeTypes == null) {
			if (other.includeTypes != null)
				return false;
		} else if (!includeTypes.equals(other.includeTypes))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ParentStatsRequest [parentId=" + parentId + ", includeTypes=" + includeTypes + ", childIdsToExclude="
				+ childIdsToExclude + ", includeTotalChildCount=" + includeTotalChildCount + ", includeSumFileSizes="
				+ includeSumFileSizes + "]";
	}

}
