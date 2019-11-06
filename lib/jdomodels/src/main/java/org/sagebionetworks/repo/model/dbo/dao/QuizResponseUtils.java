package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.persistence.DBOQuizResponse;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuizResponse;

public class QuizResponseUtils {
	private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(QuizResponse.class, PassingRecord.class).build();


	public static void copyDtoToDbo(QuizResponse dto, PassingRecord passingRecord, DBOQuizResponse dbo) {
		dbo.setId(dto.getId());
		if (dto.getCreatedBy()!=null) dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		if (dto.getCreatedOn()!=null) dbo.setCreatedOn(new Long(dto.getCreatedOn().getTime()));
		dbo.setQuizId(dto.getQuizId());
		dbo.setPassed(passingRecord.getPassed());
		dbo.setScore(passingRecord.getScore());
		try {
			dbo.setPassingRecord(JDOSecondaryPropertyUtils.compressObject(X_STREAM, passingRecord));
			dbo.setSerialized(JDOSecondaryPropertyUtils.compressObject(X_STREAM, dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static QuizResponse copyDboToDto(DBOQuizResponse dbo) {
		QuizResponse dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setQuizId(dbo.getQuizId());
		return dto;
	}

	
	public static QuizResponse copyFromSerializedField(DBOQuizResponse dbo) throws DatastoreException {
		try {
			return (QuizResponse)JDOSecondaryPropertyUtils.decompressObject(X_STREAM, dbo.getSerialized());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

}
