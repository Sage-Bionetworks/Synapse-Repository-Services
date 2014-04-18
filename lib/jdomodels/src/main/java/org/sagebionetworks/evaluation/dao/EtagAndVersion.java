package org.sagebionetworks.evaluation.dao;

public class EtagAndVersion {
	private String Etag;
	private Long version;
	
	
	public EtagAndVersion(String etag, Long version) {
		super();
		Etag = etag;
		this.version = version;
	}
	
	public String getEtag() {
		return Etag;
	}
	public void setEtag(String etag) {
		Etag = etag;
	}
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}
	
	
	

}
