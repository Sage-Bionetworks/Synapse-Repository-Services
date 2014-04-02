/**
 * 
 */
package org.sagebionetworks.repo.manager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
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
import org.sagebionetworks.repo.model.quiz.TextFieldQuestion;
import org.sagebionetworks.repo.model.quiz.TextFieldResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author brucehoff
 *
 */
public class CertifiedUserManagerImpl implements CertifiedUserManager, InitializingBean {
	
	public static final String QUESTIONNAIRE_PROPERTIES_FILE = "certifiedUsersTestDefault.json";
	public static final String S3_QUESTIONNAIRE_KEY = "repository-managers."+QUESTIONNAIRE_PROPERTIES_FILE;

	@Autowired
	AmazonS3Utility s3Utility;	
	
	/**
	 * Throw exception if not valid
	 * 
	 * @param quiz
	 */
	public static void validateQuizGenerator(QuizGenerator quiz) {
		//	make sure there is a minimum score and that it's >=0, <=# question varieties
		Long minimumScore = quiz.getMinimumScore();
		if (minimumScore==null || minimumScore<0) 
			throw new RuntimeException("expected minimumScore>-0 but found "+minimumScore);
		List<QuestionVariety> varieties = quiz.getQuestions();
		if (varieties==null || varieties.size()==0)
			throw new RuntimeException("This test has no questions.");
		if (minimumScore>varieties.size())
			throw new RuntimeException("Minimum score cannot exceed the number of questions.");
		//	make sure there's an answer for each question
		for (QuestionVariety v : varieties) {
			List<Question> questions = v.getQuestionOptions();
			if (questions==null || questions.size()==0) 
				throw new RuntimeException("Question variety has no questions.");
			for (Question q : questions) {
				Long qIndex = q.getQuestionIndex();
				if (qIndex==null) throw new RuntimeException("Missing question index.");
				if (q instanceof MultichoiceQuestion) {
					MultichoiceQuestion mq = (MultichoiceQuestion)q;
					List<Long> correctAnswers = new ArrayList<Long>();
					for (MultichoiceAnswer a : mq.getAnswers()) {
						if (a.getAnswerIndex()==null)
							throw new RuntimeException("Answer "+a.getPrompt()+" has no index.");
						if (a.getIsCorrect()!=null && a.getIsCorrect()) correctAnswers.add(a.getAnswerIndex());
					}
					if (correctAnswers.size()==0)
						throw new RuntimeException("No correct answer specified to question "+q.getPrompt());
					if (mq.getExclusive()!=null && mq.getExclusive() && correctAnswers.size()>1)
						throw new RuntimeException("Expected a single correct answer but found: "+correctAnswers);
				} else if (q instanceof TextFieldQuestion) {
					TextFieldQuestion tq  = (TextFieldQuestion)q;
					if (tq.getAnswer()==null)
						throw new RuntimeException("No answer provided for "+q.getPrompt());
				} else {
					throw new RuntimeException("Unexpected questions type "+QUESTIONNAIRE_PROPERTIES_FILE.getClass());
				}
			}
		}		
	}
	
