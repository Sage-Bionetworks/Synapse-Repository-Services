package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * Model object holding the relationship between a user and use agreement, this
 * is the user's "signature" to the terms of the eula.
 */
public class Agreement implements Nodeable {

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
}
