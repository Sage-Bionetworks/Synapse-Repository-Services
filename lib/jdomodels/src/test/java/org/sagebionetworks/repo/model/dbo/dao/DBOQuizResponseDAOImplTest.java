package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.quiz.MultichoiceResponse;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOQuizResponseDAOImplTest {
	
	@Autowired
	private QuizResponseDAO quizResponseDao;
	
	private static QuizResponse createDTO(String principalId, Long quizId) {
		QuizResponse dto = new QuizResponse();
		dto.setCreatedBy(principalId);
		dto.setCreatedOn(new Date());
		List<QuestionResponse> questionResponses = new ArrayList<QuestionResponse>();
		MultichoiceResponse response = new MultichoiceResponse();
		response.setQuestionIndex(101L);
		response.setAnswerIndex(new HashSet<Long>(Collections.singletonList(999L)));
		questionResponses.add(response);
		dto.setQuestionResponses(questionResponses);
		dto.setQuizId(quizId);
		return dto;
	}
	
	private static PassingRecord createPassingRecord(String principalId, Long quizId, Long responseId, boolean pass, long score) {
		PassingRecord pr = new PassingRecord();
		pr.setPassed(pass);
		pr.setScore(score);
		pr.setPassedOn(new Date());
		pr.setQuizId(quizId);
		pr.setResponseId(responseId);
		pr.setUserId(principalId);
		return pr;
	}
	
	private QuizResponse storeDTO(QuizResponse dto, PassingRecord passingRecord) {
		QuizResponse created = quizResponseDao.create(dto, passingRecord);
		assertNotNull(created.getId());
		toDelete.add(created.getId());
		return created;
	}
	
	private QuizResponse createDTOAndStore(String principalId, Long quizId, boolean pass, long score) {
		QuizResponse dto = createDTO(principalId, quizId);
		PassingRecord passingRecord = createPassingRecord(principalId, quizId, dto.getId(), pass, score);
		return storeDTO(dto, passingRecord);
	}
	
	private Collection<Long> toDelete;
	
	private String userId;
	
	@Before
	public void before() throws Exception {
		toDelete = new ArrayList<Long>();
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
	}
	
	@After
	public void after() throws Exception {
		for (Long id : toDelete) {
			quizResponseDao.delete(id);
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		QuizResponse dto = createDTO(userId, 1L);
		PassingRecord passingRecord = createPassingRecord(userId, 1L, dto.getId(), true, 10L);
		QuizResponse created = storeDTO(dto, passingRecord);
		dto.setId(created.getId());
		assertEquals(dto, created);
		QuizResponse got = quizResponseDao.get(created.getId().toString());
		assertEquals(dto, got);
		quizResponseDao.delete(created.getId());
	}
	
	@Test
	public void testQueries() throws Exception {
		Long quizId = 1L;
		Long limit = 10L;
		Long offset = 0L;
		Long principalId = Long.parseLong(userId);
		List<QuizResponse> list = quizResponseDao.getAllResponsesForQuiz(quizId, limit, offset);
		assertTrue(list.isEmpty());
		long count = quizResponseDao.getAllResponsesForQuizCount(quizId);
		assertEquals(0L, count);
		list = quizResponseDao.getUserResponsesForQuiz(quizId, principalId, limit, offset);
		assertTrue(list.isEmpty());
		count = quizResponseDao.getUserResponsesForQuizCount(quizId, principalId);
		assertEquals(0L, count);
		List<PassingRecord> passingRecords = quizResponseDao.getAllPassingRecords(quizId, principalId, limit, offset);
		assertTrue(passingRecords.isEmpty());
		count = quizResponseDao.getAllPassingRecordsCount(quizId, principalId);
		assertEquals(0L, count);

		// now add some records and try retrieving
		QuizResponse created = createDTOAndStore(principalId.toString(), quizId, true, 10L);
		String someOtherUserId=BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
		QuizResponse otherUser = createDTOAndStore(someOtherUserId, quizId, true, 10L);
		QuizResponse otherQuiz = createDTOAndStore(principalId.toString(), quizId+1, true, 10L);
		list = quizResponseDao.getAllResponsesForQuiz(quizId, limit, offset);
		assertEquals(2, list.size());
		assertTrue(list.contains(created));
		assertTrue(list.contains(otherUser));
		count = quizResponseDao.getAllResponsesForQuizCount(quizId);
		assertEquals(2L, count);
		
		list = quizResponseDao.getUserResponsesForQuiz(quizId, principalId, limit, offset);
		assertEquals(1, list.size());
		assertTrue(list.contains(created));
		count = quizResponseDao.getUserResponsesForQuizCount(quizId, principalId);
		assertEquals(1L, count);
	
		// make sure pagination works
		assertEquals(0L, quizResponseDao.getAllResponsesForQuiz(quizId, /*limit*/10L, /*offset*/2L).size());
		assertEquals(0L, quizResponseDao.getAllResponsesForQuiz(quizId, /*limit*/0L, /*offset*/0L).size());
		assertEquals(0L, quizResponseDao.getUserResponsesForQuiz(quizId, principalId, /*limit*/10L, /*offset*/1L).size());
		assertEquals(0L, quizResponseDao.getUserResponsesForQuiz(quizId, principalId, /*limit*/0L, /*offset*/0L).size());
	
		// all passing records
		createDTOAndStore(principalId.toString(), quizId, false, 8L);
		createDTOAndStore(principalId.toString(), quizId, false, 2L);
		passingRecords = quizResponseDao.getAllPassingRecords(quizId, principalId, limit, offset);
		assertEquals(3, passingRecords.size());
		count = quizResponseDao.getAllPassingRecordsCount(quizId, principalId);
		assertEquals(3L, count);
	}
	
	private static void checkPassingRecord(
			PassingRecord pr, Long quizId, String userId, Long responseId, boolean passed, Long score) {
		assertEquals(passed, pr.getPassed());
		assertNotNull(pr.getPassedOn());
		assertEquals(quizId, pr.getQuizId());
		assertEquals(responseId, pr.getResponseId());
		assertEquals(score, pr.getScore());
		assertEquals(userId, pr.getUserId());
	}
	
	@Test
	public void testPassingRecord() throws Exception {
		Long quizId = 1L;
		Long principalId = Long.parseLong(userId);
		try {
			quizResponseDao.getPassingRecord(quizId, principalId);
			fail("Exception expected.");
		} catch (NotFoundException e) {
			// as expected
		}
		// now add some records and try retrieving
		long score = 10L;
		{
			QuizResponse failedQuiz = createDTOAndStore(principalId.toString(), quizId, false, score);
			String someOtherUserId=BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
			createDTOAndStore(someOtherUserId, quizId, false, score+1);
			createDTOAndStore(principalId.toString(), quizId+1, false, score+2);
			PassingRecord pr = quizResponseDao.getPassingRecord(quizId, principalId);
			checkPassingRecord(pr, quizId, principalId.toString(), failedQuiz.getId(), false, score);
		}
		
		// now add a passing quiz result and retrieve it
		{
			QuizResponse passedQuiz = createDTOAndStore(principalId.toString(), quizId, true, score+5L);
			PassingRecord pr = quizResponseDao.getPassingRecord(quizId, principalId);
			checkPassingRecord(pr, quizId, principalId.toString(), passedQuiz.getId(), true, score+5L);
		}
		
		// now add a better passing quiz result and retrieve it
		{
			QuizResponse evenBetterPassedQuiz = createDTOAndStore(principalId.toString(), quizId, true, score+10L);
			PassingRecord pr = quizResponseDao.getPassingRecord(quizId, principalId);
			checkPassingRecord(pr, quizId, principalId.toString(), evenBetterPassedQuiz.getId(), true, score+10L);
		}
	}
	

}
