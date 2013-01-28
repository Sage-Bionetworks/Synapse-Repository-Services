package org.sagebionetworks.repo.competition.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Test basic operations of SubmissionBundle
 * @author bkng
 *
 */
public class SubmissionBundleTest {
	
	private SubmissionBundle submissionBundle;
	
	@Before
	public void setUp() {
		submissionBundle = new SubmissionBundle();
	}
	
	@Test
	public void testJSONRoundTrip() throws Exception{
		submissionBundle = createDummySubmissionBundle();
		
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		joa = submissionBundle.writeToJSONObject(joa);
		String json = joa.toJSONString();
		assertNotNull(json);
		
		SubmissionBundle clone = new SubmissionBundle();
		clone.initializeFromJSONObject(joa.createNew(json));
		assertEquals(submissionBundle, clone);		
	}
	
	/**
	 * Create an EntityBundle filled with dummy data
	 */
	public static SubmissionBundle createDummySubmissionBundle() {
		SubmissionBundle submissionBundle = new SubmissionBundle();
		
		Submission submission = new Submission();
		submission.setCompetitionId("1234");
		submission.setCreatedOn(new Date());
		submission.setEntityId("5678");
		submission.setId("42");
		submission.setName("some name");
		submission.setUserId("314159");
		submission.setVersionNumber(1L);
		
		SubmissionStatus submissionStatus = new SubmissionStatus();
		submissionStatus.setEtag("etag");
		submissionStatus.setId("42");
		submissionStatus.setModifiedOn(new Date());
		submissionStatus.setScore(3.14);
		submissionStatus.setStatus(SubmissionStatusEnum.SCORED);
		
		submissionBundle.setSubmission(submission);
		submissionBundle.setSubmissionStatus(submissionStatus);
		
		return submissionBundle;
	}
}
