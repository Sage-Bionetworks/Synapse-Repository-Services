package org.sagebionetworks.repo.model.gaejdo;

import java.util.Date;

import org.sagebionetworks.repo.model.Base;
import com.google.appengine.api.datastore.Key;

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
	public void setId(Key id);

	/**
	 * 
	 * @return id of the persistent object
	 */
	public Key getId();

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
