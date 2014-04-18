package org.sagebionetworks.evaluation.dao;

public class EtagAndVersion {
	private Long id;
	private String Etag;
	private Long version;
	
	
	public EtagAndVersion(Long id, String etag, Long version) {
		super();
		this.id=id;
		this.Etag = etag;
		this.version = version;
	}
	
	public Long getId() {
		return id;
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
