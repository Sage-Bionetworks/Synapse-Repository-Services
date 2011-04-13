package org.sagebionetworks.repo.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This DTO returns a subset of the fields contained in a persisted layer
 * 
 * @author deflaux
 *
 */
@XmlRootElement
public class LayerPreview implements Base {
	
	private String id; // The id of the containing layer
	private String preview;

	/** 
	 * The following members are set by the service layer and should not be persisted.
	 */
	private String uri; // URI for this layer preview
	private String etag; // ETag for this layer preview

	/**
	 * @param preview the preview to set
	 */
	public void setPreview(String preview) {
		this.preview = preview;
	}
	
	/**
	 * @return the preview
	 */
	public String getPreview() {
		return preview;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the uri
	 */
	public String getUri() {
		return uri;
	}
	
	/**
	 * @param uri the uri to set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	/**
	 * @return the etag
	 */
	public String getEtag() {
		return etag;
	}
	
	/**
	 * @param etag the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((preview == null) ? 0 : preview.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerPreview other = (LayerPreview) obj;
		if (preview == null) {
			if (other.preview != null)
				return false;
		} else if (!preview.equals(other.preview))
			return false;
		return true;
	}

}
