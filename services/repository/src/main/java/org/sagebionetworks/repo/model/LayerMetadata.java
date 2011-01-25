/**
 * 
 */
package org.sagebionetworks.repo.model;

/**
 * TODO wrote this in a hurry, think about how we might do this more cleanly
 * 
 * @author deflaux
 * 
 */
public class LayerMetadata {

	private String type;
	private String uri;

	/**
	 * 
	 */
	public LayerMetadata() {
	}

	/**
	 * @param type
	 * @param uri
	 */
	public LayerMetadata(String type, String uri) {
		this.type = type;
		this.uri = uri;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

}
