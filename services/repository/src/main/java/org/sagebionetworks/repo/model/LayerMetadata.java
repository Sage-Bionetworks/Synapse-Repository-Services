/**
 * 
 */
package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * LayerMetadata holds the few shallow DataLayer properties that should be
 * returned with a Dataset DTO
 * 
 * @author deflaux
 * 
 */
public class LayerMetadata implements Base {

	private String id;
	private String type;
	private String uri;
	private String etag;
	private Date creationDate;

	/**
	 * 
	 */
	public LayerMetadata() {
	}

	/**
	 * @param id
	 */
	public LayerMetadata(String id) {
		this.id = id;
	}

	/**
	 * @param id
	 * @param type
	 * @param uri
	 */
	public LayerMetadata(String id, String type, String uri) {
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

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

}
