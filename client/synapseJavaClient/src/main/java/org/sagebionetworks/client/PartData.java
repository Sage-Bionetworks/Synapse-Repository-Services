package org.sagebionetworks.client;

import java.io.File;

/**
 * Data about a single part of a multi-part upload.s
 *
 */
public class PartData {
	
	File partFile;
	int partNumber;
	String partMD5Hex;
	
	public File getPartFile() {
		return partFile;
	}
	public void setPartFile(File partFile) {
		this.partFile = partFile;
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
				+ ((partFile == null) ? 0 : partFile.hashCode());
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
		PartData other = (PartData) obj;
		if (partFile == null) {
			if (other.partFile != null)
				return false;
		} else if (!partFile.equals(other.partFile))
			return false;
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
		return "PartData [partFile=" + partFile + ", partNumber=" + partNumber
				+ ", partMD5Hex=" + partMD5Hex + "]";
	}
	
}
