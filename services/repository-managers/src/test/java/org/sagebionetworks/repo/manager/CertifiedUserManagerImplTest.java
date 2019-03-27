package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.quiz.MultichoiceAnswer;
import org.sagebionetworks.repo.model.quiz.MultichoiceQuestion;
import org.sagebionetworks.repo.model.quiz.MultichoiceResponse;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Question;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.QuestionVariety;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizGenerator;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.quiz.ResponseCorrectness;
import org.sagebionetworks.repo.model.quiz.TextFieldQuestion;
import org.sagebionetworks.repo.model.quiz.TextFieldResponse;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class CertifiedUserManagerImplTest {
	
	private CertifiedUserManagerImpl certifiedUserManager;
	private AmazonS3Utility s3Utility;
	private GroupMembersDAO groupMembersDao;
	private QuizResponseDAO quizResponseDao;
	private TransactionalMessenger mockTransactionalMessenger;
	
	@Before
	public void setUp() throws Exception {
		s3Utility = Mockito.mock(AmazonS3Utility.class);
		groupMembersDao = Mockito.mock(GroupMembersDAO.class);
		quizResponseDao = Mockito.mock(QuizResponseDAO.class);
		mockTransactionalMessenger = Mockito.mock(TransactionalMessenger.class);
		certifiedUserManager = new CertifiedUserManagerImpl(
				 s3Utility,
				 groupMembersDao,
				 quizResponseDao,
				 mockTransactionalMessenger);
		
	}
	
	private static String getDefaultQuizGeneratorAsString() throws IOException {
		InputStream is = CertifiedUserManagerImplTest.class.getClassLoader().getResourceAsStream(CertifiedUserManagerImpl.QUESTIONNAIRE_PROPERTIES_FILE);
		try {
			return IOUtils.toString(is);
		} finally {
			is.close();
		}
	}

	private static QuizGenerator getDefaultQuizGenerator() throws JSONObjectAdapterException, IOException {
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
		List<String> errors = CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator);
		assertTrue(errors.toString(), errors.isEmpty());
	}

	@Test
	public void testValidateQuizGenerator() throws Exception {
		QuizGenerator quizGenerator = new QuizGenerator();
		quizGenerator.setId(999L);
		quizGenerator.setMinimumScore(1L);
		List<QuestionVariety> qvs = new ArrayList<QuestionVariety>();
		quizGenerator.setQuestions(qvs);
		// test missing question
		assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());  
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			questionOptions.add(generateQuestion(1L, "1"));
			questionOptions.add(generateQuestion(2L, "2"));
		}
		// happy case
		assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		quizGenerator.setMinimumScore(null); // can't be null
		assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		quizGenerator.setMinimumScore(2L); // can't be bigger than # qv's
		assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		
		// test missing ID
		quizGenerator.setMinimumScore(1L);
		assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		quizGenerator.setId(null);
		assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		
		quizGenerator.setId(999L);

		// test missing question index
		qvs.clear();
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			MultichoiceQuestion mq = new MultichoiceQuestion();
			questionOptions.add(mq);
			List<MultichoiceAnswer> mas = new ArrayList<MultichoiceAnswer>();
			mq.setAnswers(mas);
			mas.add(createMultichoiceAnswer(true, 10L));
			mq.setQuestionIndex(99L);
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).toString(), CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			mq.setQuestionIndex(null);
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		}
		
		// test repeated question index
		qvs.clear();
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			MultichoiceQuestion mq = new MultichoiceQuestion();
			questionOptions.add(mq);
			List<MultichoiceAnswer> mas = new ArrayList<MultichoiceAnswer>();
			mq.setAnswers(mas);
			mq.setQuestionIndex(99L);
			mas.add(createMultichoiceAnswer(true, 10L));
			
			mq = new MultichoiceQuestion();
			questionOptions.add(mq);
			mas = new ArrayList<MultichoiceAnswer>();
			mq.setAnswers(mas);
			mq.setQuestionIndex(100L);
			mas.add(createMultichoiceAnswer(true, 20L));
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			mq.setQuestionIndex(99L);
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		}
		
		// test multichoice answer missing index
		qvs.clear();
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			MultichoiceQuestion mq = new MultichoiceQuestion();
			questionOptions.add(mq);
			List<MultichoiceAnswer> mas = new ArrayList<MultichoiceAnswer>();
			mq.setAnswers(mas);
			mq.setQuestionIndex(99L);
			mas.add(createMultichoiceAnswer(true, 5L));
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			mas.clear();
			mas.add(createMultichoiceAnswer(true, null));
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		}
		
		// test multichoice answer repeating index
		qvs.clear();
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			MultichoiceQuestion mq = new MultichoiceQuestion();
			questionOptions.add(mq);
			List<MultichoiceAnswer> mas = new ArrayList<MultichoiceAnswer>();
			mq.setAnswers(mas);
			mq.setQuestionIndex(99L);
			mas.add(createMultichoiceAnswer(true, 10L));
			mas.add(createMultichoiceAnswer(false, 20L));
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			mas.clear();
			mas.add(createMultichoiceAnswer(true, 10L));
			mas.add(createMultichoiceAnswer(false, 10L));
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			
			// test multichoice missing answer
			mas.clear();
			mas.add(createMultichoiceAnswer(true, 10L));
			mas.add(createMultichoiceAnswer(false, 20L));
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			mas.clear();
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		}
		
		// test 'exclusive' multichoice with multiple correct answers
		qvs.clear();
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			MultichoiceQuestion mq = new MultichoiceQuestion();
			questionOptions.add(mq);
			List<MultichoiceAnswer> mas = new ArrayList<MultichoiceAnswer>();
			mq.setAnswers(mas);
			mq.setQuestionIndex(99L);
			mq.setExclusive(true);
			mas.add(createMultichoiceAnswer(true, 10L));
			mas.add(createMultichoiceAnswer(false, 20L));
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			mas.clear();
			mas.add(createMultichoiceAnswer(true, 10L));
			mas.add(createMultichoiceAnswer(true, 20L));
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		}
		
		// test text field question missing answer
		qvs.clear();
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			questionOptions.add(generateQuestion(10L, "foo"));
			assertTrue(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
			questionOptions.clear();
			questionOptions.add(generateQuestion(10L, null));
			assertFalse(CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator).isEmpty());
		}
	}
	
	@Test
	public void testRetrieveCertificationQuizGenerator() throws Exception {
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(false);
		QuizGenerator q = certifiedUserManager.retrieveCertificationQuizGenerator();
		assertNotNull(q); // we check the content elsewhere, so here we just check we get a result
		
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(true);
		when(s3Utility.downloadFromS3ToString(anyString())).thenReturn(getDefaultQuizGeneratorAsString());
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
		// test that the quiz answers have been scrubbed
		for (Question q : quiz.getQuestions()) {
			if (q instanceof MultichoiceQuestion) {
				MultichoiceQuestion mq = (MultichoiceQuestion)q;
				for (MultichoiceAnswer a : mq.getAnswers()) {
					assertNull(a.getIsCorrect());
				}
			} else if (q instanceof TextFieldQuestion) {
				TextFieldQuestion tq  = (TextFieldQuestion)q;
				assertNull(tq.getAnswer());
			}
		}
	}

	/*
	 * PLFM-3478
	 */
	@Test (expected=UnauthorizedException.class)
	public void testGetCertificationQuizUnauthorized() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		assertNotNull(certifiedUserManager.getCertificationQuiz(userInfo).getId());
	}

	@Test
	public void testGetCertificationQuiz() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(789L);
		assertNotNull(certifiedUserManager.getCertificationQuiz(userInfo).getId());
	}

	@Test
	public void testCloneAndScrubPrivateFields() throws Exception {
		MultichoiceQuestion mcq = new MultichoiceQuestion();
		mcq.setQuestionIndex(1L);
		mcq.setPrompt("foo");
		List<MultichoiceAnswer> mcas = new ArrayList<MultichoiceAnswer>();
		MultichoiceAnswer mca = new MultichoiceAnswer();
		mca.setAnswerIndex(1L);
		mca.setIsCorrect(true);
		mca.setPrompt("bar");
		mcas.add(mca);
		mcq.setAnswers(mcas);
		MultichoiceQuestion mcqClone = (MultichoiceQuestion)CertifiedUserManagerImpl.cloneAndScrubPrivateFields(mcq);
		assertEquals(mcq.getPrompt(), mcqClone.getPrompt());
		assertEquals(mcq.getQuestionIndex(), mcqClone.getQuestionIndex());
		assertNull(mcqClone.getAnswers().get(0).getIsCorrect());
		assertTrue(mcq.getAnswers().get(0).getIsCorrect());
		
		//now try a textfield question
		TextFieldQuestion tfq = new TextFieldQuestion();
		tfq.setQuestionIndex(1L);
		tfq.setPrompt("foo");
		tfq.setAnswer("bar");
		TextFieldQuestion tfqClone = (TextFieldQuestion)CertifiedUserManagerImpl.cloneAndScrubPrivateFields(tfq);
		assertEquals(tfq.getPrompt(), tfqClone.getPrompt());
		assertEquals(tfq.getQuestionIndex(), tfqClone.getQuestionIndex());
		assertNull(tfqClone.getAnswer());
		assertEquals("bar", tfq.getAnswer());
	}
		
	private static MultichoiceAnswer createMultichoiceAnswer(Boolean isCorrect, Long index) {
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
	
	@Test
	public void testPLFM2701() throws Exception {
		MultichoiceQuestion mq = new MultichoiceQuestion();
		mq.setAnswers(Collections.singletonList(createMultichoiceAnswer(null, 2L)));
		// throws a NPE in PLFM-2701
		CertifiedUserManagerImpl.isCorrectResponse(mq, createMultiChoiceResponse(new Long[]{1L}));
	}
	
	private static TextFieldQuestion generateQuestion(Long questionIndex, String correctAnswer) {
		TextFieldQuestion q = new TextFieldQuestion();
		q.setQuestionIndex(questionIndex);
		q.setAnswer(correctAnswer);
		return q;
	}
	
	private static QuizGenerator createQuizGenerator() {
		QuizGenerator gen = new QuizGenerator();
		gen.setId(999L);
		gen.setMinimumScore(2L); // you need to get two questions right, one from each 'variety'
		List<QuestionVariety> qvs = new ArrayList<QuestionVariety>();
		gen.setQuestions(qvs);
		// the first question variety, having two questions
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			questionOptions.add(generateQuestion(1L, "1"));
			questionOptions.add(generateQuestion(2L, "2"));
		}
		// the second question variety, having two questions
		{
			QuestionVariety qv = new QuestionVariety();
			qvs.add(qv);
			List<Question> questionOptions = new ArrayList<Question>();
			qv.setQuestionOptions(questionOptions);
			questionOptions.add(generateQuestion(3L, "3"));
			questionOptions.add(generateQuestion(4L, "4"));
		}
		return gen;
	}
	
	private static QuizResponse createPassingQuizResponse(long quizId) {
		QuizResponse resp = new QuizResponse();
		resp.setQuizId(quizId);
		List<QuestionResponse> questionResponses = new ArrayList<QuestionResponse>();
		resp.setQuestionResponses(questionResponses);
		{
			TextFieldResponse tr = new TextFieldResponse();
			questionResponses.add(tr);
			tr.setQuestionIndex(1L);
			tr.setResponse("1");
		}
		{
			TextFieldResponse tr = new TextFieldResponse();
			questionResponses.add(tr);
			tr.setQuestionIndex(3L);
			tr.setResponse("3");
		}
		return resp;
	}
	
	@Test
	public void testScoreQuizResponseHappyCase() throws Exception {
		QuizGenerator gen = createQuizGenerator();
		QuizResponse resp = createPassingQuizResponse(gen.getId());
		PassingRecord passingRecord = CertifiedUserManagerImpl.scoreQuizResponse(gen, resp);
		assertTrue(passingRecord.getPassed());
		assertEquals(new Long(2L), passingRecord.getScore());
		// make sure all the responses are in the 'passing record'
		assertEquals(resp.getQuestionResponses().size(), passingRecord.getCorrections().size());
		for (ResponseCorrectness rc : passingRecord.getCorrections()) {
			assertTrue(resp.getQuestionResponses().contains(rc.getResponse()));
			assertTrue(rc.getIsCorrect());
			assertNotNull(rc.getQuestion());
			if (rc.getQuestion() instanceof MultichoiceQuestion) {
				MultichoiceQuestion mcq = (MultichoiceQuestion)rc.getQuestion();
				for (MultichoiceAnswer ma : mcq.getAnswers()) {
					assertNull(ma.getIsCorrect());
				}
			} else if (rc.getQuestion() instanceof TextFieldQuestion) {
				TextFieldQuestion tfq = (TextFieldQuestion)rc.getQuestion();
				assertNull(tfq.getAnswer());
			} else {
				fail("Unknown type "+rc.getQuestion().getClass());
			}
		}
	}
	
	@Test
	public void testPLFM3080() throws Exception {
		QuizGenerator quizGenerator = getDefaultQuizGenerator();
		// this should not affect the QuizGenerator (but the fact that it DID caused PLFM-3080)
		CertifiedUserManagerImpl.selectQuiz(quizGenerator);
		// this should return no errors
		List<String> errorMessages = CertifiedUserManagerImpl.validateQuizGenerator(quizGenerator);
		assertTrue(errorMessages.toString(), errorMessages.isEmpty());
	}

	private static QuizResponse createFailingQuizResponse(long quizId) {
		QuizResponse resp = new QuizResponse();
		resp.setQuizId(quizId);
		List<QuestionResponse> questionResponses = new ArrayList<QuestionResponse>();
		resp.setQuestionResponses(questionResponses);
		{
			TextFieldResponse tr = new TextFieldResponse();
			questionResponses.add(tr);
			tr.setQuestionIndex(1L);
			tr.setResponse("1");
		}
		{
			TextFieldResponse tr = new TextFieldResponse();
			questionResponses.add(tr);
			tr.setQuestionIndex(3L);
			tr.setResponse("99");
		}
		return resp;
	}
	
	@Test
	public void testScoreQuizResponseWrongAnswer() throws Exception {
		QuizGenerator gen = createQuizGenerator();
		QuizResponse resp = createFailingQuizResponse(gen.getId());
		PassingRecord passingRecord = CertifiedUserManagerImpl.scoreQuizResponse(gen, resp);
		assertFalse(passingRecord.getPassed());
		assertEquals(new Long(1L), passingRecord.getScore());
		// make sure all the responses are in the 'passing record'
		assertEquals(resp.getQuestionResponses().size(), passingRecord.getCorrections().size());
		for (ResponseCorrectness rc : passingRecord.getCorrections()) {
			assertTrue(resp.getQuestionResponses().contains(rc.getResponse()));
			Long qi = rc.getResponse().getQuestionIndex();
			if (qi==1L) {
				assertTrue(rc.getIsCorrect());
			} else if (qi==3L) {
				assertFalse(rc.getIsCorrect());
			} else {
				fail("Unexpected question index "+qi);
			}
			assertNotNull(rc.getQuestion());
			if (rc.getQuestion() instanceof MultichoiceQuestion) {
				MultichoiceQuestion mcq = (MultichoiceQuestion)rc.getQuestion();
				for (MultichoiceAnswer ma : mcq.getAnswers()) {
					assertNull(ma.getIsCorrect());
				}
			} else if (rc.getQuestion() instanceof TextFieldQuestion) {
				TextFieldQuestion tfq = (TextFieldQuestion)rc.getQuestion();
				assertNull(tfq.getAnswer());
			} else {
				fail("Unknown type "+rc.getQuestion().getClass());
			}
		}
	}
	
	private static QuizResponse createIllegalQuizResponse(long quizId) {
		QuizResponse resp = new QuizResponse();
		resp.setQuizId(quizId);
		List<QuestionResponse> questionResponses = new ArrayList<QuestionResponse>();
		resp.setQuestionResponses(questionResponses);
		{
			TextFieldResponse tr = new TextFieldResponse();
			questionResponses.add(tr);
			tr.setQuestionIndex(1L);
			tr.setResponse("1");
		}
		{
			TextFieldResponse tr = new TextFieldResponse();
			questionResponses.add(tr);
			tr.setQuestionIndex(2L);
			tr.setResponse("2");
		}
		return resp;
	}

	@Test(expected=IllegalArgumentException.class)
	public void testScoreQuizResponseWrongQuestions() throws Exception {
		QuizGenerator gen = createQuizGenerator();
		// can't answer two questions from one variety.  
		// this will throw an IllegalArgumentException
		QuizResponse resp = createIllegalQuizResponse(gen.getId());
		CertifiedUserManagerImpl.scoreQuizResponse(gen, resp);
	}

	/*
	 * PLFM-3478
	 */
	@Test (expected=UnauthorizedException.class)
	public void testSubmitCertificationQuizUnauthorized() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		QuizGenerator quizGenerator = createQuizGenerator();
		QuizResponse quizResponse = createPassingQuizResponse(quizGenerator.getId());
		certifiedUserManager.submitCertificationQuizResponse(userInfo, quizResponse);
	}

	@Test
	public void testSubmitCertificationQuizPASSINGResponse() throws Exception {
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(true);
		QuizGenerator quizGenerator = createQuizGenerator();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		quizGenerator.writeToJSONObject(adapter);
		String quizGeneratorAsString = adapter.toJSONString();
		when(s3Utility.downloadFromS3ToString(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(quizGeneratorAsString);
		QuizResponse quizResponse = createPassingQuizResponse(quizGenerator.getId());
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(666L);
		QuizResponse created = createPassingQuizResponse(quizGenerator.getId());
		created.setCreatedBy(userInfo.getId().toString());
		created.setCreatedOn(new Date());
		created.setId(10101L);
		ArgumentCaptor<PassingRecord> captor = ArgumentCaptor.forClass(PassingRecord.class);
		when(quizResponseDao.create(eq(quizResponse), captor.capture())).thenReturn(created);
		when(quizResponseDao.getPassingRecord(quizGenerator.getId(), 666L)).thenThrow(new NotFoundException());
		PassingRecord pr = certifiedUserManager.submitCertificationQuizResponse(userInfo, quizResponse);
		// check that 5 fields are filled in quizResponse
		assertEquals(userInfo.getId().toString(), quizResponse.getCreatedBy());
		PassingRecord passingRecord = captor.getValue();
		assertEquals(true, passingRecord.getPassed());
		assertEquals(quizGenerator.getId(), quizResponse.getQuizId());
		assertEquals(2L, passingRecord.getScore().longValue());
		assertNotNull(quizResponse.getCreatedOn());
		verify(quizResponseDao).create(eq(quizResponse), captor.capture());
		verify(groupMembersDao).addMembers(anyString(), (List<String>)any());
		assertEquals(passingRecord.getPassed(), pr.getPassed());
		assertNotNull(pr.getPassedOn());
		assertEquals(created.getQuizId(), pr.getQuizId());
		assertEquals(created.getId(), pr.getResponseId());
		assertEquals(passingRecord.getScore(), pr.getScore());
		assertEquals(created.getCreatedBy(), pr.getUserId());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(userInfo.getId().toString(), ObjectType.CERTIFIED_USER_PASSING_RECORD, "etag", ChangeType.CREATE);
	}
	
	@Test
	public void testSubmitCertificationQuizFAILINGResponse() throws Exception {
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(true);
		QuizGenerator quizGenerator = createQuizGenerator();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		quizGenerator.writeToJSONObject(adapter);
		String quizGeneratorAsString = adapter.toJSONString();
		when(s3Utility.downloadFromS3ToString(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(quizGeneratorAsString);
		QuizResponse quizResponse = createFailingQuizResponse(quizGenerator.getId());
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(666L);
		QuizResponse created = createFailingQuizResponse(quizGenerator.getId());
		created.setCreatedBy(userInfo.getId().toString());
		created.setCreatedOn(new Date());
		created.setId(10101L);
		ArgumentCaptor<PassingRecord> captor = ArgumentCaptor.forClass(PassingRecord.class);
		when(quizResponseDao.create(eq(quizResponse), captor.capture())).thenReturn(created);
		when(quizResponseDao.getPassingRecord(quizGenerator.getId(), 666L)).thenThrow(new NotFoundException());
		PassingRecord pr = certifiedUserManager.submitCertificationQuizResponse(userInfo, quizResponse);
		verify(quizResponseDao).create(eq(quizResponse), captor.capture());
		PassingRecord passingRecord = captor.getValue();
		// check that 5 fields are filled in quizResponse
		assertEquals(userInfo.getId().toString(), quizResponse.getCreatedBy());
		assertEquals(false, passingRecord.getPassed());
		assertEquals(quizGenerator.getId(), quizResponse.getQuizId());
		assertEquals(1L, passingRecord.getScore().longValue());
		assertNotNull(quizResponse.getCreatedOn());
		verify(groupMembersDao, never()).addMembers(anyString(), (List<String>)any());
		assertEquals(passingRecord.getPassed(), pr.getPassed());
		assertNotNull(pr.getPassedOn());
		assertEquals(created.getQuizId(), pr.getQuizId());
		assertEquals(created.getId(), pr.getResponseId());
		assertEquals(passingRecord.getScore(), pr.getScore());
		assertEquals(created.getCreatedBy(), pr.getUserId());
		verify(mockTransactionalMessenger).sendMessageAfterCommit(userInfo.getId().toString(), ObjectType.CERTIFIED_USER_PASSING_RECORD, "etag", ChangeType.CREATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testSubmitCertificationQuizAlreadyPassed() throws Exception {
		when(s3Utility.doesExist(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(true);
		QuizGenerator quizGenerator = createQuizGenerator();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		quizGenerator.writeToJSONObject(adapter);
		String quizGeneratorAsString = adapter.toJSONString();
		when(s3Utility.downloadFromS3ToString(CertifiedUserManagerImpl.S3_QUESTIONNAIRE_KEY)).thenReturn(quizGeneratorAsString);
		QuizResponse quizResponse = createPassingQuizResponse(quizGenerator.getId());
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(666L);
		QuizResponse created = createPassingQuizResponse(quizGenerator.getId());
		created.setCreatedBy(userInfo.getId().toString());
		created.setCreatedOn(new Date());
		created.setId(10101L);
		ArgumentCaptor<PassingRecord> captor = ArgumentCaptor.forClass(PassingRecord.class);
		when(quizResponseDao.create(eq(quizResponse), captor.capture())).thenReturn(created);
		PassingRecord previousPR = new PassingRecord();
		previousPR.setPassed(true);
		when(quizResponseDao.getPassingRecord(quizGenerator.getId(), 666L)).thenReturn(previousPR);
		PassingRecord pr = certifiedUserManager.submitCertificationQuizResponse(userInfo, quizResponse);
	}
	
	@Test
	public void testGetQuizResponses() throws Exception {
		UserInfo userInfo = new UserInfo(true);
		certifiedUserManager.getQuizResponses(userInfo, null, 3L, 10L);
		Long quizId = getDefaultQuizGenerator().getId();
		verify(quizResponseDao).getAllResponsesForQuiz(quizId, 3L, 10L);
		verify(quizResponseDao).getAllResponsesForQuizCount(quizId);
	}

	@Test
	public void testGetQuizResponsesForAUser() throws Exception {
		UserInfo userInfo = new UserInfo(true);
		Long userId = 666L;
		certifiedUserManager.getQuizResponses(userInfo, userId, 3L, 10L);
		Long quizId = getDefaultQuizGenerator().getId();
		verify(quizResponseDao).getUserResponsesForQuiz(quizId, userId, 3L, 10L);
		verify(quizResponseDao).getUserResponsesForQuizCount(quizId, userId);
	}

	@Test(expected=ForbiddenException.class)
	public void testGetQuizResponsesNonAdmin() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		certifiedUserManager.getQuizResponses(userInfo, 101L, 3L, 10L);
	}

	@Test
	public void testDeleteQuizResponseAdmin() throws Exception {
		UserInfo userInfo = new UserInfo(true);
		certifiedUserManager.deleteQuizResponse(userInfo, 101L);
		verify(quizResponseDao).delete(101L);
	}
	
	@Test(expected=ForbiddenException.class)
	public void testDeleteQuizResponseNonAdmin() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		certifiedUserManager.deleteQuizResponse(userInfo, 101L);
	}
	
	@Test
	public void testGetPassingRecord() throws Exception {
		certifiedUserManager.getPassingRecord(101L);
		verify(quizResponseDao).getPassingRecord(anyLong(), eq(101L));
	}

	@Test(expected=ForbiddenException.class)
	public void testGetPassingRecordsNonAdmin() throws Exception {
		UserInfo userInfo = new UserInfo(false);
		certifiedUserManager.getPassingRecords(userInfo, 101L, 10L, 0L);
	}

	@Test
	public void testGetPassingRecordsAdmin() throws Exception {
		UserInfo userInfo = new UserInfo(true);
		certifiedUserManager.getPassingRecords(userInfo, 101L, 10L, 0L);
		verify(quizResponseDao).getAllPassingRecords(anyLong(), eq(101L), eq(10L), eq(0L));
		verify(quizResponseDao).getAllPassingRecordsCount(anyLong(), eq(101L));
	}
}
