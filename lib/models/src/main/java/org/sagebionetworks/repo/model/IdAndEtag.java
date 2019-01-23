package org.sagebionetworks.repo.model;

/**
 * Data transfer object to capture an objects Id and etag.
 * 
 */
public class IdAndEtag {

	Long id;
	String etag;
	Long benefactorId;
	Long projectId;
	
	/**
	 * @param id Object's ID
	 * @param etag Object's etag
	 */
	public IdAndEtag(Long id, String etag, Long benefactor, Long project) {
		super();
		this.id = id;
		this.etag = etag;
		this.benefactorId = benefactor;
		this.projectId = project;
	}
	
	
	public Long getId() {
		return id;
	}

	public String getEtag() {
		return etag;
	}

	public Long getBenefactorId(){
		return benefactorId;
	}
	
	public Long getProjectId() {
		return projectId;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((benefactorId == null) ? 0 : benefactorId.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		IdAndEtag other = (IdAndEtag) obj;
		if (benefactorId == null) {
			if (other.benefactorId != null)
				return false;
		} else if (!benefactorId.equals(other.benefactorId))
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
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "IdAndEtag [id=" + id + ", etag=" + etag + ", benefactorId=" + benefactorId + ", projectId=" + projectId
				+ "]";
	}
	
}
