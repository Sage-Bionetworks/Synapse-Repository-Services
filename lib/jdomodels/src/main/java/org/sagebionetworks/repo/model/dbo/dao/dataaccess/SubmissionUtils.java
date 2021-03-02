package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class SubmissionUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(Submission.class).build();


	public static void copyDtoToDbo(Submission dto, DBOSubmission dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setDataAccessRequestId(Long.parseLong(dto.getRequestId()));
		dbo.setCreatedBy(Long.parseLong(dto.getSubmittedBy()));
		dbo.setCreatedOn(dto.getSubmittedOn().getTime());
		dbo.setEtag(dto.getEtag());
		dbo.setResearchProjectId(Long.parseLong(dto.getResearchProjectSnapshot().getId()));
		copyToSerializedField(dto, dbo);
	}

	private static void copyToSerializedField(Submission dto, DBOSubmission dbo) {
		dbo.setSubmissionSerialized(writeSerializedField(dto));
	}
	
	public static byte[] writeSerializedField(Submission dto) {
		try {
			return JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static DBOSubmissionSubmitter createDBOSubmissionSubmitter(Submission dto, IdGenerator idGenerator) {
		DBOSubmissionSubmitter dbo = new DBOSubmissionSubmitter();
		dbo.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_SUBMITTER_ID));
		dbo.setSubmitterId(Long.parseLong(dto.getSubmittedBy()));
		dbo.setCurrentSubmissionId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setEtag(UUID.randomUUID().toString());
		return dbo;
	}

	public static DBOSubmissionStatus getDBOStatus(Submission dto) {
		try {
			DBOSubmissionStatus dbo = new DBOSubmissionStatus();
			dbo.setSubmisionId(Long.parseLong(dto.getId()));
			dbo.setCreatedBy(Long.parseLong(dto.getSubmittedBy()));
			dbo.setCreatedOn(dto.getSubmittedOn().getTime());
			dbo.setModifiedBy(Long.parseLong(dto.getModifiedBy()));
			dbo.setModifiedOn(dto.getModifiedOn().getTime());
			dbo.setState(dto.getState().toString());
			if (dto.getRejectedReason() != null) {
				dbo.setReason(dto.getRejectedReason().getBytes("UTF-8"));
			}
			return dbo;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException();
		}
	}

	public static Submission copyDboToDto(DBOSubmission dbo, DBOSubmissionStatus status) {
		try {
			Submission dto = copyFromSerializedField(dbo);
			dto.setId(dbo.getId().toString());
			dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
			dto.setSubmittedBy(dbo.getCreatedBy().toString());
			dto.setSubmittedOn(new Date(dbo.getCreatedOn()));
			dto.setEtag(dbo.getEtag());
			dto.setModifiedBy(status.getModifiedBy().toString());
			dto.setModifiedOn(new Date(status.getModifiedOn()));
			dto.setState(SubmissionState.valueOf(status.getState()));
			dto.setRejectedReason(new String(status.getReason(), "UTF-8"));
			return dto;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException();
		}
	}

	private static Submission copyFromSerializedField(DBOSubmission dbo) {
		return readSerializedField(dbo.getSubmissionSerialized());
	}
	
	public static Submission readSerializedField(byte[] serializedField) {
		try {
			return (Submission)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, serializedField);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static Set<String> extractAllFileHandleIds(Submission submission) {
		Set<String> associatedIds = new HashSet<String>();
		if (submission.getAttachments()!= null && !submission.getAttachments().isEmpty()) {
			associatedIds.addAll(submission.getAttachments());
		}
		if (submission.getDucFileHandleId() != null) {
			associatedIds.add(submission.getDucFileHandleId());
		}
		if (submission.getIrbFileHandleId() != null) {
			associatedIds.add(submission.getIrbFileHandleId());
		}
		return associatedIds;
	}
}
