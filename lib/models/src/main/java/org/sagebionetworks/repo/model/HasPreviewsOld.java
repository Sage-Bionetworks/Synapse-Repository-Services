package org.sagebionetworks.repo.model;

/**
 * Any object that has previews should implement this method.
 * @author jmhill
 *
 */
public interface HasPreviewsOld extends NodeableOld {
	
	/**
	 * The URL to get all of the previews for this object.
	 * @param previews
	 */
	public void setPreviews(String previews);
	
	/**
	 * The URL to get all of the previews for this object.
	 * @return
	 */
	public String getPreviews();

}
