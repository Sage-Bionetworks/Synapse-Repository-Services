package org.sagebionetworks.repo.model;

import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;

public class SubmissionBackup {

	private Submission submission;
	private SubmissionStatus submissionStatus;
	
	public Submission getSubmission() {
		return submission;
	}
	public void setSubmission(Submission submission) {
		this.submission = submission;
	}
	public SubmissionStatus getSubmissionStatus() {
		return submissionStatus;
	}
	public void setSubmissionStatus(SubmissionStatus submissionStatus) {
		this.submissionStatus = submissionStatus;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((submission == null) ? 0 : submission.hashCode());
		result = prime
				* result
				+ ((submissionStatus == null) ? 0 : submissionStatus.hashCode());
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
		SubmissionBackup other = (SubmissionBackup) obj;
		if (submission == null) {
			if (other.submission != null)
				return false;
		} else if (!submission.equals(other.submission))
			return false;
		if (submissionStatus == null) {
			if (other.submissionStatus != null)
				return false;
		} else if (!submissionStatus.equals(other.submissionStatus))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "SubmissionBackup [submission=" + submission
				+ ", submissionStatus=" + submissionStatus + "]";
	}
}
