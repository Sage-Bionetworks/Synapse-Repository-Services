package org.sagebionetworks.repo.model;

/**
 * Data transfer object to capture an objects Id and etag.
 * 
 */
public class IdAndEtag {

	Long id;
	String etag;
	
	/**
	 * @param id Object's ID
	 * @param etag Object's etag
	 */
	public IdAndEtag(Long id, String etag) {
		super();
		this.id = id;
		this.etag = etag;
	}
	
	
	public Long getId() {
		return id;
	}

	public String getEtag() {
		return etag;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
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
		IdAndEtag other = (IdAndEtag) obj;
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
		return true;
	}
	
	
}
