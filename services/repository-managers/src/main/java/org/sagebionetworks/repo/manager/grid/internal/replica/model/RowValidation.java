package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class RowValidation {

	/**
	 * The ID of the object that contains both 'isValid' and 'errorMessage'.
	 */
	private LogicalTimestamp objectId;
	private Boolean isValid;
	private String errorMessage;

	public Boolean getIsValid() {
		return isValid;
	}

	public RowValidation setIsValid(Boolean isValid) {
		this.isValid = isValid;
		return this;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public RowValidation setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		return this;
	}

	public LogicalTimestamp getObjectId() {
		return objectId;
	}

	public RowValidation setObjectId(LogicalTimestamp objectId) {
		this.objectId = objectId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errorMessage, isValid, objectId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowValidation other = (RowValidation) obj;
		return Objects.equals(errorMessage, other.errorMessage) && Objects.equals(isValid, other.isValid)
				&& Objects.equals(objectId, other.objectId);
	}

	@Override
	public String toString() {
		return "RowValidation [objectId=" + objectId + ", isValid=" + isValid + ", errorMessage=" + errorMessage + "]";
	}

}
