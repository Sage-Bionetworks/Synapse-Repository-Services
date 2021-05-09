package org.sagebionetworks.replication.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.evaluation.dao.SubmissionField;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SubmissionReplicationIntegrationTest {
	
	private static final int MAX_WAIT = 2 * 60 * 1000;

	@Autowired
	private SubmissionManager submissionManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	private TestHelper testHelper;
	
	private UserInfo evaluationOwner;

	private UserInfo submitter;
	
	private Team submitterTeam;

	private Project evaluationProject;
	private Project submitterProject;
	
	private Evaluation evaluation;
	

	@BeforeEach
	public void before() throws Exception {
		
		testHelper.before();
		
		evaluationOwner = testHelper.createUser();
		submitter = testHelper.createUser();
		submitterTeam = testHelper.createTeam(submitter);
		submitterProject = testHelper.createProject(submitter);
		
		evaluationProject = testHelper.createProject(evaluationOwner);
		evaluation = testHelper.createEvaluation(evaluationOwner, evaluationProject);

		testHelper.setEvaluationACLForSubmission(evaluationOwner, evaluation, submitterTeam);
	}

	@AfterEach
	public void after() {
		testHelper.cleanup();
	}

	@Test
	public void testSubmissionReplication() throws Exception {
		Entity entity = testHelper.createFolder(submitter, submitterProject);
		SubmissionBundle submission = testHelper.createSubmission(submitter, evaluation, entity);
		
		Long submissionId = Long.valueOf(submission.getSubmissionStatus().getId());
		String etag = submission.getSubmissionStatus().getEtag();
		
		ObjectDataDTO data = asyncHelper.waitForObjectReplication(ViewObjectType.SUBMISSION, submissionId, etag, MAX_WAIT);

		assertEquals(KeyFactory.stringToKey(evaluationProject.getId()), data.getProjectId());
		assertEquals(KeyFactory.stringToKey(evaluation.getId()), data.getParentId());
		assertEquals(KeyFactory.stringToKey(evaluation.getId()), data.getBenefactorId());
		assertEquals(submitter.getId(), data.getCreatedBy());
		
		Map<String, ObjectAnnotationDTO> annotations = data.getAnnotations()
				.stream()
				.collect(Collectors.toMap(ObjectAnnotationDTO::getKey, Function.identity()));
		
		assertFalse(annotations.isEmpty());
		
		assertValue(submitter.getId().toString(), annotations.get(SubmissionField.submitterid.getColumnName()));
		assertValue(submission.getSubmissionStatus().getStatus().name(), annotations.get(SubmissionField.status.getColumnName()));

		// Updates the status
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission.getSubmission().getId());
		status.setStatus(SubmissionStatusEnum.EVALUATION_IN_PROGRESS);
		
		// And add some annotations
		Annotations submissionAnnotations = AnnotationsV2Utils.emptyAnnotations();
		AnnotationsV2TestUtils.putAnnotations(submissionAnnotations, "foo", "bar", AnnotationsValueType.STRING);
		status.setSubmissionAnnotations(submissionAnnotations);
		
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		etag = status.getEtag();
		
		data = asyncHelper.waitForObjectReplication(ViewObjectType.SUBMISSION, submissionId, etag, MAX_WAIT);
		
		annotations = data.getAnnotations()
				.stream()
				.collect(Collectors.toMap(ObjectAnnotationDTO::getKey, Function.identity()));
		
		// Makes sure the status was updated
		assertValue(status.getStatus().name(), annotations.get(SubmissionField.status.getColumnName()));
		// And that the annotation was added
		assertValue("bar", annotations.get("foo"));
		
	}
	
	private void assertValue(String expectedValue, ObjectAnnotationDTO annotation) {
		assertEquals(expectedValue, annotation.getValue().iterator().next());
	}

}
