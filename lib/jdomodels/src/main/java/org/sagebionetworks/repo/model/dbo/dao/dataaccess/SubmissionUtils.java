package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.internal.SubmissionContent;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class SubmissionUtils {

	/**
	 * @return The row of the given submission in DATA_ACCESS_SUBMISSION
	 */
	public static DBOSubmission toDbo(Submission dto) {
		DBOSubmission dbo = new DBOSubmission();
		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setAccessRequirementId(Long.parseLong(dto.getAccessRequirementId()));
		dbo.setAccessRequirementVersion(dto.getAccessRequirementVersion());
		dbo.setDataAccessRequestId(Long.parseLong(dto.getRequestId()));
		dbo.setCreatedBy(Long.parseLong(dto.getSubmittedBy()));
		dbo.setCreatedOn(dto.getSubmittedOn().getTime());
		dbo.setEtag(dto.getEtag());
		// The column only references the research project, the snapshot stays whole in the document
		dbo.setResearchProjectId(dto.getResearchProjectSnapshot() == null ? null
				: Long.parseLong(dto.getResearchProjectSnapshot().getId()));
		dbo.setDucFileHandleId(toLong(dto.getDucFileHandleId()));
		dbo.setIrbFileHandleId(toLong(dto.getIrbFileHandleId()));
		dbo.setAttachmentsJson(JDOSecondaryPropertyUtils.writeStringListToJson(dto.getAttachments()));
		dbo.setSubmissionJson(JDOSecondaryPropertyUtils.createJSONFromObject(toContent(dto)));
		return dbo;
	}

	/**
	 * @return The fields of the given submission that have no column of their own and are not stored in the status or
	 *         accessor changes tables
	 */
	static SubmissionContent toContent(Submission dto) {
		return new SubmissionContent()
				.setResearchProjectSnapshot(dto.getResearchProjectSnapshot())
				.setIsRenewalSubmission(dto.getIsRenewalSubmission())
				.setPublication(dto.getPublication())
				.setSummaryOfUse(dto.getSummaryOfUse())
				.setSubjectId(dto.getSubjectId())
				.setSubjectType(dto.getSubjectType())
				.setSchemaData(dto.getSchemaData());
	}

	/**
	 * @return The submission as stored in its own table, the status fields and accessor changes are not set
	 */
	public static Submission toDto(DBOSubmission dbo) {
		SubmissionContent content = JDOSecondaryPropertyUtils.createObjectFromJSON(SubmissionContent.class, dbo.getSubmissionJson());
		return new Submission()
				.setId(dbo.getId().toString())
				.setAccessRequirementId(dbo.getAccessRequirementId().toString())
				.setAccessRequirementVersion(dbo.getAccessRequirementVersion())
				.setRequestId(dbo.getDataAccessRequestId().toString())
				.setSubmittedBy(dbo.getCreatedBy().toString())
				.setSubmittedOn(new Date(dbo.getCreatedOn()))
				.setEtag(dbo.getEtag())
				.setDucFileHandleId(toString(dbo.getDucFileHandleId()))
				.setIrbFileHandleId(toString(dbo.getIrbFileHandleId()))
				.setAttachments(JDOSecondaryPropertyUtils.readJsonToStringList(dbo.getAttachmentsJson()))
				.setResearchProjectSnapshot(content.getResearchProjectSnapshot())
				.setIsRenewalSubmission(content.getIsRenewalSubmission())
				.setPublication(content.getPublication())
				.setSummaryOfUse(content.getSummaryOfUse())
				.setSubjectId(content.getSubjectId())
				.setSubjectType(content.getSubjectType())
				.setSchemaData(content.getSchemaData());
	}

	/**
	 * @param json The JSON array of the accessor changes of a submission, null when it has none
	 * @return The accessor changes ordered by user id, empty when there are none
	 */
	static List<AccessorChange> readAccessorChanges(String json) {
		if (json == null) {
			return new ArrayList<>();
		}
		// JSON_ARRAYAGG does not order its elements
		return JDOSecondaryPropertyUtils.readJsonToEntityList(json, AccessorChange.class).stream()
				.sorted(Comparator.comparing((AccessorChange change) -> Long.valueOf(change.getUserId())))
				.collect(Collectors.toList());
	}

	static Long toLong(String value) {
		return value == null ? null : Long.valueOf(value);
	}

	private static String toString(Long value) {
		return value == null ? null : value.toString();
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
