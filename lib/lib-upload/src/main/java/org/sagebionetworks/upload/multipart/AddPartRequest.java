package org.sagebionetworks.upload.multipart;

/**
 * Parameters for adding a part to a multi-part upload.
 * 
 */
public class AddPartRequest {
	
	String uploadToken;
	String bucket;
	String key;
	String partKey;
	String partMD5Hex;
	int partNumber;

	/**
	 * 
	 * @param uploadToken
	 *            The AWS S3 upload ID.
	 * @param bucket
	 *            The source and destination bucket
	 * @param key
	 *            The destination key.
	 * @param partKey
	 *            The part key.
	 * @param partMD5Hex
	 *            The part MD5 as a hex string.
	 */
	public AddPartRequest(String uploadToken, String bucket, String key,
			String partKey, String partMD5Hex, int partNumber) {
		super();
		this.uploadToken = uploadToken;
		this.bucket = bucket;
		this.key = key;
		this.partKey = partKey;
		this.partMD5Hex = partMD5Hex;
		this.partNumber = partNumber;
	}

	public String getUploadToken() {
		return uploadToken;
	}

	public int getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(int partNumber) {
		this.partNumber = partNumber;
	}

	public void setUploadToken(String uploadToken) {
		this.uploadToken = uploadToken;
	}

	public String getBucket() {
		return bucket;
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getPartKey() {
		return partKey;
	}

	public void setPartKey(String partKey) {
		this.partKey = partKey;
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
		result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((partKey == null) ? 0 : partKey.hashCode());
		result = prime * result
				+ ((partMD5Hex == null) ? 0 : partMD5Hex.hashCode());
		result = prime * result + partNumber;
		result = prime * result
				+ ((uploadToken == null) ? 0 : uploadToken.hashCode());
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
		AddPartRequest other = (AddPartRequest) obj;
		if (bucket == null) {
			if (other.bucket != null)
				return false;
		} else if (!bucket.equals(other.bucket))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (partKey == null) {
			if (other.partKey != null)
				return false;
		} else if (!partKey.equals(other.partKey))
			return false;
		if (partMD5Hex == null) {
			if (other.partMD5Hex != null)
				return false;
		} else if (!partMD5Hex.equals(other.partMD5Hex))
			return false;
		if (partNumber != other.partNumber)
			return false;
		if (uploadToken == null) {
			if (other.uploadToken != null)
				return false;
		} else if (!uploadToken.equals(other.uploadToken))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AddPartRequest [uploadToken=" + uploadToken + ", bucket="
				+ bucket + ", key=" + key + ", partKey=" + partKey
				+ ", partMD5Hex=" + partMD5Hex + ", partNumber=" + partNumber
				+ "]";
	}

}
