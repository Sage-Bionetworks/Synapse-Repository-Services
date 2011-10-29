package org.sagebionetworks.web.shared;

/**
 * 
 */
public class Reference {
	
	private String targetId;
	private Long targetVersionNumber;

	/**
	 * @return the id of the entity to which this reference refers
	 */
	public String getTargetId() {
		return targetId;
	}

	/**
	 * Set the id of the entity to which this reference refers
	 * @param targetId
	 */
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	/**
	 * @return the version number of the entity to which this reference refers
	 */
	public Long getTargetVersionNumber() {
		return targetVersionNumber;
	}
	
	/**
	 * Set the version number of the entity to which this reference refers
	 * @param targetVersionNumber 
	 */
	public void setTargetVersionNumber(Long targetVersionNumber) {
		this.targetVersionNumber = targetVersionNumber;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((targetId == null) ? 0 : targetId.hashCode());
		result = prime
				* result
				+ ((targetVersionNumber == null) ? 0 : targetVersionNumber
						.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reference other = (Reference) obj;
		if (targetId == null) {
			if (other.targetId != null)
				return false;
		} else if (!targetId.equals(other.targetId))
			return false;
		if (targetVersionNumber == null) {
			if (other.targetVersionNumber != null)
				return false;
		} else if (!targetVersionNumber.equals(other.targetVersionNumber))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Reference [targetId=" + targetId + ", targetVersionNumber="
				+ targetVersionNumber + "]";
	}

	
}
