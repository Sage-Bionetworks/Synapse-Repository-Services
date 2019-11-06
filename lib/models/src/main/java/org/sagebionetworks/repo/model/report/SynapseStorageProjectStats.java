package org.sagebionetworks.repo.model.report;

public class SynapseStorageProjectStats {

	String projectId;
	String projectName;
	Long sizeInBytes;

	public void setId(String projectId) {
		this.projectId = projectId;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public void setSizeInBytes(Long sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public String getId() {
		return projectId;
	}

	public String getProjectName() {
		return projectName;
	}

	public Long getSizeInBytes() {
		return sizeInBytes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sizeInBytes == null) ? 0 : sizeInBytes.hashCode());
		result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
		result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
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
		SynapseStorageProjectStats other = (SynapseStorageProjectStats) obj;
		if (sizeInBytes == null) {
			if (other.sizeInBytes != null)
				return false;
		} else if (!sizeInBytes.equals(other.sizeInBytes))
			return false;
		if (projectName == null) {
			if (other.projectName != null)
				return false;
		} else if (!projectName.equals(other.projectName))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		return true;
	}


}