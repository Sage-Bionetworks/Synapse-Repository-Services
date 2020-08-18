/**
 * 
 */
package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
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
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class CertifiedUserManagerImpl implements CertifiedUserManager {
	
	private static final String UTF_8 = "UTF-8";
	public static final String QUESTIONNAIRE_PROPERTIES_FILE = "certifiedUsersTestDefault_v4.json";
	public static final String S3_QUESTIONNAIRE_KEY = "repository-managers."+QUESTIONNAIRE_PROPERTIES_FILE;
	
	@Autowired
	private AmazonS3Utility s3Utility;	
	
	@Autowired
	private GroupMembersDAO groupMembersDao;
	
	@Autowired
	private QuizResponseDAO quizResponseDao;

	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	public CertifiedUserManagerImpl() {}
	
	/**
	 * For unit testing
	 * 
	 * @param s3Utility
	 * @param groupMembersDao
	 * @param quizResponseDao
	 * @param transactionalMessenger
	 */
	public CertifiedUserManagerImpl(
			 AmazonS3Utility s3Utility,
			 GroupMembersDAO groupMembersDao,
			 QuizResponseDAO quizResponseDao,
			 TransactionalMessenger transactionalMessenger
			) {
		this.s3Utility=s3Utility;
		this.groupMembersDao=groupMembersDao;
		this.quizResponseDao=quizResponseDao;
		this.transactionalMessenger=transactionalMessenger;
	}
	
	/**
	 * returns exception if not valid.  if list is empty the quiz generator is valid
	 * 
	 * @param quiz
	 */
	public static List<String> validateQuizGenerator(QuizGenerator quiz) {
		List<String> errorMessages = new ArrayList<String>();
		if (quiz.getId()==null) errorMessages.add("id is required.");
		//	make sure there is a minimum score and that it's >=0, <=# question varieties
		Long minimumScore = quiz.getMinimumScore();
		if (minimumScore==null || minimumScore<0) 
			errorMessages.add("expected minimumScore>=0 but found "+minimumScore);
		List<QuestionVariety> varieties = quiz.getQuestions();
		if (varieties==null || varieties.isEmpty()) {
			errorMessages.add("This test has no questions.");
			varieties = new ArrayList<QuestionVariety>(); // create an empty list so we can continue
		}
		if (minimumScore!=null && minimumScore>varieties.size())
			errorMessages.add("Minimum score cannot exceed the number of questions.");
		//	make sure there's an answer for each question
		Set<Long> questionIndices = new HashSet<Long>();
		for (QuestionVariety v : varieties) {
			List<Question> questions = v.getQuestionOptions();
			if (questions==null || questions.isEmpty()) {
				errorMessages.add("Question variety has no questions.");
				questions = new ArrayList<Question>(); // create an empty list so we can continue
			}
			for (Question q : questions) {
				Long qIndex = q.getQuestionIndex();
				if (qIndex==null) errorMessages.add("Missing question index.");
				if (questionIndices.contains(qIndex)) {
					errorMessages.add("Repeated question index "+qIndex);
				}
				questionIndices.add(qIndex);
				if (q instanceof MultichoiceQuestion) {
					MultichoiceQuestion mq = (MultichoiceQuestion)q;
					List<Long> correctAnswers = new ArrayList<Long>();
					Set<Long> answerIndices = new HashSet<Long>();
					for (MultichoiceAnswer a : mq.getAnswers()) {
						if (a.getAnswerIndex()==null)
							errorMessages.add("Answer "+a.getPrompt()+" has no index.");
						if (answerIndices.contains(a.getAnswerIndex())) {
							errorMessages.add("Repeated answer index "+a.getAnswerIndex());
						}
						answerIndices.add(a.getAnswerIndex());
						if (a.getIsCorrect()!=null && a.getIsCorrect()) correctAnswers.add(a.getAnswerIndex());
					}
					if (correctAnswers.isEmpty())
						errorMessages.add("No correct answer specified to question "+q.getPrompt());
					if (mq.getExclusive()!=null && mq.getExclusive() && correctAnswers.size()>1)
						errorMessages.add("Expected a single correct answer but found: "+correctAnswers);
				} else if (q instanceof TextFieldQuestion) {
					TextFieldQuestion tq  = (TextFieldQuestion)q;
					if (tq.getAnswer()==null)
						errorMessages.add("No answer provided for "+q.getPrompt());
				} else {
					errorMessages.add("Unexpected questions type "+QUESTIONNAIRE_PROPERTIES_FILE.getClass());
				}
			}
		}	
		return errorMessages;
	}
	
	private static String readInputStreamToString(InputStream is) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			int n = 0;
			byte[] buffer = new byte[1024];
			while (n>-1) {
				n = is.read(buffer);
				if (n>0) baos.write(buffer, 0, n);
			}
			return baos.toString(UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
				baos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	}
	
	/**
	 * Note:  The returned value can be freely modified by the caller.
	 * @return
	 */
	public QuizGenerator retrieveCertificationQuizGenerator() {
		// pull this from an S-3 File (if it's there) otherwise read from the default file on the classpath
		String quizGeneratorAsString;
		if (s3Utility.doesExist(S3_QUESTIONNAIRE_KEY)) {
			quizGeneratorAsString = s3Utility.downloadFromS3ToString(S3_QUESTIONNAIRE_KEY);
		} else {
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(QUESTIONNAIRE_PROPERTIES_FILE);
			quizGeneratorAsString = readInputStreamToString(is);
		}
		QuizGenerator quizGenerator = new QuizGenerator();
		try {
			JSONObjectAdapter adapter = (new JSONObjectAdapterImpl()).createNew(quizGeneratorAsString);
			quizGenerator.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		List<String> errorMessages = validateQuizGenerator(quizGenerator);
		if (!errorMessages.isEmpty()) throw new RuntimeException(errorMessages.toString());
		
		return quizGenerator;
	}
	
	private static Random random = new Random();
	
	public static Quiz selectQuiz(QuizGenerator quizGenerator) {
		Quiz quiz = new Quiz();
		quiz.setHeader(quizGenerator.getHeader());
		quiz.setId(quizGenerator.getId());
		List<Question> questions = new ArrayList<Question>();
		for (QuestionVariety v : quizGenerator.getQuestions()) {
			List<Question> questionOptions = v.getQuestionOptions();
			// pick a random question from the variety of questions in the QuizGenerator
			questions.add(cloneAndScrubPrivateFields(
					questionOptions.get(
							random.nextInt(questionOptions.size()))));
		}
		quiz.setQuestions(questions);
		PrivateFieldUtils.clearPrivateFields(quiz);
		return quiz;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getCertificationQuiz()
	 */
	@Override
	public Quiz getCertificationQuiz(UserInfo userInfo) {
		/*
		 * PLFM-3478 - anonymous cannot become certified user
		 */
		if (AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().equals(userInfo.getId())) {
			throw new UnauthorizedException("You need to login to take the Certification Quiz.");
		}
		QuizGenerator quizGenerator = retrieveCertificationQuizGenerator();
		Quiz quiz = selectQuiz(quizGenerator);
		return quiz;
	}
	
	public static boolean isCorrectResponse(Question q, QuestionResponse response) {
		if (q instanceof MultichoiceQuestion) {
			if (!(response instanceof MultichoiceResponse)) 
				throw new IllegalArgumentException("MultichoiceQuestion must have a MultichoiceResponse");
			Set<Long> correctAnswers = new HashSet<Long>();
			for (MultichoiceAnswer a : ((MultichoiceQuestion)q).getAnswers()) {
				if (a.getIsCorrect()!=null && a.getIsCorrect()) correctAnswers.add(a.getAnswerIndex());
			}
			return ((MultichoiceResponse)response).
					getAnswerIndex().equals(correctAnswers);
		} else if (q instanceof TextFieldQuestion) {
			if (!(response instanceof TextFieldResponse)) 
				throw new IllegalArgumentException("TextFieldQuestion must have a TextFieldResponse");
			return ((TextFieldResponse)response).getResponse().
					equalsIgnoreCase(((TextFieldQuestion)q).getAnswer());
		} else {
			throw new IllegalArgumentException("Unexpected question type "+q.getClass());
		}
	}
	
	public static Question cloneAndScrubPrivateFields(Question question) {
		Question clone;
		if (question.getConcreteType().equals("org.sagebionetworks.repo.model.quiz.MultichoiceQuestion")) {
			clone = new MultichoiceQuestion();
		} else if (question.getConcreteType().equals("org.sagebionetworks.repo.model.quiz.TextFieldQuestion")) {
			clone = new TextFieldQuestion();
		} else {
			throw new IllegalArgumentException("Unexpected type "+question.getConcreteType());
		}
		try {
			JSONObjectAdapter adapter = (new JSONObjectAdapterImpl()).createNew();
			question.writeToJSONObject(adapter);
			clone.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		PrivateFieldUtils.clearPrivateFields(clone);
		return clone;
	}
	
	/**
	 * fills in 'pass' and 'score' in the response
	 * @param quiz
	 * @param response
	 */
	public static PassingRecord scoreQuizResponse(QuizGenerator quizGenerator, QuizResponse quizResponse) {
		// The key in the following map is the *index* of the *question variety* 
		// containing the question in the list of question varieties in the QuizGenerator.  
		// (This allows us to make sure we don't receive multiple answers to a single question variety.)
		// The value in the map says whether the answer is right or wrong.
		Map<Integer, Boolean> responseMap = new HashMap<Integer, Boolean>();
		List<ResponseCorrectness> corrections = new ArrayList<ResponseCorrectness>();
		if (quizResponse.getQuestionResponses()!=null) {
			for (QuestionResponse r : quizResponse.getQuestionResponses()) {
				Integer questionVarietyIndex = null;
				// find the variety index for the question
				List<QuestionVariety> variety = quizGenerator.getQuestions();
				for (int i=0; variety!=null && i<variety.size(); i++) {
					for (Question q: variety.get(i).getQuestionOptions()) {
						if (r.getQuestionIndex()!=null && r.getQuestionIndex().equals(q.getQuestionIndex())) {
							// found it!
							questionVarietyIndex = i;
							if (responseMap.containsKey(questionVarietyIndex)) {
								throw new IllegalArgumentException("Response set contains multiple responses for question variety "+questionVarietyIndex);
							}
							boolean isCorrect = isCorrectResponse(q, r);
							responseMap.put(questionVarietyIndex, isCorrect);
							ResponseCorrectness rc = new ResponseCorrectness();
							rc.setQuestion(cloneAndScrubPrivateFields(q));
							rc.setResponse(r);
							rc.setIsCorrect(isCorrect);
							corrections.add(rc);
						}
					}
				}
				if (questionVarietyIndex==null) {
					throw new IllegalArgumentException("Question index "+r.getQuestionIndex()+
							" does not appear in quiz generator "+quizGenerator.getId());
				}
			}
		}
		int correctAnswerCount = 0;
		for (Boolean isCorrect : responseMap.values()) {
			if (isCorrect) correctAnswerCount++;
		}
		boolean pass = correctAnswerCount >= quizGenerator.getMinimumScore();
		PassingRecord passingRecord = new PassingRecord();
		passingRecord.setCorrections(corrections);
		passingRecord.setPassed(pass);
		passingRecord.setPassedOn(quizResponse.getCreatedOn());
		passingRecord.setQuizId(quizResponse.getQuizId());
		passingRecord.setResponseId(quizResponse.getId());
		passingRecord.setScore((long)correctAnswerCount);
		passingRecord.setUserId(quizResponse.getCreatedBy());		
		return passingRecord;
	}
	
	public static void fillInResponseValues(QuizResponse response, Long userId, Date createdOn, Long quizId) {
		response.setCreatedBy(userId.toString());
		response.setCreatedOn(createdOn);
		response.setQuizId(quizId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#submitCertificationQuizResponse(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.quiz.QuizResponse)
	 */
	@WriteTransaction
	@Override
	public PassingRecord submitCertificationQuizResponse(
			UserInfo userInfo, QuizResponse response) throws NotFoundException, UnauthorizedException {
		/*
		 * PLFM-3478 - anonymous cannot become certified user
		 */
		if (AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().equals(userInfo.getId())) {
			throw new UnauthorizedException("You need to login to submit the Certification Quiz.");
		}
		QuizGenerator quizGenerator = retrieveCertificationQuizGenerator();
		
		// don't let someone take the test twice
		try {
			PassingRecord passingRecord = quizResponseDao.getPassingRecord(quizGenerator.getId(), userInfo.getId());
			if(passingRecord.getPassed()) {
				throw new UnauthorizedException("You have already passed the certification test.");
			}
		} catch (NotFoundException e) {
			// OK
		}
		
		Date now = new Date();
		fillInResponseValues(response, userInfo.getId(), now, quizGenerator.getId());
		// grade the submission:  pass or fail?
		PassingRecord passingRecord = scoreQuizResponse(quizGenerator, response);
		// store the submission in the RDS
		QuizResponse created = quizResponseDao.create(response, passingRecord);
		passingRecord.setResponseId(created.getId());
		// if pass, add to Certified group
		if (passingRecord.getPassed()) {
			groupMembersDao.addMembers(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(), 
					Collections.singletonList(userInfo.getId().toString()));
		}
		transactionalMessenger.sendMessageAfterCommit(userInfo.getId().toString(), ObjectType.CERTIFIED_USER_PASSING_RECORD, ChangeType.CREATE);
		return passingRecord;
	}
	
	@Override
	public void setUserCertificationStatus(UserInfo userInfo, Long principalId, boolean isCertified) throws DatastoreException, NotFoundException {
		if (!userInfo.isAdmin()) throw new UnauthorizedException("You are not a Synapse Administrator.");
		if (isCertified) {
			groupMembersDao.addMembers(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(), 
					Collections.singletonList(principalId.toString()));
		} else {
			groupMembersDao.removeMembers(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(), 
					Collections.singletonList(principalId.toString()));
		}
	}
	

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getQuizResponses(org.sagebionetworks.repo.model.UserInfo, java.lang.Long, java.lang.Long, long, long)
	 */
	@Override
	public PaginatedResults<QuizResponse> getQuizResponses(
			UserInfo userInfo, Long principalId,
			long limit, long offset) {
		if (!userInfo.isAdmin()) throw new ForbiddenException("Only Synapse administrators may make this request.");
		QuizGenerator quizGenerator = retrieveCertificationQuizGenerator();
		long quizId = quizGenerator.getId();
		PaginatedResults<QuizResponse> result = new PaginatedResults<QuizResponse>();
		if (principalId==null) {
			result.setResults(quizResponseDao.getAllResponsesForQuiz(quizId, limit, offset));
			result.setTotalNumberOfResults(quizResponseDao.getAllResponsesForQuizCount(quizId));
		} else {
			result.setResults(quizResponseDao.getUserResponsesForQuiz(quizId, principalId, limit, offset));
			result.setTotalNumberOfResults(quizResponseDao.getUserResponsesForQuizCount(quizId, principalId));
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#deleteQuizResponse(org.sagebionetworks.repo.model.UserInfo, java.lang.Long)
	 */
	@Override
	public void deleteQuizResponse(UserInfo userInfo, Long responseId) throws NotFoundException {
		if (!userInfo.isAdmin()) throw new ForbiddenException("Only Synapse administrators may make this request.");
		quizResponseDao.delete(responseId);
	}

	@Override
	public PassingRecord getPassingRecord(Long principalId) throws DatastoreException, NotFoundException {
		QuizGenerator quizGenerator = retrieveCertificationQuizGenerator();
		long quizId = quizGenerator.getId();
		return quizResponseDao.getPassingRecord(quizId, principalId);
	}

	@Override
	public PaginatedResults<PassingRecord> getPassingRecords(UserInfo userInfo, Long principalId, long limit, long offset) throws DatastoreException, NotFoundException {
		if (!userInfo.isAdmin()) 
			throw new ForbiddenException("Only Synapse administrators may make this request.");
		QuizGenerator quizGenerator = retrieveCertificationQuizGenerator();
		long quizId = quizGenerator.getId();
		PaginatedResults<PassingRecord> result = new PaginatedResults<PassingRecord>();
		result.setResults(quizResponseDao.getAllPassingRecords(quizId, principalId, limit, offset));
		result.setTotalNumberOfResults(quizResponseDao.getAllPassingRecordsCount(quizId, principalId));
		return result;
	}
}
