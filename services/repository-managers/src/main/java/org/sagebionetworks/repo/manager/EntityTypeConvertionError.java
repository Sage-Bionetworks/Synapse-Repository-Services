package org.sagebionetworks.repo.manager;
/**
 * All of the types of errors that can occur when an old entity type is converted to a new type.
 * @author John
 *
 */
public enum EntityTypeConvertionError {

	NOT_LOCATIONABLE,
	FILES_CANNOT_HAVE_CHILDREN,
	LOCATIONABLE_HAS_NO_LOCATIONS,
	LOCATIONABLE_HAS_MORE_THAN_ONE_LOCATION;
	
	/**
	 * Throw an IllegalArgumentException for this type.
	 */
	public void throwException(){
		throw new IllegalArgumentException(this.name());
	}
}
