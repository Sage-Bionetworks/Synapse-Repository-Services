package org.sagebionetworks.repo.model.file;

/**
 * A DTO representing an error of a single part of a multi-part upload.
 *
 */
public class PartErrors {

	int partNumber;
	String errorDetails;
	public int getPartNumber() {
		return partNumber;
	}
	public void setPartNumber(int partNumber) {
		this.partNumber = partNumber;
	}
	public String getErrorDetails() {
		return errorDetails;
	}
	public void setErrorDetails(String errorDetails) {
		this.errorDetails = errorDetails;
	}
	
	public PartErrors(int partNumber, String errorDetails) {
		super();
		this.partNumber = partNumber;
		this.errorDetails = errorDetails;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((errorDetails == null) ? 0 : errorDetails.hashCode());
		result = prime * result + partNumber;
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
		PartErrors other = (PartErrors) obj;
		if (errorDetails == null) {
			if (other.errorDetails != null)
				return false;
		} else if (!errorDetails.equals(other.errorDetails))
			return false;
		if (partNumber != other.partNumber)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "PartErrors [partNumber=" + partNumber + ", errorDetails="
				+ errorDetails + "]";
	}
	
}
