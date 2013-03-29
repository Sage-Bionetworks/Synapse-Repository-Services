package org.sagebionetworks.doi;

import org.sagebionetworks.repo.model.doi.Doi;

public class EzidDoi {

	public Doi getDto() {
		if (dto == null) {
			throw new NullPointerException("Missing the DOI DTO. DOI DTO is required");
		}
		return dto;
	}

	public void setDto(Doi dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Missing the DOI DTO. DOI DTO is required");
		}
		this.dto = dto;
	}

	public String getDoi() {
		if (doi == null || doi.isEmpty()) {
			throw new NullPointerException("Missing DOI. DOI is required");
		}
		return doi;
	}

	public void setDoi(String doi) {
		if (doi == null || doi.isEmpty()) {
			throw new IllegalArgumentException("Missing DOI. DOI is required");
		}
		this.doi = doi;
	}

	public EzidMetadata getMetadata() {
		if (metadata == null) {
			throw new NullPointerException("Missing metadata. Metadata is required");
		}
		return metadata;
	}

	public void setMetadata(EzidMetadata metadata) {
		if (metadata == null) {
			throw new IllegalArgumentException("Missing metadata. Metadata is required");
		}
		this.metadata = metadata;
	}

	private Doi dto;
    private String doi;
    private EzidMetadata metadata;
}
