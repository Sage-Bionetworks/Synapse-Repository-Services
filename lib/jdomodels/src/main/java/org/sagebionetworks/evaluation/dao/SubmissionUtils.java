package org.sagebionetworks.evaluation.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.evaluation.dbo.SubmissionContributorDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class SubmissionUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(SubmissionStatus.class).build();

	/**
	 * Copy a Submission data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	public static void copyDtoToDbo(Submission dto, SubmissionDBO dbo) {	
		try {
			dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Submission ID: " + dto.getId());
		}
		try {
			dbo.setUserId(dto.getUserId() == null ? null : Long.parseLong(dto.getUserId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid User ID: " + dto.getUserId());
		}
		dbo.setSubmitterAlias(dto.getSubmitterAlias());
		try {
			dbo.setEvalId(dto.getEvaluationId() == null ? null : Long.parseLong(dto.getEvaluationId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Evaluation ID: " + dto.getEvaluationId());
		}
		try {
			dbo.setEvalRoundId(dto.getEvaluationRoundId() == null ? null : Long.parseLong(dto.getEvaluationRoundId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Round Evaluation ID: " + dto.getEvaluationId());
		}
		dbo.setEntityId(dto.getEntityId() == null ? null : KeyFactory.stringToKey(dto.getEntityId()));
		dbo.setVersionNumber(dto.getVersionNumber());
		dbo.setName(dto.getName());
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
		dbo.setEntityBundle(dto.getEntityBundleJSON() == null ? null : dto.getEntityBundleJSON().getBytes());
		try {
			dbo.setTeamId(dto.getTeamId() == null ? null : Long.parseLong(dto.getTeamId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Team ID: " + dto.getId());
		}
		dbo.setDockerRepositoryName(dto.getDockerRepositoryName());
		dbo.setDockerDigest(dto.getDockerDigest());
	}
	
	/**
	 * Copy a SubmissionDBO database object to a Submission data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	public static void copyDboToDto(SubmissionDBO dbo, Submission dto) throws DatastoreException {
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setUserId(dbo.getUserId() == null ? null : dbo.getUserId().toString());
		dto.setSubmitterAlias(dbo.getSubmitterAlias());
		dto.setEvaluationId(dbo.getEvalId() == null ? null : dbo.getEvalId().toString());
		dto.setEvaluationRoundId(dbo.getEvalRoundId() == null ? null : dbo.getEvalRoundId().toString());
		dto.setEntityId(dbo.getEntityId() == null ? null : KeyFactory.keyToString(dbo.getEntityId()));
		dto.setVersionNumber(dbo.getVersionNumber());
		dto.setName(dbo.getName());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setEntityBundleJSON(dbo.getEntityBundle() == null ? null : new String(dbo.getEntityBundle()));
		dto.setTeamId(dbo.getTeamId() == null ? null : dbo.getTeamId().toString());
		dto.setDockerRepositoryName(dbo.getDockerRepositoryName());
		dto.setDockerDigest(dbo.getDockerDigest());
	}

	/**
	 * Convert a SubmissionStatusDBO database object to a Participant data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	public static SubmissionStatusDBO convertDtoToDbo(SubmissionStatus dto) {
		SubmissionStatusDBO dbo = new SubmissionStatusDBO();
		try {
			dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Submission ID: " + dto.getId());
		}
		dbo.seteTag(dto.getEtag());
		dbo.setModifiedOn(dto.getModifiedOn() == null ? null : dto.getModifiedOn().getTime());
		dbo.setScore(dto.getScore());
		dbo.setStatusEnum(dto.getStatus());
		dbo.setVersion(dto.getStatusVersion());
		
		// Serialize the annotations V2, note that this might be null if the annotations are empty
		String jsonAnnotations = AnnotationsV2Utils.toJSONStringForStorage(dto.getSubmissionAnnotations());
		
		dbo.setAnnotations(jsonAnnotations);
		
		copyToSerializedField(dto, dbo);
		
		return dbo;
	}
	
	

	/**
	 * Convert a SubmissionStatus data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	public static SubmissionStatus convertDboToDto(SubmissionStatusDBO dbo) throws DatastoreException {		
		// serialized entity is regarded as the "true" representation of the object
		SubmissionStatus dto = copyFromSerializedField(dbo);
		
		// use non-serialized eTag and modified date as the "true" values
		dto.setEtag(dbo.geteTag());
		dto.setModifiedOn(dbo.getModifiedOn() == null ? null : new Date(dbo.getModifiedOn()));
		
		// populate from secondary columns if necessary (to support legacy non-serialized objects)
		if (dto.getId() == null) 
			dto.setId(dbo.getId().toString());			
		if (dto.getScore() == null)
			dto.setScore(dbo.getScore());
		if (dto.getStatus() == null)
			dto.setStatus(dbo.getStatusEnum());
		dto.setStatusVersion(dbo.getVersion());

		Annotations annotations = AnnotationsV2Utils.fromJSONString(dbo.getAnnotations());
		
		if (annotations != null) {
			// Fill in the id and etag
			annotations.setId(dto.getId());
			annotations.setEtag(dto.getEtag());
		}
		
		dto.setSubmissionAnnotations(annotations);		
		
		return dto;
	}	
	
	/**
	 * Convert a SubmissionStatus data transfer object to a SubmissionDBO database object
	 * 
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	public static SubmissionContributor convertDboToDto(SubmissionContributorDBO dbo) throws DatastoreException {		
		SubmissionContributor dto = new SubmissionContributor();
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setPrincipalId(dbo.getPrincipalId()==null?null:dbo.getPrincipalId().toString());
		return dto;
	}	
	
	public static void copyToSerializedField(SubmissionStatus dto, SubmissionStatusDBO dbo) throws DatastoreException {
		// Clear the annotations before serialization since we store them in its own field in the DB
		Annotations currentAnnotations = dto.getSubmissionAnnotations();
		
		dto.setSubmissionAnnotations(null);
		
		try {
			dbo.setSerializedEntity(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
		// Put back the previous annotations
		dto.setSubmissionAnnotations(currentAnnotations);
	}
	
	public static SubmissionStatus copyFromSerializedField(SubmissionStatusDBO dbo) throws DatastoreException {
		try {
			return (SubmissionStatus) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getSerializedEntity());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
}
