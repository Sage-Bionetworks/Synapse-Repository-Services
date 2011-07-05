package org.sagebionetworks.repo.model;

/**
 * Versionable Entities should implement this interface.
 * 
 * @author jmhill
 *
 */
public interface Versionable extends Nodeable {

	/**
	 * The version label is the user visible string for a given version.
	 * @return
	 */
	public String getVersionLabel();
	
	/**
	 * The version label is the user visible string for a given version.
	 * @param label
	 */
	public void setVersionLabel(String label);
	
	/**
	 * When a version is created this is the comment that the user enters to describe it.
	 * @return
	 */
	public String getVersionComment();
	
	/**
	 * When a version is created this is the comment that the user enters to describe it.
	 * @param comment
	 */
	public void setVersionComment(String comment);
	
	/**
	 * When a version is created it is automatically, assigned a version number.
	 * This number is used to reference a particular version of an entity.
	 * @return
	 */
	public Long getVersionNumber();
	
	/**
	 * When a version is created it is automatically, assigned a version number.
	 * This number is used to reference a particular version of an entity.
	 * @param number
	 */
	public void setVersionNumber(Long number);
	
	/**
	 * Use this URL to reference a specific version of aa entity.
	 * @param versionUrl
	 */
	public void setVersionUrl(String versionUrl);
	
	/**
	 * Use this URL to reference a specific version of an entity.
	 * @return
	 */
	public String getVersionUrl();
	
	/**
	 * This URL will list all version of this entity.
	 * @return
	 */
	public String getVersions();
	
	/**
	 * This URL will list all version of this entity.
	 */
	public void setVersions(String versions);
}
