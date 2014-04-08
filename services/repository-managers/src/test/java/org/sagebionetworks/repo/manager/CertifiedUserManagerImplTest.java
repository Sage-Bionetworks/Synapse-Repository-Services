package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.quiz.Question;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.QuestionVariety;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizGenerator;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class CertifiedUserManagerImplTest {
	
	private CertifiedUserManagerImpl certifiedUserManager;
	private AmazonS3Utility s3Utility;
	private GroupMembersDAO groupMembersDao;
	private QuizResponseDAO quizResponseDao;
	
	@Before
	public void setUp() throws Exception {
		s3Utility = Mockito.mock(AmazonS3Utility.class);
		groupMembersDao = Mockito.mock(GroupMembersDAO.class);
		quizResponseDao = Mockito.mock(QuizResponseDAO.class);
		certifiedUserManager = new CertifiedUserManagerImpl(
				 s3Utility,
				 groupMembersDao,
				 quizResponseDao);
		
	}
	
	private static String getDefaultQuizGeneratorAsString() {
		InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);
		return IOTestUtil.readFromInputStream(is, "utf-8");
	}

	private static QuizGenerator getDefaultQuizGenerator() throws JSONObjectAdapterException {
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter = adapter.createNew(getDefaultQuizGeneratorAsString());
		QuizGenerator quizGenerator = new QuizGenerator();
		// if the resource file does not contain a valid Quiz, this will fail
		quizGenerator.initializeFromJSONObject(adapter);
		return quizGenerator;
	}

	@Test
	public void testDefaultQuiz() throws Exception {
		QuizGenerator quizGenerator = getDefaultQuizGenerator();
		// do additional validation
		CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator);
	}

	@Test
	public void testValidateQuizGenerator() throws Exception {
		QuizGenerator quizGenerator = new QuizGenerator();
		// TODO
	}
	
	@Test
	public void testRetrieveCertificationQuizGenerator() throws Exception {
		certifiedUserManager.expireQuizGeneratorCache();
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(false);
		QuizGenerator q = certifiedUserManager.retrieveCertificationQuizGenerator();
		assertNotNull(q); // we check the content elsewhere, so here we just check we get a result
		
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(false);
		when(s3Utility.downloadFromS3ToString(anyString())).thenReturn(getDefaultQuizGeneratorAsString());
		certifiedUserManager.expireQuizGeneratorCache();
		assertEquals(getDefaultQuizGenerator(), certifiedUserManager.retrieveCertificationQuizGenerator());
	}
	
	@Test
	public void testSelectQuiz() throws Exception {
		QuizGenerator quizGenerator = getDefaultQuizGenerator();
		Quiz quiz = CertifiedUserManagerImpl.selectQuiz(getDefaultQuizGenerator());
		// test that header and ID have been copied over
		assertEquals(quizGenerator.getHeader(), quiz.getHeader());
		assertEquals(quizGenerator.getId(), quiz.getId());
		// test that we have one question for each question variety
		assertEquals(quizGenerator.getQuestions().size(), quiz.getQuestions().size());
		// test that each question variety is the source of one question
		for (QuestionVariety vs : quizGenerator.getQuestions()) {
			Set<Long> questionIds = new HashSet<Long>();
			for (Question q : vs.getQuestionOptions()) questionIds.add(q.getQuestionIndex());
			assertTrue(vs.getQuestionOptions().size()>0);
			boolean foundOne = false;
			for (Question q : quiz.getQuestions()) {
				if (questionIds.contains(q.getQuestionIndex())) {
					foundOne=true;
					break;
				}
			}
			assertTrue("None of "+quiz.getQuestions().size()+
					" questions came from this variety of size "+vs.getQuestionOptions().size(), foundOne);
		}
	}
	
	@Test
	public void testGetCertificationQuiz() throws Exception {
		// the internal logic is tested elsewhere.  Here we just check 
		// that some quiz is returned and it has an ID
		assertNotNull(certifiedUserManager.getCertificationQuiz().getId());
	}
	
	@Test
	public void testIsCorrectResponse() throws Exception {
		Question q;
		QuestionResponse response;
		// TODO assertTrue(isCorrectResponse(Question q, QuestionResponse response));
	}

}
