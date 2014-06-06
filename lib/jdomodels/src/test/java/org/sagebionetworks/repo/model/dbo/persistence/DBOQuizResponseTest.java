package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.quiz.QuizResponse;

public class DBOQuizResponseTest {

	@Test
	public void testTranslator() throws Exception {
		DBOQuizResponse backup = new DBOQuizResponse();
		// set some fields (don't need to set all)
		backup.setId(101L);
		backup.setPassed(true);
		backup.setQuizId(202L);
		backup.setScore(100L);
		QuizResponse dto = new QuizResponse();
		dto.setId(101L);
		dto.setQuizId(202L);
		byte[] serialized = JDOSecondaryPropertyUtils.compressObject(dto);
		backup.setSerialized(serialized);
		
		DBOQuizResponse dbo = backup.getTranslator().createDatabaseObjectFromBackup(backup);
		
		assertEquals(backup, dbo);
		QuizResponse deserialized = (QuizResponse)JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerialized());
		assertEquals(dto, deserialized);
	}

}
