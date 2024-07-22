package org.sagebionetworks.repo.model.dbo.dao;

import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOQuizResponse;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;

public class QuizResponseUtils {

	public static void copyDtoToDbo(QuizResponse dto, PassingRecord passingRecord, DBOQuizResponse dbo) {
		dbo.setId(dto.getId());

		if (dto.getCreatedBy() != null) {
			dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		}

		if (dto.getCreatedOn() != null) {
			dbo.setCreatedOn(dto.getCreatedOn().getTime());
		}

		dbo.setQuizId(dto.getQuizId());
		dbo.setPassed(passingRecord.getPassed());
		dbo.setScore(passingRecord.getScore());

		dbo.setResponseJson(JDOSecondaryPropertyUtils.createJSONFromObject(dto));
		dbo.setPassingJson(JDOSecondaryPropertyUtils.createJSONFromObject(passingRecord));

	}

	public static QuizResponse copyDboToDto(DBOQuizResponse dbo) {
		QuizResponse dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setQuizId(dbo.getQuizId());
		return dto;
	}

	public static PassingRecord extractPassingRecord(DBOQuizResponse dbo) {
		PassingRecord passingRecord = JDOSecondaryPropertyUtils.createObejctFromJSON(PassingRecord.class,
				dbo.getPassingJson());
		passingRecord.setResponseId(dbo.getId());

		if (dbo.getRevokedOn() != null) {
			passingRecord.setRevokedOn(new Date(dbo.getRevokedOn()));
			passingRecord.setRevoked(true);
		} else {
			passingRecord.setRevoked(false);
		}

		if (passingRecord.getPassed() && !passingRecord.getRevoked()) {
			passingRecord.setCertified(true);
		} else {
			passingRecord.setCertified(false);
		}

		return passingRecord;
	}

	public static QuizResponse copyFromSerializedField(DBOQuizResponse dbo) throws DatastoreException {
		return JDOSecondaryPropertyUtils.createObejctFromJSON(QuizResponse.class, dbo.getResponseJson());
	}
}
