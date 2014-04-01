/**
 * 
 */
package org.sagebionetworks.repo.manager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.quiz.MultichoiceAnswer;
import org.sagebionetworks.repo.model.quiz.MultichoiceQuestion;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Question;
import org.sagebionetworks.repo.model.quiz.QuestionVariety;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.model.quiz.TextFieldQuestion;
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
	public static void validateQuiz(Quiz quiz) {
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
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getCertificationQuiz()
	 */
	@Override
	public Quiz getCertificationQuiz() {
		// pull this from an S-3 File (TODO cache it, temporarily)
		String quizAsString = s3Utility.downloadFromS3ToString(S3_QUESTIONNAIRE_KEY);
		Quiz quiz = new Quiz();
		try {
			JSONObjectAdapter adapter = (new JSONObjectAdapterImpl()).createNew(quizAsString);
			quiz.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		validateQuiz(quiz);
		PrivateFieldUtils.clearPrivateFields(quiz);
		
		return quiz;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#submitCertificationQuizResponse(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.quiz.QuizResponse)
	 */
	@Override
	public QuizResponse submitCertificationQuizResponse(
			UserInfo userInfo, QuizResponse response) {
		// TODO validate the submission
		// make sure that the quiz ID matches that of the Cert User quiz
		// make sure createdOn and createdBy are not null
		// TODO grade the submission:  pass or fail?
		// TODO if pass, add to Certified group
		// TODO store the submission in the RDS
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
