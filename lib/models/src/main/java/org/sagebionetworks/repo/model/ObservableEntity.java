package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * Any entity that can be observed from the outside world.  All migratable objects should be observable.
 * @author jmhill
 *
 */
public interface ObservableEntity  extends TaggableEntity {
	
	/**
	 * The id of the object.
	 * @return
	 */
	public String getIdString();
	
	/**
	 * The etag of this object.
	 * @param newEtag
	 */
	public void seteTag(String newEtag);
	
	/**
	 * Get the etag of this object
	 * @return
	 */
	public String geteTag();
	
	/**
	 * The object's type.
	 * @return
	 */
	public ObjectType getObjectType();

}
