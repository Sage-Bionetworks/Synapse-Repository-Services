package org.sagebionetworks.repo.model;

import java.util.Objects;

/**
 * Data transfer object to capture an objects Id and etag.
 * 
 */
public class IdAndEtag {

	Long id;
	Long version;
	String etag;
	Long benefactorId;
	
	/**
	 * @param id Object's ID
	 * @param etag Object's etag
	 */
	public IdAndEtag(Long id, String etag, Long benefactor) {
		super();
		this.id = id;
		this.etag = etag;
		this.benefactorId = benefactor;
	}
	
	public void setBenefactorId(Long benefactorId) {
		this.benefactorId = benefactorId;
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

	@Override
	public int hashCode() {
		return Objects.hash(benefactorId, etag, id, version);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof IdAndEtag)) {
			return false;
		}
		IdAndEtag other = (IdAndEtag) obj;
		return Objects.equals(benefactorId, other.benefactorId) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(version, other.version);
	}


	@Override
	public String toString() {
		return "IdAndEtag [id=" + id + ", version=" + version + ", etag=" + etag + ", benefactorId=" + benefactorId
				+ "]";
	}
		
}
