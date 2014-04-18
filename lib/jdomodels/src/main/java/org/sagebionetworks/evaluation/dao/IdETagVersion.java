package org.sagebionetworks.evaluation.dao;

public class IdETagVersion {
	private Long id;
	private String etag;
	private Long version;
	
	public IdETagVersion(Long id, String etag, Long version) {
		this.id=id;
		this.etag=etag;
		this.version = version;
	}
	
	public Long getId() {
		return id;
	}
	public String getEtag() {
		return etag;
	}
	public Long getVersion() {
		return version;
	}
}
