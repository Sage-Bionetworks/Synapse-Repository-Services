package org.sagebionetworks.repo.model.jdo;

import java.util.Date;


/**
 * This interface defines the methods to be implemented by all persistent
 * classes
 * 
 * @author bhoff
 * 
 */
public interface JDOBase {
	/**
	 * 
	 * @param id
	 *            id of the persistent object
	 */
	public void setId(Long id);

	/**
	 * 
	 * @return id of the persistent object
	 */
	public Long getId();
	
	/**
	 * set the etag, indicating the the peristant object has changed
	 */
	public void setEtag(Long etag);
	
	/**
	 * get the etag
	 */
	public Long getEtag();

	/**
	 * 
	 * @param d
	 *            date the persistent object was created
	 */
	public void setCreationDate(Date d);

	/**
	 * 
	 * @return date the persistent object was created
	 */
	public Date getCreationDate();
}
