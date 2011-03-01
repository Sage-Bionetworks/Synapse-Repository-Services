package org.sagebionetworks.repo.model.jdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Base;


/**
 * This interface defines the methods to be implemented by all persistent
 * classes
 * 
 * @author bhoff
 * 
 */
public interface GAEJDOBase {
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
