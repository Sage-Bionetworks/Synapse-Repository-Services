package org.sagebionetworks.repo.model.table;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.EntityType;

/**
 * Entity Data Transfer Object (TDO).
 *
 */
public class EntityDTO implements Comparable<EntityDTO> {
	
	Long id;
	Long currentVersion;
	Long createdBy;
	Date createdOn;
	String etag;
	String name;
	EntityType type;
	Long parentId;
	Long benefactorId;
	Long projectId;
	Long modifiedBy;
	Date modifiedOn;
	Long fileHandleId;
	Long fileSizeBytes;
	Boolean isInSynapseStorage;
	List<AnnotationDTO> annotations;
	
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
	public EntityType getType() {
		return type;
	}
	public void setType(EntityType type) {
		this.type = type;
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
	public List<AnnotationDTO> getAnnotations() {
		return annotations;
	}
	public void setAnnotations(List<AnnotationDTO> annotations) {
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((benefactorId == null) ? 0 : benefactorId.hashCode());
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((currentVersion == null) ? 0 : currentVersion.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((fileSizeBytes == null) ? 0 : fileSizeBytes.hashCode());
		result = prime * result + ((isInSynapseStorage == null) ? 0 : isInSynapseStorage.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		EntityDTO other = (EntityDTO) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (benefactorId == null) {
			if (other.benefactorId != null)
				return false;
		} else if (!benefactorId.equals(other.benefactorId))
			return false;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (currentVersion == null) {
			if (other.currentVersion != null)
				return false;
		} else if (!currentVersion.equals(other.currentVersion))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (fileSizeBytes == null) {
			if (other.fileSizeBytes != null)
				return false;
		} else if (!fileSizeBytes.equals(other.fileSizeBytes))
			return false;
		if (isInSynapseStorage == null) {
			if (other.isInSynapseStorage != null)
				return false;
		} else if (!isInSynapseStorage.equals(other.isInSynapseStorage))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
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
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "EntityDTO [id=" + id + ", currentVersion=" + currentVersion + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + ", etag=" + etag + ", name=" + name + ", type=" + type + ", parentId="
				+ parentId + ", benefactorId=" + benefactorId + ", projectId=" + projectId + ", modifiedBy="
				+ modifiedBy + ", modifiedOn=" + modifiedOn + ", fileHandleId=" + fileHandleId + ", fileSizeBytes="
				+ fileSizeBytes + ", isInSynapseStorage=" + isInSynapseStorage + ", annotations=" + annotations + "]";
	}
	@Override
	public int compareTo(EntityDTO o) {
		// sort on Id.
		return Long.compare(this.id, o.id);
	}
	
}
