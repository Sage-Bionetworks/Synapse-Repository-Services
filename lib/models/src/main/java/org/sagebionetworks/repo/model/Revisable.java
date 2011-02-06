package org.sagebionetworks.repo.model;

/**
 * This interface defines the methods for all data transfer objects which are
 * Revisable.
 * 
 * @author bhoff
 * 
 */
public interface Revisable extends Base {
	/**
	 * 
	 * @param version
	 */
	public void setVersion(String version);

	/**
	 * 
	 * @return the current version
	 */
	public String getVersion();
}
