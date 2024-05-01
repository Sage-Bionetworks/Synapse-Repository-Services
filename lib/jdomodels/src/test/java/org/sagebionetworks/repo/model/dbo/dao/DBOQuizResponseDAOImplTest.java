package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.QuizResponseDAO;
import org.sagebionetworks.repo.model.quiz.MultichoiceResponse;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.QuestionResponse;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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
	
	@BeforeEach
	public void before() throws Exception {
		toDelete = new ArrayList<Long>();
    	userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
	}
	
	@AfterEach
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
		
	@Test
	public void testPassingRecord() throws Exception {
		Long quizId = 1L;
		Long principalId = Long.parseLong(userId);

		assertEquals(Optional.empty(), quizResponseDao.getLatestPassingRecord(quizId, principalId));
		
		// now add some records and try retrieving
		long score = 10L;
		
		QuizResponse failedQuiz = createDTOAndStore(principalId.toString(), quizId, false, score);
		
		String someOtherUserId=BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
		createDTOAndStore(someOtherUserId, quizId, false, score+1);
		createDTOAndStore(principalId.toString(), quizId+1, false, score+2);
		
		PassingRecord pr = quizResponseDao.getLatestPassingRecord(quizId, principalId).get();
		
		assertEquals(
			new PassingRecord()
				.setCorrections(pr.getCorrections())
				.setPassed(false)
				.setRevoked(false)
				.setPassedOn(pr.getPassedOn())
				.setQuizId(quizId)
				.setResponseId(failedQuiz.getId())
				.setUserId(userId)
				.setScore(score), 
		pr);
		
		// Does not revoke a failed quiz
		assertFalse(quizResponseDao.revokeQuizResponse(failedQuiz.getId()));		
				
		// now add a passing quiz result and retrieve it
		QuizResponse passedQuiz = createDTOAndStore(principalId.toString(), quizId, true, score+5L);
		
		pr = quizResponseDao.getLatestPassingRecord(quizId, principalId).get();
		
		assertEquals(
			new PassingRecord()
				.setCorrections(pr.getCorrections())
				.setPassed(true)
				.setRevoked(false)
				.setPassedOn(pr.getPassedOn())
				.setQuizId(quizId)
				.setResponseId(passedQuiz.getId())
				.setUserId(userId)
				.setScore(score + 5), 
		pr);
		
		// now add a better passing quiz result and retrieve it
		QuizResponse evenBetterPassedQuiz = createDTOAndStore(principalId.toString(), quizId, true, score+10L);
		
		pr = quizResponseDao.getLatestPassingRecord(quizId, principalId).get();		

		assertEquals(
			new PassingRecord()
				.setCorrections(pr.getCorrections())
				.setPassed(true)
				.setRevoked(false)
				.setPassedOn(pr.getPassedOn())
				.setQuizId(quizId)
				.setResponseId(evenBetterPassedQuiz.getId())
				.setUserId(userId)
				.setScore(score + 10),
		pr);
		
		// Now revoke the latest response
		
		assertTrue(quizResponseDao.revokeQuizResponse(evenBetterPassedQuiz.getId()));
		
		pr = quizResponseDao.getLatestPassingRecord(quizId, principalId).get();		

		assertNotNull(pr.getRevokedOn());
		
		assertEquals(
			new PassingRecord()
				.setCorrections(pr.getCorrections())
				.setPassed(true)
				.setRevoked(true)
				.setPassedOn(pr.getPassedOn())
				.setRevokedOn(pr.getRevokedOn())
				.setQuizId(quizId)
				.setResponseId(evenBetterPassedQuiz.getId())
				.setUserId(userId)
				.setScore(score + 10),
		pr);
		
		// Revoking a second time does not work
		assertFalse(quizResponseDao.revokeQuizResponse(evenBetterPassedQuiz.getId()));
		
		assertEquals(pr, quizResponseDao.getLatestPassingRecord(quizId, principalId).get());
		
	}
	

}
