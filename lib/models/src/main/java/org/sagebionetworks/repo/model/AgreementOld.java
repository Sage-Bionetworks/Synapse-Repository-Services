package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * Model object holding the relationship between a user and use agreement, this
 * is the user's "signature" to the terms of the eula.
 */
public class AgreementOld implements NodeableOld {

	private String createdBy;
	private Date creationDate;
	private String etag;
	private String id;
	private String name;
	private String parentId;
	private String uri;
	private String datasetId;
	private Long datasetVersionNumber;
	private String eulaId;
	private Long eulaVersionNumber;

	@TransientField
	private String accessControlList;
	@TransientField
	private String annotations;

	public String getAccessControlList() {
		return accessControlList;
	}

	public void setAccessControlList(String accessControlList) {
		this.accessControlList = accessControlList;
	}

	public String getAnnotations() {
		return annotations;
	}

	public void setAnnotations(String annotations) {
		this.annotations = annotations;
	}

	/**
	 * @return the id of the user who made this agreement 
	 */
	public String getCreatedBy() {
		return createdBy;
	}

	/**
	 * @param createdBy
	 */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * @return the id of the dataset for which the agreement was signed
	 */
	public String getDatasetId() {
		return datasetId;
	}

	/**
	 * @param datasetId
	 */
	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	/**
	 * @return the version number of the dataset at the time this agreement was signed
	 */
	public Long getDatasetVersionNumber() {
		return datasetVersionNumber;
	}

	/**
	 * @param datasetVersionNumber
	 */
	public void setDatasetVersionNumber(Long datasetVersionNumber) {
		this.datasetVersionNumber = datasetVersionNumber;
	}

	/**
	 * @return the it of the eula for which the agreement was signed
	 */
	public String getEulaId() {
		return eulaId;
	}

	/**
	 * @param eulaId
	 */
	public void setEulaId(String eulaId) {
		this.eulaId = eulaId;
	}

	/**
	 * @return the version number of the eula at the time this agreement was signed
	 */
	public Long getEulaVersionNumber() {
		return eulaVersionNumber;
	}

	/**
	 * @param eulaVersionNumber
	 */
	public void setEulaVersionNumber(Long eulaVersionNumber) {
		this.eulaVersionNumber = eulaVersionNumber;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessControlList == null) ? 0 : accessControlList
						.hashCode());
		result = prime * result
				+ ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result
				+ ((datasetId == null) ? 0 : datasetId.hashCode());
		result = prime
				* result
				+ ((datasetVersionNumber == null) ? 0 : datasetVersionNumber
						.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((eulaId == null) ? 0 : eulaId.hashCode());
		result = prime
				* result
				+ ((eulaVersionNumber == null) ? 0 : eulaVersionNumber
						.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		AgreementOld other = (AgreementOld) obj;
		if (accessControlList == null) {
			if (other.accessControlList != null)
				return false;
		} else if (!accessControlList.equals(other.accessControlList))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
			return false;
		if (datasetId == null) {
			if (other.datasetId != null)
				return false;
		} else if (!datasetId.equals(other.datasetId))
			return false;
		if (datasetVersionNumber == null) {
			if (other.datasetVersionNumber != null)
				return false;
		} else if (!datasetVersionNumber.equals(other.datasetVersionNumber))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (eulaId == null) {
			if (other.eulaId != null)
				return false;
		} else if (!eulaId.equals(other.eulaId))
			return false;
		if (eulaVersionNumber == null) {
			if (other.eulaVersionNumber != null)
				return false;
		} else if (!eulaVersionNumber.equals(other.eulaVersionNumber))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Agreement [accessControlList=" + accessControlList
				+ ", annotations=" + annotations + ", createdBy=" + createdBy
				+ ", creationDate=" + creationDate + ", datasetId=" + datasetId
				+ ", datasetVersionNumber=" + datasetVersionNumber + ", etag="
				+ etag + ", eulaId=" + eulaId + ", eulaVersionNumber="
				+ eulaVersionNumber + ", id=" + id + ", name=" + name
				+ ", parentId=" + parentId + ", uri=" + uri + "]";
	}
	
	
}
