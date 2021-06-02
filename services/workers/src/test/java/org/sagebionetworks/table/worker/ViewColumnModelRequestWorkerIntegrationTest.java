package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ViewColumnModelRequestWorkerIntegrationTest {
	
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
	public void testViewColumnModelRequestWorker() throws Exception {
		Folder entity = testHelper.createFolder(submitter, submitterProject);
		
		SubmissionBundle submission = testHelper.createSubmission(submitter, evaluation, entity);
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission.getSubmission().getId());
		
		Annotations annotations = AnnotationsV2Utils.emptyAnnotations();
		
		AnnotationsV2TestUtils.putAnnotations(annotations, "foo", "bar", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "bar", "2", AnnotationsValueType.LONG);
		
		status.setSubmissionAnnotations(annotations);
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		// Wait for the object replication
		asyncHelper.waitForObjectReplication(ViewObjectType.SUBMISSION, Long.valueOf(status.getId()), status.getEtag(), MAX_WAIT);
				
		ViewScope viewScope = new ViewScope();
		
		viewScope.setViewEntityType(ViewEntityType.submissionview);
		viewScope.setScope(ImmutableList.of(evaluation.getId()));
		
		ViewColumnModelRequest request = new ViewColumnModelRequest();
		
		request.setViewScope(viewScope);
		
		List<ColumnModel> expectedResult = ImmutableList.of(
				columnModel("bar", ColumnType.INTEGER), 
				columnModel("foo", ColumnType.STRING, 3L)
		);		
		
		asyncHelper.assertJobResponse(evaluationOwner, request, (ViewColumnModelResponse response) -> {
			List<ColumnModel> sortedResults = response.getResults()
					.stream()
					.sorted((m1, m2) -> m1.getName().compareTo(m2.getName()))
					.collect(Collectors.toList());
			
			assertEquals(expectedResult, sortedResults);
			assertNull(response.getNextPageToken());
		}, MAX_WAIT);	
	}
	
	private static ColumnModel columnModel(String name, ColumnType type) {
		return columnModel(name, type, null);
	}
	
	private static ColumnModel columnModel(String name, ColumnType type, Long maxSize) {
		ColumnModel column = new ColumnModel();
		column.setName(name);
		column.setColumnType(type);
		column.setMaximumSize(maxSize);
		return column;
	}

}
