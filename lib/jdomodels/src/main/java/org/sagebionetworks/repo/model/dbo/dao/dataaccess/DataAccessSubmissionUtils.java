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
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DataAccessSubmissionUtils {

	public static void copyDtoToDbo(DataAccessSubmission dto, DBODataAccessSubmission dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setDataAccessRequestId(Long.parseLong(dto.getDataAccessRequestId()));
		dbo.setCreatedBy(Long.parseLong(dto.getSubmittedBy()));
		dbo.setCreatedOn(dto.getSubmittedOn().getTime());
		dbo.setEtag(dto.getEtag());
		copyToSerializedField(dto, dbo);
	}

	private static void copyToSerializedField(DataAccessSubmission dto, DBODataAccessSubmission dbo) {
		try {
			dbo.setSubmissionSerialized(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

	public static List<DBODataAccessSubmissionAccessor> createDBODataAccessSubmissionAccessor(DataAccessSubmission dto, IdGenerator idGenerator) {
		List<DBODataAccessSubmissionAccessor> accessors = new ArrayList<DBODataAccessSubmissionAccessor>();
		for (String userId : dto.getAccessors()) {
			DBODataAccessSubmissionAccessor dbo = new DBODataAccessSubmissionAccessor();
			dbo.setId(idGenerator.generateNewId(IdType.DATA_ACCESS_SUBMISSION_ACCESSOR_ID));
			dbo.setAccessorId(Long.parseLong(userId));
			dbo.setCurrentSubmissionId(Long.parseLong(dto.getId()));
			dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
			dbo.setEtag(UUID.randomUUID().toString());
			accessors.add(dbo);
		}
		return accessors;
	}

	public static DBODataAccessSubmissionStatus getDBOStatus(DataAccessSubmission dto) {
		try {
			DBODataAccessSubmissionStatus dbo = new DBODataAccessSubmissionStatus();
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

	public static DataAccessSubmission copyDboToDto(DBODataAccessSubmission dbo, DBODataAccessSubmissionStatus status) {
		try {
			DataAccessSubmission dto = copyFromSerializedField(dbo);
			dto.setId(dbo.getId().toString());
			dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
			dto.setSubmittedBy(dbo.getCreatedBy().toString());
			dto.setSubmittedOn(new Date(dbo.getCreatedOn()));
			dto.setEtag(dbo.getEtag());
			dto.setModifiedBy(status.getModifiedBy().toString());
			dto.setModifiedOn(new Date(status.getModifiedOn()));
			dto.setState(DataAccessSubmissionState.valueOf(status.getState()));
			dto.setRejectedReason(new String(status.getReason(), "UTF-8"));
			return dto;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException();
		}
	}

	private static DataAccessSubmission copyFromSerializedField(DBODataAccessSubmission dbo) {
		try {
			return (DataAccessSubmission)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSubmissionSerialized());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
}
