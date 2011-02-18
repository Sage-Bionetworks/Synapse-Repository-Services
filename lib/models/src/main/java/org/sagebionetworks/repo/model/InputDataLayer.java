package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InputDataLayer implements DatasetLayer {
	private String id;
	private String name;
	private String description;
	private Date creationDate;
	private String version;
	private Date publicationDate;
	private String releaseNotes;
	private String type;
	private String tissueType;
	private String platform;
	private String processingFacility;
	private String qcBy;
	private Date qcDate;

	/** 
	 * The following members are set by the service layer and should not be persisted.
	 */
	private String uri; // URI for this layer
	private String etag; // ETag for this layer
	private String annotations; // URI for annotations
	private String preview; // URI for preview
	private Collection<String> locations; // URIs for locations

	/**
	 * Allowable layer type names
	 * 
	 * TODO do we want to encode allowable values here?
	 */
	public enum LayerTypeNames {
		E, G, C;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getEtag() {
		return etag;
	}

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

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

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

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	public void setReleaseNotes(String releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) throws InvalidModelException {
		
        try {
        	LayerTypeNames.valueOf(type);
        } catch( IllegalArgumentException e ) {
        	StringBuilder helpfulErrorMessage = new StringBuilder("'type' must be one of:");
        	for(LayerTypeNames name : LayerTypeNames.values()) {
        		helpfulErrorMessage.append(" ").append(name);
        	}
        	throw new InvalidModelException(helpfulErrorMessage.toString());
        }

		this.type = type;
	}

	public String getTissueType() {
		return tissueType;
	}

	public void setTissueType(String tissueType) {
		this.tissueType = tissueType;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getProcessingFacility() {
		return processingFacility;
	}

	public void setProcessingFacility(String processingFacility) {
		this.processingFacility = processingFacility;
	}

	public String getQcBy() {
		return qcBy;
	}

	public void setQcBy(String qcBy) {
		this.qcBy = qcBy;
	}

	public Date getQcDate() {
		return qcDate;
	}

	public void setQcDate(Date qcDate) {
		this.qcDate = qcDate;
	}

	/**
	 * @return the location uris
	 */
	public Collection<String> getLocations() {
		return locations;
	}

	/**
	 * @param locations the uri locations to set
	 */
	public void setLocations(Collection<String> locations) {
		this.locations = locations;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((platform == null) ? 0 : platform.hashCode());
		result = prime
				* result
				+ ((processingFacility == null) ? 0 : processingFacility
						.hashCode());
		result = prime * result
				+ ((publicationDate == null) ? 0 : publicationDate.hashCode());
		result = prime * result + ((qcBy == null) ? 0 : qcBy.hashCode());
		result = prime * result + ((qcDate == null) ? 0 : qcDate.hashCode());
		result = prime * result
				+ ((releaseNotes == null) ? 0 : releaseNotes.hashCode());
		result = prime * result
				+ ((tissueType == null) ? 0 : tissueType.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		InputDataLayer other = (InputDataLayer) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (platform == null) {
			if (other.platform != null)
				return false;
		} else if (!platform.equals(other.platform))
			return false;
		if (processingFacility == null) {
			if (other.processingFacility != null)
				return false;
		} else if (!processingFacility.equals(other.processingFacility))
			return false;
		if (publicationDate == null) {
			if (other.publicationDate != null)
				return false;
		} else if (!publicationDate.equals(other.publicationDate))
			return false;
		if (qcBy == null) {
			if (other.qcBy != null)
				return false;
		} else if (!qcBy.equals(other.qcBy))
			return false;
		if (qcDate == null) {
			if (other.qcDate != null)
				return false;
		} else if (!qcDate.equals(other.qcDate))
			return false;
		if (releaseNotes == null) {
			if (other.releaseNotes != null)
				return false;
		} else if (!releaseNotes.equals(other.releaseNotes))
			return false;
		if (tissueType == null) {
			if (other.tissueType != null)
				return false;
		} else if (!tissueType.equals(other.tissueType))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}


}
