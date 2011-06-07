package org.sagebionetworks.repo.model;

/**
 * Any object that has locations should implement this interface.
 * 
 * @author jmhill
 *
 */
public interface HasLocations extends Nodeable{
	
	/**
	 * The URL for the locations of this object.
	 */
	public void setLocations(String locations);
	
	/**
	 * The URL for the locations of this object.
	 */
	public String getLocations();

}
