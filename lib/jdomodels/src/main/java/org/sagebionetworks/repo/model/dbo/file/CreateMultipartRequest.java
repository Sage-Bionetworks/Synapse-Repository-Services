package org.sagebionetworks.repo.model.dbo.file;

/**
 * DTO to create a new multi-part request.
 *
 */
public class CreateMultipartRequest {

	Long userId;
	String hash;
	String requestString;
	Long storageLocationId;
	String storageLocationToken;
	
	/**
	 * 
	 * @param userId The Id of the user starting the upload.
	 * @param hash The hash the uniquely identifies this upload.
	 * @param requestString Data about the request to stored.
	 * @param storageLocationId The location where the file is to be upload.
	 * @param storageLocationToken  The identifier for this upload from the storate location provider.
	 */
	public CreateMultipartRequest(Long userId, String hash,
			String requestString, Long storageLocationId,
			String storageLocationToken) {
		super();
		this.userId = userId;
		this.hash = hash;
		this.requestString = requestString;
		this.storageLocationId = storageLocationId;
		this.storageLocationToken = storageLocationToken;
	}
	
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getRequestString() {
		return requestString;
	}
	public void setRequestString(String requestString) {
		this.requestString = requestString;
	}
	public Long getStorageLocationId() {
		return storageLocationId;
	}
	public void setStorageLocationId(Long storageLocationId) {
		this.storageLocationId = storageLocationId;
	}
	public String getStorageLocationToken() {
		return storageLocationToken;
	}
	public void setStorageLocationToken(String storageLocationToken) {
		this.storageLocationToken = storageLocationToken;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hash == null) ? 0 : hash.hashCode());
		result = prime * result
				+ ((requestString == null) ? 0 : requestString.hashCode());
		result = prime
				* result
				+ ((storageLocationId == null) ? 0 : storageLocationId
						.hashCode());
		result = prime
				* result
				+ ((storageLocationToken == null) ? 0 : storageLocationToken
						.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		CreateMultipartRequest other = (CreateMultipartRequest) obj;
		if (hash == null) {
			if (other.hash != null)
				return false;
		} else if (!hash.equals(other.hash))
			return false;
		if (requestString == null) {
			if (other.requestString != null)
				return false;
		} else if (!requestString.equals(other.requestString))
			return false;
		if (storageLocationId == null) {
			if (other.storageLocationId != null)
				return false;
		} else if (!storageLocationId.equals(other.storageLocationId))
			return false;
		if (storageLocationToken == null) {
			if (other.storageLocationToken != null)
				return false;
		} else if (!storageLocationToken.equals(other.storageLocationToken))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CreateMultipartRequest [userId=" + userId + ", hash=" + hash
				+ ", requestString=" + requestString + ", storageLocationId="
				+ storageLocationId + ", storageLocationToken="
				+ storageLocationToken + "]";
	}

	
}
