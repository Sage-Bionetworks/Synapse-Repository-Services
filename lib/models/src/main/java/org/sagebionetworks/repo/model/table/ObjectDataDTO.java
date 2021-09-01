package org.sagebionetworks.repo.model.table;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Data Transfer Object (DTO) used to populate the replication index from synapse objects.
 *
 */
public class ObjectDataDTO implements Comparable<ObjectDataDTO> {

	private Long id;
	private Long currentVersion;
	private Long version;
	private Long createdBy;
	private Date createdOn;
	private String etag;
	private String name;
	private SubType subType;
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

	public ObjectDataDTO setId(Long id) {
		this.id = id;
		return this;
	}
	
	public Long getCurrentVersion() {
		return currentVersion;
	}

	public ObjectDataDTO setCurrentVersion(Long currentVersion) {
		this.currentVersion = currentVersion;
		return this;
	}

	/**
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public ObjectDataDTO setVersion(Long version) {
		this.version = version;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public ObjectDataDTO setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public ObjectDataDTO setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public ObjectDataDTO setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getName() {
		return name;
	}

	public ObjectDataDTO setName(String name) {
		this.name = name;
		return this;
	}

	public SubType getSubType() {
		return subType;
	}

	public ObjectDataDTO setSubType(SubType type) {
		this.subType = type;
		return this;
	}

	public Long getParentId() {
		return parentId;
	}

	public ObjectDataDTO setParentId(Long parentId) {
		this.parentId = parentId;
		return this;
	}

	public Long getBenefactorId() {
		return benefactorId;
	}

	public ObjectDataDTO setBenefactorId(Long benefactorId) {
		this.benefactorId = benefactorId;
		return this;
	}

	public Long getProjectId() {
		return projectId;
	}

	public ObjectDataDTO setProjectId(Long projectId) {
		this.projectId = projectId;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public ObjectDataDTO setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public ObjectDataDTO setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public ObjectDataDTO setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
		return this;
	}

	public List<ObjectAnnotationDTO> getAnnotations() {
		return annotations;
	}

	public ObjectDataDTO setAnnotations(List<ObjectAnnotationDTO> annotations) {
		this.annotations = annotations;
		return this;
	}

	public Long getFileSizeBytes() {
		return this.fileSizeBytes;
	}

	public ObjectDataDTO setFileSizeBytes(Long fileSizeBytes) {
		this.fileSizeBytes = fileSizeBytes;
		return this;
	}

	public Boolean getIsInSynapseStorage() {
		return this.isInSynapseStorage;
	}

	public ObjectDataDTO setIsInSynapseStorage(Boolean isInSynapseStorage) {
		this.isInSynapseStorage = isInSynapseStorage;
		return this;
	}

	public String getFileMD5() {
		return fileMD5;
	}

	public ObjectDataDTO setFileMD5(String fileMD5) {
		this.fileMD5 = fileMD5;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(annotations, benefactorId, createdBy, createdOn, currentVersion, etag, fileHandleId,
				fileMD5, fileSizeBytes, id, isInSynapseStorage, modifiedBy, modifiedOn, name, parentId, projectId,
				subType, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ObjectDataDTO)) {
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
				&& Objects.equals(projectId, other.projectId) && subType == other.subType
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "ObjectDataDTO [id=" + id + ", currentVersion=" + currentVersion + ", version=" + version
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", etag=" + etag + ", name=" + name
				+ ", subType=" + subType + ", parentId=" + parentId + ", benefactorId=" + benefactorId + ", projectId="
				+ projectId + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn + ", fileHandleId="
				+ fileHandleId + ", fileSizeBytes=" + fileSizeBytes + ", isInSynapseStorage=" + isInSynapseStorage
				+ ", fileMD5=" + fileMD5 + ", annotations=" + annotations + "]";
	}

	@Override
	public int compareTo(ObjectDataDTO o) {
		// sort on Id then version
		int idComp = Long.compare(this.id, o.id);
		if(idComp == 0) {
			return Long.compare(this.version, o.version);
		}else {
			return idComp;
		}
	}
	
	/**
	 * returns 'id.version'
	 * @return
	 */
	public String getIdVersion() {
		return String.format("%s.%s", this.id, this.version);
	}
	
	/**
	 * Deduplcate the passed collection of ObjectDataDTO based on 'id.version'
	 * @param in
	 * @return
	 */
	public static Collection<ObjectDataDTO> deDuplicate(Collection<ObjectDataDTO> in){
		Map<String, ObjectDataDTO> deduplicated = in.stream().collect(
				Collectors.toMap(ObjectDataDTO::getIdVersion, Function.identity(), (a, b) -> b)
		);
		return deduplicated.values();
	}

}
