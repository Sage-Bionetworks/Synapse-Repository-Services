package org.sagebionetworks.repo.competition.model;

import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Bundle to transport a Submission and its accompanying SubmissionStatus object
 * 
 * @author bkng
 *
 */
public class SubmissionBundle implements JSONEntity {
	
	public static final String JSON_SUBMISSION = "submisison";
	public static final String JSON_SUBMISSION_STATUS = "submissionStatus";
	
	private Submission submission;
	private SubmissionStatus submissionStatus;	
	
	/**
	 * Create a new SubmissionBundle
	 */
	public SubmissionBundle() {}
	
	/**
	 * Create a new SubmissionBundle and initialize from a JSONObjectAdapter.
	 * 
	 * @param initializeFrom
	 * @throws JSONObjectAdapterException
	 */
	public SubmissionBundle(JSONObjectAdapter initializeFrom) throws JSONObjectAdapterException {
		this();
		initializeFromJSONObject(initializeFrom);
	}

	@Override
	public JSONObjectAdapter initializeFromJSONObject(
			JSONObjectAdapter toInitFrom) throws JSONObjectAdapterException {
		if (toInitFrom == null) {
            throw new IllegalArgumentException("org.sagebionetworks.schema.adapter.JSONObjectAdapter cannot be null");
        }	
		if (toInitFrom.has(JSON_SUBMISSION)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_SUBMISSION);
			submission = new Submission();
			submission.initializeFromJSONObject(joa);
		}
		if (toInitFrom.has(JSON_SUBMISSION_STATUS)) {
			JSONObjectAdapter joa = (JSONObjectAdapter) toInitFrom.getJSONObject(JSON_SUBMISSION_STATUS);
			submissionStatus = new SubmissionStatus();
			submissionStatus.initializeFromJSONObject(joa);
		}
		return toInitFrom;
	}

	@Override
	public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo)
			throws JSONObjectAdapterException {
		if (writeTo == null) {
		        throw new IllegalArgumentException("JSONObjectAdapter cannot be null");
		}
		if (submission != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			submission.writeToJSONObject(joa);
			writeTo.put(JSON_SUBMISSION, joa);
		}
		if (submissionStatus != null) {
			JSONObjectAdapter joa = writeTo.createNew();
			submissionStatus.writeToJSONObject(joa);
			writeTo.put(JSON_SUBMISSION_STATUS, joa);
		}
		return writeTo;
	}

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
	public String getJSONSchema() {
		// Auto-generated method stub
		return null;
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
		SubmissionBundle other = (SubmissionBundle) obj;
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
		return "SubmissionBundle [submission=" + submission
				+ ", submissionStatus=" + submissionStatus + "]";
	}


}
