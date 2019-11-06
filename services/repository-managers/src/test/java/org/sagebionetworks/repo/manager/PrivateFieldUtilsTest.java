package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.quiz.MultichoiceAnswer;
import org.sagebionetworks.repo.model.quiz.MultichoiceQuestion;
import org.sagebionetworks.repo.model.quiz.QuizGenerator;
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
	public void testQuiz() throws Exception {
		InputStream is = PrivateFieldUtilsTest.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		try {
			String s = IOUtils.toString(is);
			adapter = adapter.createNew(s);
		} finally {
			is.close();
		}
		QuizGenerator quizGenerator = new QuizGenerator();
		// if the resource file does not contain a valid Quiz, this will fail
		quizGenerator.initializeFromJSONObject(adapter);
		
		assertNotNull(quizGenerator.getMinimumScore());
		List<MultichoiceAnswer> answers = ((MultichoiceQuestion)quizGenerator.getQuestions().get(0).getQuestionOptions().get(0)).getAnswers();
		
		// at least one of the answers is correct
		boolean foundCorrect = false;
		for (MultichoiceAnswer a : answers) {
			if (a.getIsCorrect()!=null && a.getIsCorrect()) foundCorrect=true;
		}
		assertTrue(foundCorrect);
		
		PrivateFieldUtils.clearPrivateFields(quizGenerator);
		
		// check that the arrays are still in place
		answers = ((MultichoiceQuestion)quizGenerator.getQuestions().get(0).getQuestionOptions().get(0)).getAnswers();

		// now the min score field is cleared
		assertNull(quizGenerator.getMinimumScore());
		
		// but other fields are not scrubbed
		assertNotNull(quizGenerator.getId());

		for (MultichoiceAnswer a : answers) {
			// and the correct answer field is cleared
			assertNull(a.getIsCorrect());
			// but the others remain
			assertNotNull(a.getPrompt());
			assertNotNull(a.getAnswerIndex());
		}
	}
	
	@Test
	public void testNoScrubReference() throws Exception {
		MultichoiceQuestion q = new MultichoiceQuestion();
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId("101");
		key.setOwnerObjectType(ObjectType.ENTITY);
		key.setWikiPageId("102");
		q.setReference(key);
		PrivateFieldUtils.clearPrivateFields(q);
		assertNotNull(q.getReference());
		assertNotNull(q.getReference().getOwnerObjectId());
		assertNotNull(q.getReference().getOwnerObjectType());
		assertNotNull(q.getReference().getWikiPageId());		
	}

}
