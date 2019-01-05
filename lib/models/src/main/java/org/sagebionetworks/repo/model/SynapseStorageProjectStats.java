package org.sagebionetworks.repo.model;

/**
 * Data transfer object to capture an objects Id and etag.
 * 
 */
public class SynapseStorageProjectStats {

	Long id;
	String projectName;
	Long size;
	Double proportion;

	/**
	 *
	 * @param id Project ID
	 * @param projectName Project name
	 * @param size Size of all files in the project in Synapse storage
	 * @param proportion The proportion of Synapse storage this project takes up
	 */
	public SynapseStorageProjectStats(Long id, String projectName, Long size, Double proportion) {
		this.id = id;
		this.projectName = projectName;
		this.size = size;
		this.proportion = proportion;
	}
	
	
	public Long getId() {
		return id;
	}

	public String getProjectName() {
		return projectName;
	}

	public Long getSize() {
		return size;
	}

	public Double getProportion() {
		return proportion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((proportion == null) ? 0 : proportion.hashCode());
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		if (proportion == null) {
			if (other.proportion != null)
				return false;
		} else if (!proportion.equals(other.proportion))
			return false;
		if (size == null) {
			if (other.size != null)
				return false;
		} else if (!size.equals(other.size))
			return false;
		if (projectName == null) {
			if (other.projectName != null)
				return false;
		} else if (!projectName.equals(other.projectName))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
}
