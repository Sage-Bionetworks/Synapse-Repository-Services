/**
 * 
 */
package org.sagebionetworks.repo.manager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.questionnaire.MultichoiceAnswer;
import org.sagebionetworks.repo.model.questionnaire.MultichoiceQuestion;
import org.sagebionetworks.repo.model.questionnaire.PassingRecord;
import org.sagebionetworks.repo.model.questionnaire.Question;
import org.sagebionetworks.repo.model.questionnaire.QuestionVariety;
import org.sagebionetworks.repo.model.questionnaire.Questionnaire;
import org.sagebionetworks.repo.model.questionnaire.QuestionnaireResponse;
import org.sagebionetworks.repo.model.questionnaire.TextFieldQuestion;
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
	 * @param questionnaire
	 */
	public static void validateQuestionnaire(Questionnaire questionnaire) {
		//	make sure there is a minimum score and that it's >=0, <=# question varieties
		Long minimumScore = questionnaire.getMinimumScore();
		if (minimumScore==null || minimumScore<0) 
			throw new RuntimeException("expected minimumScore>-0 but found "+minimumScore);
		List<QuestionVariety> varieties = questionnaire.getQuestions();
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
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getCertificationQuestionnaire()
	 */
	@Override
	public Questionnaire getCertificationQuestionnaire() {
		// pull this from an S-3 File (TODO cache it, temporarily)
		String questionnaireAsString = s3Utility.downloadFromS3ToString(S3_QUESTIONNAIRE_KEY);
		Questionnaire questionnaire = new Questionnaire();
		try {
			JSONObjectAdapter adapter = (new JSONObjectAdapterImpl()).createNew(questionnaireAsString);
			questionnaire.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		validateQuestionnaire(questionnaire);
		PrivateFieldUtils.clearPrivateFields(questionnaire);
		
		return questionnaire;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#submitCertificationQuestionnaireResponse(org.sagebionetworks.repo.model.UserInfo, org.sagebionetworks.repo.model.questionnaire.QuestionnaireResponse)
	 */
	@Override
	public QuestionnaireResponse submitCertificationQuestionnaireResponse(
			UserInfo userInfo, QuestionnaireResponse response) {
		// TODO validate the submission
		// make sure that the questionnaire ID matches that of the Cert User questionnaire
		// make sure createdOn and createdBy are not null
		// TODO grade the submission:  pass or fail?
		// TODO if pass, add to Certified group
		// TODO store the submission in the RDS
		return null; // TODO return the created object
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#getQuestionnaireResponses(org.sagebionetworks.repo.model.UserInfo, java.lang.Long, java.lang.Long, long, long)
	 */
	@Override
	public PaginatedResults<QuestionnaireResponse> getQuestionnaireResponses(
			UserInfo userInfo, Long principalId,
			long limit, long offset) {
		// TODO validate userInfo -- only an admin may make this request
		// TODO get the responses in the system, filtered questionnaire id and optionally user id
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.CertifiedUserManager#deleteQuestionnaireResponse(org.sagebionetworks.repo.model.UserInfo, java.lang.Long)
	 */
	@Override
	public void deleteQuestionnaireResponse(UserInfo userInfo, Long responseId) {
		// TODO validate userInfo -- only an admin may make this request
		// TODO delete the questionnaire
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
