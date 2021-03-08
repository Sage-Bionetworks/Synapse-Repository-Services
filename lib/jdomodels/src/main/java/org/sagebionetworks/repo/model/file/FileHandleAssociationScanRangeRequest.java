package org.sagebionetworks.repo.model.file;

import java.util.Objects;

/**
 * DTO used to start scanning a range of ids for a given association type
 */
public class FileHandleAssociationScanRangeRequest {

	private Long jobId;
	private FileHandleAssociateType associationType;
	private IdRange idRange;

	public FileHandleAssociationScanRangeRequest() {
	}

	public Long getJobId() {
		return jobId;
	}

	public FileHandleAssociationScanRangeRequest withJobId(Long jobId) {
		this.jobId = jobId;
		return this;
	}

	public FileHandleAssociateType getAssociationType() {
		return associationType;
	}

	public FileHandleAssociationScanRangeRequest withAssociationType(FileHandleAssociateType associationType) {
		this.associationType = associationType;
		return this;
	}

	public IdRange getIdRange() {
		return idRange;
	}

	public FileHandleAssociationScanRangeRequest withIdRange(IdRange idRange) {
		this.idRange = idRange;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(associationType, idRange, jobId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileHandleAssociationScanRangeRequest other = (FileHandleAssociationScanRangeRequest) obj;
		return associationType == other.associationType && Objects.equals(idRange, other.idRange) && Objects.equals(jobId, other.jobId);
	}

	@Override
	public String toString() {
		return "FileHandleAssociationScanRangeRequest [jobId=" + jobId + ", associationType=" + associationType + ", idRange=" + idRange
				+ "]";
	}

}
