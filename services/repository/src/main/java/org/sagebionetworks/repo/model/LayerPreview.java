/**
 * 
 */
package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * LayerPreview holds the few shallow DataLayer properties that should be
 * returned with a Dataset DTO
 * <p>
 * 
 * This is a "preview" type of object. When looking at the parent object (a
 * Dataset) you can see that it has layers with URIs so that users know how to
 * request them but it also includes the type field from a layer as a teaser - a
 * preview of what is to come if you ask for the full-on layer object
 * 
 * @author deflaux
 * 
 */
public class LayerPreview {

	private String id;
	private String type;
	private String uri;

	/**
	 * 
	 */
	public LayerPreview() {
	}

	/**
	 * @param id
	 */
	public LayerPreview(String id) {
		this.id = id;
	}

	/**
	 * @param id
	 * @param type
	 * @param uri
	 */
	public LayerPreview(String id, String type, String uri) {
		this.id = id;
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
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
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
