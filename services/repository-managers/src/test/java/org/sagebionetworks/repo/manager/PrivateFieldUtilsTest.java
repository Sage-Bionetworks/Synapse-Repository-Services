package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.questionnaire.MultichoiceAnswer;
import org.sagebionetworks.repo.model.questionnaire.MultichoiceQuestion;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class PrivateFieldUtilsTest {

	@Test
	public void testUserProfile() {
		UserProfile up = new UserProfile();
		up.setEmails(Collections.singletonList("foo@bar.com"));
		up.setLastName("Bar");
		up.setUserName("foobar");
		PrivateFieldUtils.clearPrivateFields(up);
		assertNull(up.getEmails());
		assertNotNull(up.getLastName());
		assertNotNull(up.getUserName());
	}
	
	@Test
	public void testQuestionnaire() throws Exception {
		InputStream is = PrivateFieldUtilsTest.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter = adapter.createNew(IOTestUtil.readFromInputStream(is, "utf-8"));
		Questionnaire questionnaire = new Questionnaire();
		// if the resource file does not contain a valid Questionnaire, this will fail
		questionnaire.initializeFromJSONObject(adapter);
		
		assertNotNull(questionnaire.getMinimumScore());
		List<MultichoiceAnswer> answers = ((MultichoiceQuestion)questionnaire.getQuestions().get(0).getQuestionOptions().get(0)).getAnswers();
		
		// at least one of the answers is correct
		boolean foundCorrect = false;
		for (MultichoiceAnswer a : answers) {
			if (a.getIsCorrect()!=null && a.getIsCorrect()) foundCorrect=true;
		}
		assertTrue(foundCorrect);
		
		PrivateFieldUtils.clearPrivateFields(questionnaire);
		
		// check that the arrays are still in place
		answers = ((MultichoiceQuestion)questionnaire.getQuestions().get(0).getQuestionOptions().get(0)).getAnswers();

		// now the min score field is cleared
		assertNull(questionnaire.getMinimumScore());
		
		// but other fields are not scrubbed
		assertNotNull(questionnaire.getId());

		for (MultichoiceAnswer a : answers) {
			// and the correct answer field is cleared
			assertNull(a.getIsCorrect());
			// but the others remain
			assertNotNull(a.getPrompt());
			assertNotNull(a.getAnswerIndex());
		}
		


	}

}
