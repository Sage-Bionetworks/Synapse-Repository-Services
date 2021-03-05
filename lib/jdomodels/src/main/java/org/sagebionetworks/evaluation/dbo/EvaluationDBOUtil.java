package org.sagebionetworks.evaluation.dbo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

public class EvaluationDBOUtil {

	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(SubmissionQuota.class).build();

	/**
	 * Copy a EvaluationDBO database object to a Evaluation data transfer object
	 * 
	 * @param dto
	 * @param dbo
	 */
	public static void copyDtoToDbo(Evaluation dto, EvaluationDBO dbo) {	
		try {
			dbo.setId(dto.getId() == null ? null : Long.parseLong(dto.getId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Evaluation ID: " + dto.getId());
		}
		dbo.seteTag(dto.getEtag());
		dbo.setName(dto.getName());
		dbo.setDescription(dto.getDescription() == null ? null : dto.getDescription().getBytes());
		try {
			dbo.setOwnerId(dto.getOwnerId() == null ? null : Long.parseLong(dto.getOwnerId()));
		} catch (NumberFormatException e) {
			throw new NumberFormatException("Invalid Owner ID: " + dto.getOwnerId());
		}
		dbo.setCreatedOn(dto.getCreatedOn() == null ? null : dto.getCreatedOn().getTime());
		dbo.setContentSource(KeyFactory.stringToKey(dto.getContentSource()));
		if (dto.getSubmissionInstructionsMessage() != null) {
			dbo.setSubmissionInstructionsMessage(dto.getSubmissionInstructionsMessage().getBytes());
		}
		if (dto.getSubmissionReceiptMessage() != null) {
			dbo.setSubmissionReceiptMessage(dto.getSubmissionReceiptMessage().getBytes());
		}
		if (dto.getQuota() != null) {
			try {
				SubmissionQuota quota = dto.getQuota();
				dbo.setQuota(JDOSecondaryPropertyUtils.compressObject(X_STREAM, quota));
				Long startTime = quota.getFirstRoundStart()==null ? null : quota.getFirstRoundStart().getTime();
				dbo.setStartTimestamp(startTime);
				dbo.setEndTimestamp(getEndTimeOrNull(startTime, quota.getRoundDurationMillis(), quota.getNumberOfRounds()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		verifyEvaluationDBO(dbo);
	}
	
	/**
	 * Ensure that a EvaluationDBO object has all required components
	 * 
	 * @param dbo
	 */
	private static void verifyEvaluationDBO(EvaluationDBO dbo) {
		ValidateArgument.required(dbo.getId(), "ID");
		ValidateArgument.required(dbo.getEtag(), "etag");
		ValidateArgument.required(dbo.getName(), "name");
		ValidateArgument.required(dbo.getOwnerId(), "ownerID");
		ValidateArgument.required(dbo.getCreatedOn(), "creation date");
		ValidateArgument.required(dbo.getContentSource(), "content source");
	}
	
	public static Long getEndTimeOrNull(Long startTime, Long roundDurationMillis, Long numberOfRounds) {
		if (startTime==null || roundDurationMillis==null || numberOfRounds==null) return null;
		return startTime+roundDurationMillis*numberOfRounds;
	}
	
	/**
	 * Copy a Evaluation data transfer object to a EvaluationDBO database object
	 *
	 * @param dbo
	 * @param dto
	 * @throws DatastoreException
	 */
	public static void copyDboToDto(EvaluationDBO dbo, Evaluation dto) throws DatastoreException {	
		dto.setId(dbo.getId() == null ? null : dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		dto.setName(dbo.getName());
		if (dbo.getDescription() != null) {
			try {
				dto.setDescription(new String(dbo.getDescription(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		} else {
			dto.setDescription(null);
		}
		dto.setOwnerId(dbo.getOwnerId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setContentSource(KeyFactory.keyToString(dbo.getContentSource()));
		if (dbo.getSubmissionInstructionsMessage() != null) {
			try {
				dto.setSubmissionInstructionsMessage(new String(dbo.getSubmissionInstructionsMessage(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}
		if (dbo.getSubmissionReceiptMessage() != null) {
			try {
				dto.setSubmissionReceiptMessage(new String(dbo.getSubmissionReceiptMessage(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new DatastoreException(e);
			}
		}
		if (dbo.getQuota() != null) {
			try {
				dto.setQuota((SubmissionQuota)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getQuota()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void copyDbosToDtos(List<EvaluationDBO> dbos, List<Evaluation> dtos) throws DatastoreException {
		for (EvaluationDBO dbo : dbos) {
			Evaluation dto = new Evaluation();
			copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
	}
}
