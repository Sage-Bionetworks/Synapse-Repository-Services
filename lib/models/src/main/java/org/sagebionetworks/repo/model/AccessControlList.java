package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;

public class AccessControlList implements Base {
	private String id;
	private String resourceId;
	private String createdBy;
	private Date creationDate;
	private String modifiedBy;
	private Date modifiedOn;
	private String etag;
	private String uri;
	
	private Set<ResourceAccess> resourceAccess;

	
	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the createdBy
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy the createdBy to set
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/**
	 * @return the modifiedBy
	 */
	public String getModifiedBy() {
		return modifiedBy;
	}

	/**
	 * @param modifiedBy the modifiedBy to set
	 */
	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	/**
	 * @return the modifiedOn
	 */
	public Date getModifiedOn() {
		return modifiedOn;
	}

	/**
	 * @param modifiedOn the modifiedOn to set
	 */
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	/**
	 * @return the etag
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * @param etag the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/**
	 * @return the resourceId
	 */
	public String getResourceId() {
		return resourceId;
	}

	/**
	 * @param resourceId the resourceId to set
	 */
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * @return the resourceAccess
	 */
	public Set<ResourceAccess> getResourceAccess() {
		return resourceAccess;
	}

	/**
	 * @param resourceAccess the resourceAccess to set
	 */
	public void setResourceAccess(Set<ResourceAccess> resourceAccess) {
		this.resourceAccess = resourceAccess;
	}
	/**
	 * Will create an ACL that will grant all permissions to a given user for the given node.
	 * @param nodeId
	 * @param userId
	 * @return
	 */
	public static AccessControlList createACLToGrantAll(String nodeId, UserInfo info){
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		UserInfo.validateUserInfo(info);
		AccessControlList acl = new AccessControlList();
		acl.setCreatedBy(info.getUser().getUserId());
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setResourceId(nodeId);
		acl.setModifiedBy(acl.getCreatedBy());
		acl.setModifiedOn(acl.getCreationDate());
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		set.add(access);
		// This user should be able to do everything.
		Set<ACCESS_TYPE> typeSet = new HashSet<AuthorizationConstants.ACCESS_TYPE>();
		ACCESS_TYPE array[] = ACCESS_TYPE.values();
		for(ACCESS_TYPE type: array){
			typeSet.add(type);
		}
		access.setAccessType(typeSet);
		access.setUserGroupId(info.getIndividualGroup().getId());
		return acl;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result
				+ ((resourceAccess == null) ? 0 : resourceAccess.hashCode());
		result = prime * result
				+ ((resourceId == null) ? 0 : resourceId.hashCode());
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
		if (!(obj instanceof AccessControlList))
			return false;
		AccessControlList other = (AccessControlList) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (resourceAccess == null) {
			if (other.resourceAccess != null)
				return false;
		} else if (!resourceAccess.equals(other.resourceAccess))
			return false;
		if (resourceId == null) {
			if (other.resourceId != null)
				return false;
		} else if (!resourceId.equals(other.resourceId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AccessControlList [id=" + id + ", resourceId=" + resourceId
				+ ", createdBy=" + createdBy + ", creationDate=" + creationDate
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", etag=" + etag + ", uri=" + uri + ", resourceAccess="
				+ resourceAccess + "]";
	}
	
	

}
