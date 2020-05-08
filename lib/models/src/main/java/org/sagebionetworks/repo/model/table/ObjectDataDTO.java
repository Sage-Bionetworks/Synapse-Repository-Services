package org.sagebionetworks.repo.model.table;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Data Transfer Object (TDO) used to populate the replication index from synapse objects.
 *
 */
public class ObjectDataDTO implements Comparable<ObjectDataDTO> {

	private Long id;
	private Long currentVersion;
	private Long createdBy;
	private Date createdOn;
	private String etag;
	private String name;
	private String subType;
	private Long parentId;
	private Long benefactorId;
	private Long projectId;
	private Long modifiedBy;
	private Date modifiedOn;
	private Long fileHandleId;
	private Long fileSizeBytes;
	private Boolean isInSynapseStorage;
	private String fileMD5;
	private List<ObjectAnnotationDTO> annotations;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(Long currentVersion) {
		this.currentVersion = currentVersion;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
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

	public String getSubType() {
		return subType;
	}

	public void setSubType(String type) {
		this.subType = type;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public Long getBenefactorId() {
		return benefactorId;
	}

	public void setBenefactorId(Long benefactorId) {
		this.benefactorId = benefactorId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}

	public List<ObjectAnnotationDTO> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<ObjectAnnotationDTO> annotations) {
		this.annotations = annotations;
	}

	public Long getFileSizeBytes() {
		return this.fileSizeBytes;
	}

	public void setFileSizeBytes(Long fileSizeBytes) {
		this.fileSizeBytes = fileSizeBytes;
	}

	public Boolean getIsInSynapseStorage() {
		return this.isInSynapseStorage;
	}

	public void setIsInSynapseStorage(Boolean isInSynapseStorage) {
		this.isInSynapseStorage = isInSynapseStorage;
	}

	public String getFileMD5() {
		return fileMD5;
	}

	public void setFileMD5(String fileMD5) {
		this.fileMD5 = fileMD5;
	}

	@Override
	public int hashCode() {
		return Objects.hash(annotations, benefactorId, createdBy, createdOn, currentVersion, etag, fileHandleId,
				fileMD5, fileSizeBytes, id, isInSynapseStorage, modifiedBy, modifiedOn, name, parentId, projectId,
				subType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ObjectDataDTO other = (ObjectDataDTO) obj;
		return Objects.equals(annotations, other.annotations) && Objects.equals(benefactorId, other.benefactorId)
				&& Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(currentVersion, other.currentVersion) && Objects.equals(etag, other.etag)
				&& Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(fileMD5, other.fileMD5)
				&& Objects.equals(fileSizeBytes, other.fileSizeBytes) && Objects.equals(id, other.id)
				&& Objects.equals(isInSynapseStorage, other.isInSynapseStorage)
				&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(name, other.name) && Objects.equals(parentId, other.parentId)
				&& Objects.equals(projectId, other.projectId) && Objects.equals(subType, other.subType);
	}

	@Override
	public int compareTo(ObjectDataDTO o) {
		// sort on Id.
		return Long.compare(this.id, o.id);
	}

}
