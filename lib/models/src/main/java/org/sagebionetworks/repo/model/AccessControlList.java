package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;

public class AccessControlList implements Base {
	private String id;
	private Date creationDate;
	private String etag;
	private String uri;
	
	// the following three fields are no longer used
	// but must be included to allow deserialization of
	// legacy archives by Xstream (http://xstream.codehaus.org/)

	private String createdBy;
	private String modifiedBy;
	private Date modifiedOn;
	
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
		acl.setCreationDate(new Date(System.currentTimeMillis()));
		acl.setId(nodeId);
		Set<ResourceAccess> set = new HashSet<ResourceAccess>();
		acl.setResourceAccess(set);
		ResourceAccess access = new ResourceAccess();
		// This user should be able to do everything.
		Set<ACCESS_TYPE> typeSet = new HashSet<AuthorizationConstants.ACCESS_TYPE>();
		ACCESS_TYPE array[] = ACCESS_TYPE.values();
		for(ACCESS_TYPE type: array){
			typeSet.add(type);
		}
		access.setAccessType(typeSet);
		access.setGroupName(info.getIndividualGroup().getName());
		access.setDisplayName(info.getUser().getDisplayName());
		access.setPrincipalId(Long.parseLong(info.getIndividualGroup().getId()));
		set.add(access);
		return acl;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((resourceAccess == null) ? 0 : resourceAccess.hashCode());
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
		AccessControlList other = (AccessControlList) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (resourceAccess == null) {
			if (other.resourceAccess != null)
				return false;
		} else if (!resourceAccess.equals(other.resourceAccess))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AccessControlList [id=" + id 
				+ ", creationDate=" + creationDate  +  ", etag=" + etag
				+ ", uri=" + uri + ", resourceAccess=" + resourceAccess + "]";
	}

}
