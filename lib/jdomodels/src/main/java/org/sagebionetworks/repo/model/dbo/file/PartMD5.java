package org.sagebionetworks.repo.model.dbo.file;

public class PartMD5 {

	int partNumber;
	String partMD5Hex;

	public PartMD5(int partNumber, String partMD5Hex) {
		super();
		this.partNumber = partNumber;
		this.partMD5Hex = partMD5Hex;
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		this.partNumber = partNumber;
	}

	public String getPartMD5Hex() {
		return partMD5Hex;
	}

	public void setPartMD5Hex(String partMD5Hex) {
		this.partMD5Hex = partMD5Hex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((partMD5Hex == null) ? 0 : partMD5Hex.hashCode());
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
		PartMD5 other = (PartMD5) obj;
		if (partMD5Hex == null) {
			if (other.partMD5Hex != null)
				return false;
		} else if (!partMD5Hex.equals(other.partMD5Hex))
			return false;
		if (partNumber != other.partNumber)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PartMD5 [partNumber=" + partNumber + ", partMD5Hex="
				+ partMD5Hex + "]";
	}
}
