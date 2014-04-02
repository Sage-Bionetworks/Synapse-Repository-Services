package org.sagebionetworks.repo.manager;

import java.io.InputStream;

import org.junit.Test;
import org.sagebionetworks.repo.model.quiz.QuizGenerator;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class CertifiedUserManagerImplTest {

	@Test
	public void testDefaultQuiz() throws Exception {
		InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter = adapter.createNew(IOTestUtil.readFromInputStream(is, "utf-8"));
		QuizGenerator quizGenerator = new QuizGenerator();
		// if the resource file does not contain a valid Quiz, this will fail
		quizGenerator.initializeFromJSONObject(adapter);
		// do additional validation
		CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator);
	}

	@Test
	public void testValidateQuizGenerator() throws Exception {
		// TODO
	}

}
