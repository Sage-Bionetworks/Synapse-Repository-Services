package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class SubmissionUtils {

	/**
	 * Decodes the serialized blob that submissions used to be stored as. Only used to read backups that predate the JSON document,
	 * remove with {@link DBOSubmission#getTranslator()}'s legacy handling once every stack has been migrated, see PLFM-10061.
	 */
	private static final UnmodifiableXStream LEGACY_X_STREAM = UnmodifiableXStream.builder().allowTypes(Submission.class).allowTypesByWildcard(new String[] {"org.sagebionetworks.repo.model.**"}).build();

	public static void copyDtoToDbo(Submission dto, DBOSubmission dbo) throws DatastoreException{
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setAccessRequirementVersion(dto.getAccessRequirementVersion());
		dbo.setDataAccessRequestId(Long.parseLong(dto.getRequestId()));
		dbo.setCreatedBy(Long.parseLong(dto.getSubmittedBy()));
		dbo.setCreatedOn(dto.getSubmittedOn().getTime());
		dbo.setEtag(dto.getEtag());
		dbo.setResearchProjectId(dto.getResearchProjectSnapshot() == null ? null
				: Long.parseLong(dto.getResearchProjectSnapshot().getId()));
		dbo.setDucFileHandleId(toLong(dto.getDucFileHandleId()));
		dbo.setIrbFileHandleId(toLong(dto.getIrbFileHandleId()));
		dbo.setAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(dto.getAttachments()));
		dbo.setSubmissionJson(createSubmissionJson(dto));
	}

	/**
	 * @return The JSON document of the submission: every field of the submission except the ones stored in their own column or
	 *         in the status and accessor changes tables
	 */
	static String createSubmissionJson(Submission dto) {
		// Cleared on a copy so that the caller's object is left untouched and each value is stored once. The research project
		// snapshot stays whole in the document, its id column only references the research project.
		Submission document = JDOSecondaryPropertyUtils.createObjectFromJSON(Submission.class,
				JDOSecondaryPropertyUtils.createJSONFromObject(dto));
		document.setId(null);
		document.setAccessRequirementId(null);
		document.setAccessRequirementVersion(null);
		document.setRequestId(null);
		document.setSubmittedBy(null);
		document.setSubmittedOn(null);
		document.setEtag(null);
		document.setDucFileHandleId(null);
		document.setIrbFileHandleId(null);
		document.setAttachments(null);
		// Stored in DATA_ACCESS_SUBMISSION_STATUS
		document.setState(null);
		document.setModifiedBy(null);
		document.setModifiedOn(null);
		document.setRejectedReason(null);
		// Stored in DATA_ACCESS_SUBMISSION_ACCESSOR_CHANGES
		document.setAccessorChanges(null);
		return JDOSecondaryPropertyUtils.createJSONFromObject(document);
	}

	private static Long toLong(String value) {
		return value == null ? null : Long.valueOf(value);
	}

	/**
	 * Populates the document and file handle columns of the given backup from the legacy serialized blob and clears the blob.
	 * 
	 * @throws IllegalArgumentException If a column that the blob would populate is already set
	 */
	public static void moveLegacySerializedFieldToColumns(DBOSubmission backup) {
		if (backup.getSubmissionJson() != null || backup.getDucFileHandleId() != null || backup.getIrbFileHandleId() != null
				|| backup.getAttachmentsJson() != null) {
			throw new IllegalArgumentException(String.format(
					"Both '%s' and the submission document or file handle columns are not null", "submissionSerialized"));
		}
		Submission legacy = readLegacySerializedField(backup.getSubmissionSerialized());
		// The columns that sat beside the blob take precedence over their copies inside the blob
		legacy.setId(backup.getId().toString());
		legacy.setAccessRequirementId(backup.getAccessRequirementId().toString());
		if (backup.getAccessRequirementVersion() != null) {
			legacy.setAccessRequirementVersion(backup.getAccessRequirementVersion());
		}
		legacy.setRequestId(backup.getDataAccessRequestId().toString());
		legacy.setSubmittedBy(backup.getCreatedBy().toString());
		legacy.setSubmittedOn(new Date(backup.getCreatedOn()));
		legacy.setEtag(backup.getEtag());
		copyDtoToDbo(legacy, backup);
		backup.setSubmissionSerialized(null);
	}

	public static Submission readLegacySerializedField(byte[] serializedField) {
		try {
			return (Submission) JDOSecondaryPropertyUtils.decompressObject(LEGACY_X_STREAM, serializedField);
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
	
	public static List<DBOSubmissionAccessorChange> createDBOSubmissionAccessorChanges(Submission dto) {
		if (dto.getAccessorChanges() == null || dto.getAccessorChanges().isEmpty()) {
			return Collections.emptyList();
		}
		
		return dto.getAccessorChanges().stream().map(accessorChange -> {
			DBOSubmissionAccessorChange dbo = new DBOSubmissionAccessorChange();
			dbo.setSubmissionId(Long.valueOf(dto.getId()));
			dbo.setAccessorId(Long.valueOf(accessorChange.getUserId()));
			dbo.setAccessType(accessorChange.getType().name());
			return dbo;
		}).collect(Collectors.toList());		
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

	/**
	 * @return The submission as stored in its own table, the status fields and accessor changes are not set
	 */
	public static Submission copyDboToDto(DBOSubmission dbo) {
		Submission dto = JDOSecondaryPropertyUtils.createObjectFromJSON(Submission.class, dbo.getSubmissionJson());
		dto.setId(dbo.getId().toString());
		dto.setAccessRequirementId(dbo.getAccessRequirementId().toString());
		dto.setAccessRequirementVersion(dbo.getAccessRequirementVersion());
		dto.setRequestId(dbo.getDataAccessRequestId().toString());
		dto.setSubmittedBy(dbo.getCreatedBy().toString());
		dto.setSubmittedOn(new Date(dbo.getCreatedOn()));
		dto.setEtag(dbo.getEtag());
		dto.setDucFileHandleId(dbo.getDucFileHandleId() == null ? null : dbo.getDucFileHandleId().toString());
		dto.setIrbFileHandleId(dbo.getIrbFileHandleId() == null ? null : dbo.getIrbFileHandleId().toString());
		dto.setAttachments(JDOSecondaryPropertyUtils.readJsonToStringList(dbo.getAttachmentsJson()));
		return dto;
	}

	public static Submission copyDboToDto(DBOSubmission dbo, DBOSubmissionStatus status) {
		try {
			Submission dto = copyDboToDto(dbo);
			dto.setModifiedBy(status.getModifiedBy().toString());
			dto.setModifiedOn(new Date(status.getModifiedOn()));
			dto.setState(SubmissionState.valueOf(status.getState()));
			dto.setRejectedReason(new String(status.getReason(), "UTF-8"));
			return dto;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException();
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
