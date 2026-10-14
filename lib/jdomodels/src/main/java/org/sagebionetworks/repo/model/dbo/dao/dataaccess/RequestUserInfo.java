package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.Date;
import java.util.Objects;

import org.sagebionetworks.repo.model.dataaccess.SubmissionState;

public class RequestUserInfo {

	private String requestId;
	private String accessRequirementId;
	private String accessRequirementName;
	private SubmissionState submissionStatus;
	private String envelopeId;
	private Date submittedOn;
	private Date modifiedOn;
	private Date expiresOn;

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(String accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public String getAccessRequirementName() {
		return accessRequirementName;
	}

	public void setAccessRequirementName(String accessRequirementName) {
		this.accessRequirementName = accessRequirementName;
	}

	public SubmissionState getSubmissionStatus() {
		return submissionStatus;
	}

	public void setSubmissionStatus(SubmissionState submissionStatus) {
		this.submissionStatus = submissionStatus;
	}

	public String getEnvelopeId() {
		return envelopeId;
	}

	public void setEnvelopeId(String envelopeId) {
		this.envelopeId = envelopeId;
	}

	public Date getSubmittedOn() {
		return submittedOn;
	}

	public void setSubmittedOn(Date submittedOn) {
		this.submittedOn = submittedOn;
	}

	public Date getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Date getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(Date expiresOn) {
		this.expiresOn = expiresOn;
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, accessRequirementId, accessRequirementName,
				submissionStatus, envelopeId, submittedOn, modifiedOn, expiresOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		RequestUserInfo other = (RequestUserInfo) obj;
		return Objects.equals(requestId, other.requestId)
				&& Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(accessRequirementName, other.accessRequirementName)
				&& Objects.equals(submissionStatus, other.submissionStatus)
				&& Objects.equals(envelopeId, other.envelopeId)
				&& Objects.equals(expiresOn, other.expiresOn)
				&& Objects.equals(submittedOn, other.submittedOn)
				&& Objects.equals(modifiedOn, other.modifiedOn);
	}
}
