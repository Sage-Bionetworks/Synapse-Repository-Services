package org.sagebionetworks.repo.model;

import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This DTO returns a subset of the fields contained in a persisted layer
 * 
 * @author deflaux
 * 
 */
@XmlRootElement
public class LayerLocations implements Base {

	private Collection<LayerLocation> locations;
	/**
	 * The following members are set by the service layer and should not be
	 * persisted.
	 */
	private String id; // The id of the containing layer
	private String uri; // URI for this layer preview
	private String etag; // ETag for this layer preview

	/**
	 * @return the locations
	 */
	public Collection<LayerLocation> getLocations() {
		return locations;
	}

	/**
	 * @param locations
	 *            the locations to set
	 */
	public void setLocations(Collection<LayerLocation> locations) {
		this.locations = locations;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public void setUri(String uri) {
		this.uri = uri;
	}

	@Override
	public String getEtag() {
		return etag;
	}

	@Override
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((locations == null) ? 0 : locations.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
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
		LayerLocations other = (LayerLocations) obj;
		if (locations == null) {
			if (other.locations != null)
				return false;
		} else if (!locations.equals(other.locations))
			return false;
		return true;
	}

}
