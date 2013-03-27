package org.sagebionetworks.doi;

import org.sagebionetworks.repo.model.doi.DoiObjectType;

public class EzidDoi {

	public String getObjectId() {
		if (objectId == null || objectId.isEmpty()) {
			throw new NullPointerException("Missing object ID. Object ID is required");
		}
		return objectId;
	}

	public void setObjectId(String objectId) {
		if (objectId == null || objectId.isEmpty()) {
			throw new IllegalArgumentException("Missing object ID. Object ID is required");
		}
		this.objectId = objectId;
	}

	public DoiObjectType getDoiObjectType() {
		if (doiObjectType == null) {
			throw new NullPointerException("Missing object type. Object type is required");
		}
		return doiObjectType;
	}

	public void setDoiObjectType(DoiObjectType doiObjectType) {
		if (doiObjectType == null) {
			throw new IllegalArgumentException("Missing object type. Object type is required");
		}
		this.doiObjectType = doiObjectType;
	}

	public Long getObjectVersion() {
		return objectVersion;
	}

	public void setObjectVersion(Long objectVersion) {
		this.objectVersion = objectVersion;
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

	private String objectId;
    private DoiObjectType doiObjectType;
    private Long objectVersion;
    private String doi;
    private EzidMetadata metadata;
}
