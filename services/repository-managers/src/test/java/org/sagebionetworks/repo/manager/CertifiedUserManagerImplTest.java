package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.quiz.MultichoiceAnswer;
import org.sagebionetworks.repo.model.quiz.MultichoiceQuestion;
import org.sagebionetworks.repo.model.quiz.MultichoiceResponse;
import org.sagebionetworks.repo.model.quiz.Question;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.QuestionVariety;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizGenerator;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.quiz.TextFieldQuestion;
import org.sagebionetworks.repo.model.quiz.TextFieldResponse;
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
	
	private static MultichoiceAnswer createMultichoiceAnswer(boolean isCorrect, Long index) {
		MultichoiceAnswer ma = new MultichoiceAnswer();
		ma.setAnswerIndex(index);
		ma.setIsCorrect(isCorrect);
		return ma;
	}
	
	private static MultichoiceResponse createMultiChoiceResponse(Long[] choices) {
		MultichoiceResponse mr = new MultichoiceResponse();
		mr.setAnswerIndex(new HashSet<Long>(Arrays.asList(choices)));
		return mr;
	}
	
	@Test
	public void testIsCorrectResponse() throws Exception {
		MultichoiceQuestion mq = new MultichoiceQuestion();
		List<MultichoiceAnswer> mas = new ArrayList<MultichoiceAnswer>();
		mq.setAnswers(mas);
		mas.add(createMultichoiceAnswer(true, 1L));
		mas.add(createMultichoiceAnswer(false, 2L));
		// the correct answer is 1
		assertTrue(CertifiedUserManagerImpl.isCorrectResponse(mq, createMultiChoiceResponse(new Long[]{1L})));
		// 2 is wrong
		assertFalse(CertifiedUserManagerImpl.isCorrectResponse(mq, createMultiChoiceResponse(new Long[]{2L})));
		mas = new ArrayList<MultichoiceAnswer>();
		mq.setAnswers(mas);
		mas.add(createMultichoiceAnswer(true, 1L));
		mas.add(createMultichoiceAnswer(true, 2L));
		mas.add(createMultichoiceAnswer(false, 3L));
		// if there are two right answers you have to get both
		assertFalse(CertifiedUserManagerImpl.isCorrectResponse(mq, createMultiChoiceResponse(new Long[]{1L})));
		assertTrue(CertifiedUserManagerImpl.isCorrectResponse(mq, createMultiChoiceResponse(new Long[]{1L, 2L})));
		
		TextFieldQuestion tq = new TextFieldQuestion();
		tq.setAnswer("FOO");
		TextFieldResponse tr = new TextFieldResponse();
		tr.setResponse("foo");
		// question and answer must match, but not necessarily in case
		assertTrue(CertifiedUserManagerImpl.isCorrectResponse(tq, tr));
		tr = new TextFieldResponse();
		tr.setResponse("bar");
		assertFalse(CertifiedUserManagerImpl.isCorrectResponse(tq, tr));
			
		try {
			CertifiedUserManagerImpl.isCorrectResponse(mq, tr);
			fail("Exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		try {
			CertifiedUserManagerImpl.isCorrectResponse(tq, createMultiChoiceResponse(new Long[]{1L}));
			fail("Exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	private static QuizGenerator createQuizGenerator() {
		QuizGenerator gen = new QuizGenerator();
		gen.setMinimumScore(1L);
		List<QuestionVariety> qvs = new ArrayList<QuestionVariety>();
		gen.setQuestions(qvs);
		QuestionVariety qv = new QuestionVariety();
		qvs.add(qv);
		List<Question> questionOptions = new ArrayList<Question>();
		qv.setQuestionOptions(questionOptions);
		TextFieldQuestion q = new TextFieldQuestion();
		questionOptions.add(q);
		q.setQuestionIndex(1L);
		q.setAnswer("1");
		return gen;
	}
	
	@Test
	public void testScoreQuizResponse() throws Exception {
		QuizGenerator gen = createQuizGenerator();
		QuizResponse resp = new QuizResponse();
		List<QuestionResponse> questionResponses = new ArrayList<QuestionResponse>();
		resp.setQuestionResponses(questionResponses);
		TextFieldResponse tr = new TextFieldResponse();
		questionResponses.add(tr);
		tr.setQuestionIndex(1L);
		tr.setResponse("1");
		CertifiedUserManagerImpl.scoreQuizResponse(gen, resp);
		assertTrue(resp.getPass());
		assertEquals(new Long(1L), resp.getScore());
	}

}
