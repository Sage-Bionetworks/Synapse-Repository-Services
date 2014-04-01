package org.sagebionetworks.repo.manager;

import java.io.InputStream;

import org.junit.Test;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class CertifiedUserManagerImplTest {

	@Test
	public void testDefaultQuiz() throws Exception {
		InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter = adapter.createNew(IOTestUtil.readFromInputStream(is, "utf-8"));
		Quiz quiz = new Quiz();
		// if the resource file does not contain a valid Quiz, this will fail
		quiz.initializeFromJSONObject(adapter);
		// do additional validation
		CertifiedUserManagerImpl.validateQuiz(quiz);
	}


}
