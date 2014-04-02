package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.util.Date;

import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.persistence.DBOQuizResponse;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class QuizResponseUtils {
	public static void copyDtoToDbo(QuizResponse dto, DBOQuizResponse dbo) {
		dbo.setId(dto.getId());
		if (dto.getCreatedBy()!=null) dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		if (dto.getCreatedOn()!=null) dbo.setCreatedOn(new Long(dto.getCreatedOn().getTime()));
		dbo.setPassed(dto.getPass());
		dbo.setQuizId(dto.getQuizId());
		dbo.setScore(dto.getScore());
		copyToSerializedField(dto, dbo);
	}
	
	public static QuizResponse copyDboToDto(DBOQuizResponse dbo) {
		QuizResponse dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId());
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setPass(dbo.getPassed());
		dto.setQuizId(dbo.getQuizId());
		dto.setScore(dbo.getScore());
;		return dto;
	}
	
	public static void copyToSerializedField(QuizResponse dto, DBOQuizResponse dbo) throws DatastoreException {
		try {
			dbo.setSerialized(JDOSecondaryPropertyUtils.compressObject(dto));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static QuizResponse copyFromSerializedField(DBOQuizResponse dbo) throws DatastoreException {
		try {
			return (QuizResponse)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerialized());
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}

}
