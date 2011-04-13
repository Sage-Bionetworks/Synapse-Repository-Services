package org.sagebionetworks.web.shared;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This is a data transfer object that will be populated from REST JSON.
 * 
 */
public class Layer implements IsSerializable {
	public enum LayerType { G, C, E }
		
	private String annotations;
	private Date creationDate;
	private String description;
	private String etag;
	private String id;
	private List<String> locations;
	private String name;
	private Integer numSamples;	
	private String platform;
	private String preview;
	private String processingFacility;	
	private Date publicationDate;
	private String qcBy;
	private Date qcDate;
	private String releaseNotes;
	private String status;
	private String tissueType;
	private LayerType type;	
	private String uri;
	private String version;

	/**
	 * Default constructor is required
	 */
	public Layer() {

	}


	public String getType() {		
		return type.name();
	}

	public void setType(String typeString) {		
		this.type = LayerType.valueOf(typeString);
	}

	
	/*
	 * Auto generated methods
	 */
	public String getAnnotations() {
		return annotations;
	}


	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}


	public Date getCreationDate() {
		return creationDate;
	}


	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getEtag() {
		return etag;
	}


	public void setEtag(String etag) {
		this.etag = etag;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public List<String> getLocations() {
		return locations;
	}


	public void setLocations(List<String> locations) {
		this.locations = locations;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}

	public Integer getNumSamples() {
		return numSamples;
	}

	public void setNumSamples(Integer numSamples) {
		this.numSamples = numSamples;
	}

	public String getPlatform() {
		return platform;
	}


	public void setPlatform(String platform) {
		this.platform = platform;
	}


	public String getPreview() {
		return preview;
	}


	public void setPreview(String preview) {
		this.preview = preview;
	}


	public String getProcessingFacility() {
		return processingFacility;
	}


	public void setProcessingFacility(String processingFacility) {
		this.processingFacility = processingFacility;
	}


	public Date getPublicationDate() {
		return publicationDate;
	}


	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
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


	public String getReleaseNotes() {
		return releaseNotes;
	}


	public void setReleaseNotes(String releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTissueType() {
		return tissueType;
	}


	public void setTissueType(String tissueType) {
		this.tissueType = tissueType;
	}


	public String getUri() {
		return uri;
	}


	public void setUri(String uri) {
		this.uri = uri;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((locations == null) ? 0 : locations.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((platform == null) ? 0 : platform.hashCode());
		result = prime * result + ((preview == null) ? 0 : preview.hashCode());
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
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Layer other = (Layer) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
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
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (locations == null) {
			if (other.locations != null)
				return false;
		} else if (!locations.equals(other.locations))
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
		if (preview == null) {
			if (other.preview != null)
				return false;
		} else if (!preview.equals(other.preview))
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
		if (type != other.type)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "Layer [annotations=" + annotations + ", creationDate="
				+ creationDate + ", description=" + description + ", etag="
				+ etag + ", id=" + id + ", locations=" + locations + ", name="
				+ name + ", platform=" + platform + ", preview=" + preview
				+ ", processingFacility=" + processingFacility
				+ ", publicationDate=" + publicationDate + ", qcBy=" + qcBy
				+ ", qcDate=" + qcDate + ", releaseNotes=" + releaseNotes
				+ ", tissueType=" + tissueType + ", type=" + type + ", uri="
				+ uri + ", version=" + version + "]";
	}


	
}