	private QuizGenerator retrieveCertificationQuizGenerator() {
		// pull this from an S-3 File (TODO cache it, temporarily)
		String quizGeneratorAsString = s3Utility.downloadFromS3ToString(S3_QUESTIONNAIRE_KEY);
		QuizGenerator quizGenerator = new QuizGenerator();
		try {
			JSONObjectAdapter adapter = (new JSONObjectAdapterImpl()).createNew(quizGeneratorAsString);
			quizGenerator.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		validateQuizGenerator(quizGenerator);
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
			questions.add(questionOptions.get(random.nextInt(questionOptions.size())));
		}
		quiz.setQuestions(questions);
		PrivateFieldUtils.clearPrivateFields(quiz);
		return quiz;
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getCertificationQuiz()
	 */
	@Override
	public Quiz getCertificationQuiz() {
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
				if (a.getIsCorrect()) correctAnswers.add(a.getAnswerIndex());
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
	
	/**
	 * 
	 * @param quiz
	 * @param response
	 * @return true iff response passes
	 */
	public static boolean scoreQuizResponse(QuizGenerator quizGenerator, QuizResponse quizResponse) {
		//Map<Long, QuestionResponse> correctResponses = compileCorrectAnswers(quiz);
		// The key in the following map is the *index* of the answered question in 
		// the *question variety* of the QuizGenerator.  The value says whether the
		// answer is right or wrong.
		Map<Integer, Boolean> responseMap = new HashMap<Integer, Boolean>();
		for (QuestionResponse r : quizResponse.getQuestionResponses()) {
			Integer questionVarietyIndex = null;
			// find the variety index for the question
			List<QuestionVariety> variety = quizGenerator.getQuestions();
			for (int i=0; i<variety.size(); i++) {
				for (Question q: variety.get(i).getQuestionOptions()) {
					if (r.getQuestionIndex() == q.getQuestionIndex()) {
						// found it!
						if (responseMap.containsKey(questionVarietyIndex)) {
							throw new IllegalArgumentException("Response set contains multiple responses for question variety "+questionVarietyIndex);
						}
						responseMap.put(questionVarietyIndex, isCorrectResponse(q, r));
					}
				}
			}
			if (questionVarietyIndex==null) {
				throw new IllegalArgumentException("Question index "+r.getQuestionIndex()+
						" does not appear in quiz generator "+quizGenerator.getId());
			}
		}
		int correctAnswerCount = 0;
		for (Boolean isCorrect : responseMap.values()) {
			if (isCorrect) correctAnswerCount++;
		}
		boolean pass = correctAnswerCount >= quizGenerator.getMinimumScore();
		quizResponse.setPass(pass);
		return pass;
	}
	
	public static void fillInResponseValues(QuizResponse response, Long userId, Date createdOn, Long quizId) {
		response.setCreatedBy(userId.toString());
		response.setCreatedOn(createdOn);
		response.setQuizId(quizId);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#submitCertificationQuizResponse(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.quiz.QuizResponse)
	 */
	@Override
	public QuizResponse submitCertificationQuizResponse(
			UserInfo userInfo, QuizResponse response) {
		QuizGenerator quizGenerator = retrieveCertificationQuizGenerator();
		// grade the submission:  pass or fail?
		boolean pass = scoreQuizResponse(quizGenerator, response);
		fillInResponseValues(response, userInfo.getId(), new Date(), quizGenerator.getId());
		// TODO store the submission in the RDS
		// TODO if pass, add to Certified group
		return null; // TODO return the created object
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getQuizResponses(org.sagebionetworks.repo.model.UserInfo, java.lang.Long, java.lang.Long, long, long)
	 */
	@Override
	public PaginatedResults<QuizResponse> getQuizResponses(
			UserInfo userInfo, Long principalId,
			long limit, long offset) {
		// TODO validate userInfo -- only an admin may make this request
		// TODO get the responses in the system, filtered quiz id and optionally user id
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#deleteQuizResponse(org.sagebionetworks.repo.model.UserInfo, java.lang.Long)
	 */
	@Override
	public void deleteQuizResponse(UserInfo userInfo, Long responseId) {
		// TODO validate userInfo -- only an admin may make this request
		// TODO delete the quiz
	}

	@Override
	public PassingRecord getPassingRecord(UserInfo userInfo, Long principalId) {
		// TODO retrieve whether the user passed and if so when
		return null;
	}
	
	/**
	 * make sure that the Certified User test is created in S3
	 */
	@Override
	public void afterPropertiesSet() {
		if (!s3Utility.doesExist(S3_QUESTIONNAIRE_KEY)) {
			// read from properties file
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(QUESTIONNAIRE_PROPERTIES_FILE);
			// upload to S3
			s3Utility.uploadInputStreamToS3File(S3_QUESTIONNAIRE_KEY, is, "utf-8");
		}
	}

}
