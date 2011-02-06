package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is the data transfer object for Datasets.
 * 
 * 
 * @author bhoff
 * 
 */
@XmlRootElement
public class Dataset implements Base, Revisable {
	private String id;
	private String uri;
	private String etag;
	private String name;
	private String description;
	private String creator;
	private Date creationDate;
	private String status;
	private Date releaseDate;
	private String version;
	private String annotations; // URI for annotations
	private String layer;
	private Collection<LayerPreview> layerPreviews; // (id, type, URI) triples
													// for

	// layer previews

	public String getId() {
		return id;
	}

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
	 * @param uri
	 *            the uri to set
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
	 * @param etag
	 *            the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the annotations URI
	 */
	public String getAnnotations() {
		return annotations;
	}

	/**
	 * @param annotations
	 *            the URI that can be used to retrieve our annotations
	 */
	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	/**
	 * @return the layer URI
	 */
	public String getLayer() {
		return layer;
	}

	/**
	 * @param layer
	 *            the URI that can be used to retrieve our layer
	 */
	public void setLayer(String layer) {
		this.layer = layer;
	}

	/**
	 * @return the layer previews
	 */
	public Collection<LayerPreview> getLayerPreviews() {
		return layerPreviews;
	}

	/**
	 * @param layerPreviews
	 *            the (it, type, layer URI) pairs to set
	 */
	public void setLayerPreviews(Collection<LayerPreview> layerPreviews) {
		this.layerPreviews = layerPreviews;
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
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((layerPreviews == null) ? 0 : layerPreviews.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((releaseDate == null) ? 0 : releaseDate.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		Dataset other = (Dataset) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (layerPreviews == null) {
			if (other.layerPreviews != null)
				return false;
		} else if (!layerPreviews.equals(other.layerPreviews))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (releaseDate == null) {
			if (other.releaseDate != null)
				return false;
		} else if (!releaseDate.equals(other.releaseDate))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

}
