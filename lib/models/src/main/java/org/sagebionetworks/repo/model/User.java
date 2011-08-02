package org.sagebionetworks.repo.model;

import java.util.Date;

public class User implements Base {
	private String id; // system generated Key for the object
	private String userId; // log-in ID, perhaps chosen by the user
	private String uri;
	private String etag;
	private Date creationDate;
	private String iamUserId;
	private String iamAccessId;
	private String iamSecretKey;
	
//	public String getType() {return User.class.getName();}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUserId() {
		return userId;
	}
	
	public String toString() {return getUserId();}
	
	public void setUserId(String usedId) {
		this.userId = usedId;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String getEtag() {
		return etag;
	}
	public void setEtag(String etag) {
		this.etag = etag;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	/**
	 * @return the iamUserId
	 */
	public String getIamUserId() {
		return iamUserId;
	}
	/**
	 * @param iamUserId the iamUserId to set
	 */
	public void setIamUserId(String iamUserId) {
		this.iamUserId = iamUserId;
	}
	/**
	 * @return the iamAccessId
	 */
	public String getIamAccessId() {
		return iamAccessId;
	}
	/**
	 * @param iamAccessId the iamAccessId to set
	 */
	public void setIamAccessId(String iamAccessId) {
		this.iamAccessId = iamAccessId;
	}
	/**
	 * @return the iamSecretKey
	 */
	public String getIamSecretKey() {
		return iamSecretKey;
	}
	/**
	 * @param iamSecretKey the iamSecretKey to set
	 */
	public void setIamSecretKey(String iamSecretKey) {
		this.iamSecretKey = iamSecretKey;
	}
	
	/**
	 * Is this a valid user?
	 * @param user
	 */
	public static void validateUser(User user){
		if(user == null) throw new IllegalArgumentException("User cannot be null");
		if(user.getUserId() == null) throw new IllegalArgumentException("User.userId cannot be null");
//		if(user.getId() == null) throw new IllegalArgumentException("User.id cannot be null");
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result
				+ ((iamAccessId == null) ? 0 : iamAccessId.hashCode());
		result = prime * result
				+ ((iamSecretKey == null) ? 0 : iamSecretKey.hashCode());
		result = prime * result
				+ ((iamUserId == null) ? 0 : iamUserId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		User other = (User) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (iamAccessId == null) {
			if (other.iamAccessId != null)
				return false;
		} else if (!iamAccessId.equals(other.iamAccessId))
			return false;
		if (iamSecretKey == null) {
			if (other.iamSecretKey != null)
				return false;
		} else if (!iamSecretKey.equals(other.iamSecretKey))
			return false;
		if (iamUserId == null) {
			if (other.iamUserId != null)
				return false;
		} else if (!iamUserId.equals(other.iamUserId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
}
