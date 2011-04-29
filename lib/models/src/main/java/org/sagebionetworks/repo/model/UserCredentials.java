package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * This class holds credentials we store on behalf of users to facilitate access
 * to resources.
 * <p>
 * 
 * Currently only AWS specific credentials are held in this class but one can
 * imagine that down the road we might also hold what ever is needed for Azure,
 * Google, etc... if we need to store per-user info for those other clouds.
 * 
 * @author deflaux
 * 
 */
public class UserCredentials implements Base {

	private String id; // The id of the containing user
	private String iamAccessId;
	private String iamSecretKey;

	private Date creationDate;
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the iamAccessId
	 */
	public String getIamAccessId() {
		return iamAccessId;
	}

	/**
	 * @param iamAccessId
	 *            the iamAccessId to set
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
	 * @param iamSecretKey
	 *            the iamSecretKey to set
	 */
	public void setIamSecretKey(String iamSecretKey) {
		this.iamSecretKey = iamSecretKey;
	}

	@Override
	public String getEtag() {
		// Not applicable since we currently do not plan to expose these
		// credentials outside of the service
		return null;
	}

	@Override
	public String getUri() {
		// Not applicable since we currently do not plan to expose these
		// credentials outside of the service
		return null;
	}

	@Override
	public void setEtag(String etag) {
		// Not applicable since we currently do not plan to expose these
		// credentials outside of the service

	}

	@Override
	public void setUri(String uri) {
		// Not applicable since we currently do not plan to expose these
		// credentials outside of the service

	}

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

}
