package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class SubmissionUtils {

	public static void copyDtoToDbo(Submission dto, DBOSubmission dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setDataAccessRequestId(Long.parseLong(dto.getRequestId()));
		dbo.setCreatedBy(Long.parseLong(dto.getSubmittedBy()));
		dbo.setCreatedOn(dto.getSubmittedOn().getTime());
		dbo.setEtag(dto.getEtag());
		copyToSerializedField(dto, dbo);
	}

	private static void copyToSerializedField(Submission dto, DBOSubmission dbo) {
		try {
			dbo.setSubmissionSerialized(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static List<DBOSubmissionAccessor> createDBOSubmissionAccessor(Submission dto, IdGenerator idGenerator) {
		List<DBOSubmissionAccessor> accessors = new ArrayList<DBOSubmissionAccessor>();
		for (AccessorChange change : dto.getAccessorChanges()) {
			if(AccessType.REVOKE_ACCESS.equals(change.getType())){
				// do not record users that are being revoked
				continue;
			}
			DBOSubmissionAccessor dbo = new DBOSubmissionAccessor();
			dbo.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ACCESSOR_ID));
			dbo.setAccessorId(Long.parseLong(change.getUserId()));
			dbo.setCurrentSubmissionId(Long.parseLong(dto.getId()));
			dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
			dbo.setEtag(UUID.randomUUID().toString());
			accessors.add(dbo);
		}
		return accessors;
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
		try {
			return (Submission)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSubmissionSerialized());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
