package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
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
	
	@Autowired
	private EntityManager entityManager;
	
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
		asyncHelper.waitForObjectReplication(ReplicationType.SUBMISSION, Long.valueOf(status.getId()), status.getEtag(), MAX_WAIT);
				
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
	
	// Reproduce PLFM-6344
	@Test
	public void testViewColumnModelRequestWorkerWithEmptyStringAnnotation() throws Exception {
		Folder entity = testHelper.createFolder(submitter, submitterProject);
		
		SubmissionBundle submission = testHelper.createSubmission(submitter, evaluation, entity);
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission.getSubmission().getId());
		
		Annotations annotations = AnnotationsV2Utils.emptyAnnotations();
		
		// put empty string
		AnnotationsV2TestUtils.putAnnotations(annotations, "foo", "", AnnotationsValueType.STRING);
		
		status.setSubmissionAnnotations(annotations);
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		// Wait for the object replication
		asyncHelper.waitForObjectReplication(ReplicationType.SUBMISSION, Long.valueOf(status.getId()), status.getEtag(), MAX_WAIT);
				
		ViewScope viewScope = new ViewScope();
		
		viewScope.setViewEntityType(ViewEntityType.submissionview);
		viewScope.setScope(ImmutableList.of(evaluation.getId()));
		
		ViewColumnModelRequest request = new ViewColumnModelRequest();
		
		request.setViewScope(viewScope);
		
		// expect ColumnConstants.DEFAULT_STRING_SIZE as max size when empty string is given in the annotations
		List<ColumnModel> expectedResult = ImmutableList.of(
				columnModel("foo", ColumnType.STRING, ColumnConstants.DEFAULT_STRING_SIZE));
		
		asyncHelper.assertJobResponse(evaluationOwner, request, (ViewColumnModelResponse response) -> {
			List<ColumnModel> results = response.getResults();
			assertEquals(expectedResult, results);
			assertNull(response.getNextPageToken());
		}, MAX_WAIT);	
	}
	
	// Reproduce https://sagebionetworks.jira.com/browse/PLFM-7541. Datasets are collections of versioned files, so the request might include specific versions
	// of file entities, the result should include the annotations for the selected versions.
	@Test
	public void testViewColumnModelRequestWorkerWithIdAndVersion() throws Exception {
		FileEntity fileOne = testHelper.createFileWithMultipleVersions(submitter, submitterProject.getId(), 1, "foo", 2);
		FileEntity fileTwo = testHelper.createFileWithMultipleVersions(submitter, submitterProject.getId(), 2, "bar", 2);
		
		// Remove the annotations from the current version of the second file
		entityManager.updateAnnotations(submitter, fileTwo.getId(), AnnotationsV2Utils.emptyAnnotations().setId(fileTwo.getId()).setEtag(fileTwo.getEtag()));
		fileTwo = entityManager.getEntity(submitter, fileTwo.getId(), FileEntity.class);
		
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileOne.getId()), fileOne.getEtag(), MAX_WAIT);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileTwo.getId()), fileTwo.getEtag(), MAX_WAIT);
		
		ViewScope viewScope = new ViewScope();
		
		viewScope.setViewEntityType(ViewEntityType.dataset);
		viewScope.setScope(List.of(fileOne.getId(), fileTwo.getId()));
		
		ViewColumnModelRequest request = new ViewColumnModelRequest().setViewScope(viewScope);
		
		// Only the "foo" annotation is expected since the current version of fileTwo does not have any annotation
		asyncHelper.assertJobResponse(submitter, request, (ViewColumnModelResponse response) -> {
			Set<String> columnNames = response.getResults().stream().map(ColumnModel::getName).collect(Collectors.toSet());
			assertEquals(Set.of("foo"), columnNames);
		}, MAX_WAIT);
		
		// Now include specific version of the files
		viewScope.setScope(List.of(
			KeyFactory.idAndVersion(fileOne.getId(), 2L).toString(),
			KeyFactory.idAndVersion(fileTwo.getId(), 1L).toString()
		));
				
		// We now expect both "foo" and "bar" since version 1 of the second file had that annotation
		asyncHelper.assertJobResponse(submitter, request, (ViewColumnModelResponse response) -> {
			Set<String> columnNames = response.getResults().stream().map(ColumnModel::getName).collect(Collectors.toSet());
			assertEquals(Set.of("foo", "bar"), columnNames);
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
