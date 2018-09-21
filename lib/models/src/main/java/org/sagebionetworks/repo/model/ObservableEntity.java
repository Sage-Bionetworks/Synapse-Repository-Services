package org.sagebionetworks.repo.model;

/**
 * Any entity that can be observed from the outside world.  All migratable objects should be observable.
 * @author jmhill
 *
 */
public interface ObservableEntity {
	
	/**
	 * The id of the object.
	 */
	public String getIdString();
	
	/**
	 * Get the etag of this object
	 */
	public String getEtag();
	
	/**
	 * The object's type.
	 */
	public ObjectType getObjectType();

}
