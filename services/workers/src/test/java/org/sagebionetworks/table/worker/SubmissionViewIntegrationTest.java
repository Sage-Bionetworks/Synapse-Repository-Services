package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityUpdateResult;
import org.sagebionetworks.repo.model.table.EntityUpdateResults;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SubmissionViewIntegrationTest {

	private static final int MAX_WAIT = 2 * 60 * 1000;
	
	@Autowired
	private SubmissionManager submissionManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	
	@Autowired
	private TestHelper testHelper;
	
	@Autowired
	private TableIndexDAO indexDao;
	
	@Autowired
	private ColumnModelManager modelManager;

	@Autowired
	private TableRowTruthDAO tableRowTruthDao;
	
	@Autowired
	private TableIndexConnectionFactory connectionFactory;
	
	private UserInfo evaluationOwner;

	private UserInfo submitter1;
	private UserInfo submitter2;
	
	private Team submitterTeam;

	private Project evaluationProject;
	private Project submitter1Project;
	private Project submitter2Project;
	
	private Evaluation evaluation;
	
	private SubmissionView view;
	
	@BeforeEach
	public void before() throws Exception {
		
		tableRowTruthDao.truncateAllRowData();
		
		testHelper.before();
		
		evaluationOwner = testHelper.createUser();
		
		submitter1 = testHelper.createUser();
		submitter2 = testHelper.createUser();
		
		submitterTeam = testHelper.createTeam(submitter1);
		// Share one team for submissions
		submitter2.getGroups().add(Long.valueOf(submitterTeam.getId()));
		
		submitter1Project = testHelper.createProject(submitter1);
		submitter2Project = testHelper.createProject(submitter2);
				
		evaluationProject = testHelper.createProject(evaluationOwner);
		evaluation = testHelper.createEvaluation(evaluationOwner, evaluationProject);

		testHelper.setEvaluationACLForSubmission(evaluationOwner, evaluation, submitterTeam);
	}

	@AfterEach
	public void after() {

		tableRowTruthDao.truncateAllRowData();
		
		testHelper.cleanup();
		
		if (view != null) {
			indexDao.deleteTable(IdAndVersion.parse(view.getId()));
		}
	}
	
	@Test
	public void testSubmissionView() throws Exception {
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);
		Folder entity2 = testHelper.createFolder(submitter2, submitter2Project);
		
		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		SubmissionBundle submission2 = testHelper.createSubmission(submitter2, evaluation, entity2);
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1, submission2);		
		
		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
		
	}
	
	@Test
	public void testSubmissionViewWithMultipleEvaluations() throws Exception {
		Evaluation evaluation2 = testHelper.createEvaluation(evaluationOwner, evaluationProject);
		
		testHelper.setEvaluationACLForSubmission(evaluationOwner, evaluation2, submitterTeam);
		
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);
		Folder entity2 = testHelper.createFolder(submitter2, submitter2Project);
		
		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		SubmissionBundle submission2 = testHelper.createSubmission(submitter2, evaluation2, entity2);
		
		view = createView(evaluationOwner, evaluationProject, evaluation, evaluation2);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1, submission2);
		
		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
	}

	@Test
	public void testSubmissionViewWithEvaluationRound() throws Exception {

		testHelper.setEvaluationACLForSubmission(evaluationOwner, evaluation, submitterTeam);

		testHelper.createEvaluationRound(evaluationOwner, evaluation.getId());

		Folder entity = testHelper.createFolder(submitter1, submitter1Project);

		SubmissionBundle submission = testHelper.createSubmission(submitter1, evaluation, entity);

		view = createView(evaluationOwner, evaluationProject, evaluation, evaluation);

		List<SubmissionBundle> submissions = Collections.singletonList(submission);

		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
	}
	
	@Test
	public void testSubmissionViewWithTeam() throws Exception {
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);
		
		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1, submitterTeam);
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1);
		
		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
	}
	
	@Test
	public void testSubmissionViewWithNewSubmmision() throws Exception {
		
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);
		
		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1);		

		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);

		// Add another submission
		Folder entity2 = testHelper.createFolder(submitter2, submitter2Project);
		SubmissionBundle submission2 = testHelper.createSubmission(submitter2, evaluation, entity2);
		
		submissions = ImmutableList.of(submission1, submission2);

		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
	}
	
	@Test
	public void testSubmissionViewWithStatusUpdate() throws Exception {
		
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);
		Folder entity2 = testHelper.createFolder(submitter2, submitter2Project);

		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		SubmissionBundle submission2 = testHelper.createSubmission(submitter2, evaluation, entity2);
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1, submission2);
		
		// Wait for the results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
		
		// Updates the status of the 1st submission
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission1.getSubmission().getId());
		
		status.setStatus(SubmissionStatusEnum.ACCEPTED);
		
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		submission1.setSubmissionStatus(status);

		// Wait for the updated results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);

	}
	
	@Test
	public void testSubmissionViewWithAnnotations() throws Exception {
		
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);

		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1);
		
		// Wait for the results
		QueryResultBundle result = assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
		
		// Now add the "foo" column to the model
		List<ColumnModel> columnModels = result.getColumnModels();
		
		ColumnModel fooColumnModel = modelManager.createColumnModel(evaluationOwner, TableModelTestUtils.createColumn(null, "foo", ColumnType.STRING));

		columnModels.add(fooColumnModel);
		
		// Updates the schema
		asyncHelper.setTableSchema(evaluationOwner, TableModelUtils.getIds(columnModels), view.getId(), MAX_WAIT);

		// Updates the status of the 1st submission adding the foo annotation
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission1.getSubmission().getId());
		
		Annotations annotations = AnnotationsV2Utils.emptyAnnotations();
		AnnotationsV2TestUtils.putAnnotations(annotations, "foo", "bar", AnnotationsValueType.STRING);
		
		status.setSubmissionAnnotations(annotations);
		status.setStatus(SubmissionStatusEnum.ACCEPTED);
		
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		submission1.setSubmissionStatus(status);
		
		// Wait for the updated results
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
	}
	
	@Test
	public void testSubmissionViewSuggestedSchema() throws Exception {
		
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);

		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		
		// Updates the status of the 1st submission adding the foo annotation
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission1.getSubmission().getId());
		
		Annotations annotations = AnnotationsV2Utils.emptyAnnotations();
		AnnotationsV2TestUtils.putAnnotations(annotations, "foo", "bar", AnnotationsValueType.STRING);
		
		status.setSubmissionAnnotations(annotations);
		status.setStatus(SubmissionStatusEnum.ACCEPTED);
		
		status = submissionManager.updateSubmissionStatus(evaluationOwner, status);
		
		submission1.setSubmissionStatus(status);
		
		Long submissionId = Long.valueOf(status.getId());
		String etag = status.getEtag();
		
		// Wait for the replication to finish
		asyncHelper.waitForObjectReplication(ViewObjectType.SUBMISSION, submissionId, etag, MAX_WAIT);
		
		ViewScope viewScope = new ViewScope();
		viewScope.setScope(ImmutableList.of(evaluation.getId()));
		viewScope.setViewEntityType(ViewEntityType.submissionview);
		
		ColumnModelPage page = connectionFactory.connectToFirstIndex().getPossibleColumnModelsForScope(viewScope, null);
		
		List<ColumnModel> model = page.getResults();

		assertEquals(1, model.size());
		assertEquals("foo", model.get(0).getName());
		assertEquals(ColumnType.STRING, model.get(0).getColumnType());
	}
	
	@Test
	public void testSubmissionViewAnnotationUpdateWithPartialRowSet() throws Exception {
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);

		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		Long submissionId = KeyFactory.stringToKey(submission1.getSubmissionStatus().getId());
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1);
		
		// Wait for the results
		QueryResultBundle result = assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
		
		// Now add the "foo" column to the model
		List<ColumnModel> columnModels = result.getColumnModels();
		
		ColumnModel fooColumnModel = modelManager.createColumnModel(evaluationOwner, TableModelTestUtils.createColumn(null, "foo", ColumnType.STRING));

		columnModels.add(fooColumnModel);
		
		// Updates the schema
		asyncHelper.setTableSchema(evaluationOwner, TableModelUtils.getIds(columnModels), view.getId(), MAX_WAIT);
		
		PartialRow row = new PartialRow();
		
		row.setRowId(submissionId);
		row.setValues(ImmutableMap.of(fooColumnModel.getId(), "bar"));
		row.setEtag(submission1.getSubmissionStatus().getEtag());
		
		PartialRowSet rowSet = new PartialRowSet();
		
		rowSet.setRows(Collections.singletonList(row));
		rowSet.setTableId(view.getId());
		
		AppendableRowSetRequest appendRequest = new AppendableRowSetRequest();
		appendRequest.setEntityId(view.getId());
		appendRequest.setToAppend(rowSet);
		
		TableUpdateTransactionRequest transactionRequest = TableModelUtils.wrapInTransactionRequest(appendRequest);
		
		asyncHelper.assertJobResponse(evaluationOwner, transactionRequest, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertEquals(1, response.getResults().size());
			
			EntityUpdateResults updateResults =  (EntityUpdateResults) response.getResults().get(0);
			
			assertNotNull(updateResults.getUpdateResults());
			assertEquals(1, updateResults.getUpdateResults().size());
			
			EntityUpdateResult updateResult = updateResults.getUpdateResults().get(0);
			
			assertEquals(KeyFactory.keyToString(submissionId), updateResult.getEntityId());
			
			assertNull(updateResult.getFailureCode());
			assertNull(updateResult.getFailureMessage());
			
		}, MAX_WAIT);
		
		// Checks that the annotations was added to the submission
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission1.getSubmission().getId());
		
		assertNotNull(status.getSubmissionAnnotations());
		
		assertEquals("bar", AnnotationsV2Utils.getSingleValue(status.getSubmissionAnnotations(), fooColumnModel.getName()));
	}
	
	@Test
	public void testSubmissionViewAnnotationUpdateWithRowSet() throws Exception {
		Folder entity1 = testHelper.createFolder(submitter1, submitter1Project);

		SubmissionBundle submission1 = testHelper.createSubmission(submitter1, evaluation, entity1);
		Long submissionId = KeyFactory.stringToKey(submission1.getSubmissionStatus().getId());
		
		view = createView(evaluationOwner, evaluationProject, evaluation);
		
		List<SubmissionBundle> submissions = ImmutableList.of(submission1);
		
		// Wait for the results
		QueryResultBundle result = assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
		
		// Now add the "foo" column to the model
		List<ColumnModel> columnModels = result.getColumnModels();
		
		ColumnModel fooColumnModel = modelManager.createColumnModel(evaluationOwner, TableModelTestUtils.createColumn(null, "foo", ColumnType.STRING));

		columnModels.add(fooColumnModel);
		
		// Updates the schema
		asyncHelper.setTableSchema(evaluationOwner, TableModelUtils.getIds(columnModels), view.getId(), MAX_WAIT);
		
		Row expectedRow = mapSubmission(evaluationProject, submission1);
		
		expectedRow.setValues(Collections.singletonList(null));
		
		// Select only the foo column, the values are null
		final RowSet currentRowSet = assertSubmissionQueryResults(evaluationOwner, 
				"select " +fooColumnModel.getName()+ " from " + view.getId() + " order by id", 
				submissions, 
				Collections.singletonList(expectedRow))
		.getQueryResult().getQueryResults();
		
		// Add the bar value to the foo column
		currentRowSet.getRows().get(0).setValues(Collections.singletonList("bar"));
		
		AppendableRowSetRequest appendRequest = new AppendableRowSetRequest();
		appendRequest.setEntityId(view.getId());
		appendRequest.setToAppend(currentRowSet);
		
		// Send the update
		TableUpdateTransactionRequest transactionRequest = TableModelUtils.wrapInTransactionRequest(appendRequest);
		
		asyncHelper.assertJobResponse(evaluationOwner, transactionRequest, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResults());
			assertEquals(1, response.getResults().size());
			
			EntityUpdateResults updateResults =  (EntityUpdateResults) response.getResults().get(0);
			
			assertNotNull(updateResults.getUpdateResults());
			assertEquals(1, updateResults.getUpdateResults().size());
			
			EntityUpdateResult updateResult = updateResults.getUpdateResults().get(0);
			
			assertEquals(KeyFactory.keyToString(submissionId), updateResult.getEntityId());
			
			assertNull(updateResult.getFailureCode());
			assertNull(updateResult.getFailureMessage());
			
		}, MAX_WAIT);
		
		// Double check that the annotation was actually updated
		SubmissionStatus status = submissionManager.getSubmissionStatus(evaluationOwner, submission1.getSubmission().getId());
		assertNotNull(status);
		assertEquals("bar", AnnotationsV2Utils.getSingleValue(status.getSubmissionAnnotations(), fooColumnModel.getName()));
		
		submission1.setSubmissionStatus(status);		
		
		// Verifies that the view will be synched with the new value
		assertSubmissionQueryResults(evaluationOwner, "select * from " + view.getId() + " order by id", submissions);
		
	}
	
	private QueryResultBundle assertSubmissionQueryResults(UserInfo user, String query, List<SubmissionBundle> submissions) throws Exception {
		List<Row> expectedRows = mapSubmissions(evaluationProject, submissions);
		
		return assertSubmissionQueryResults(user, query, submissions, expectedRows);
	}
	
	private QueryResultBundle assertSubmissionQueryResults(UserInfo user, String query, List<SubmissionBundle> submissions, List<Row> expectedRows) throws Exception {
		
		// Wait for the updated results
		return asyncHelper.assertQueryResult(evaluationOwner, query, (QueryResultBundle result) -> {
			
			List<Row> rows = result.getQueryResult().getQueryResults().getRows();
			
			assertEquals(submissions.size(), rows.size());
			
			for (int i=0; i<rows.size(); i++) {
				Row row = rows.get(i);
				SubmissionStatus status = submissions.get(i).getSubmissionStatus();
				Long id = KeyFactory.stringToKey(status.getId());
				String etag = status.getEtag();
				
				assertEquals(id, row.getRowId());
				assertEquals(etag, row.getEtag());
			}
			
			assertEquals(expectedRows, result.getQueryResult().getQueryResults().getRows());
			
		}, MAX_WAIT);
	}
	
	private static List<Row> mapSubmissions(Project evaluationProject, List<SubmissionBundle> submissions) {
		return submissions.stream().map((bundle) -> mapSubmission(evaluationProject, bundle)).collect(Collectors.toList());
	}
	
	private static Row mapSubmission(Project evaluationProject, SubmissionBundle bundle) {
		Row row = new Row();
		
		Long id = KeyFactory.stringToKey(bundle.getSubmission().getId());
		String eTag = bundle.getSubmissionStatus().getEtag();
		Long evaluationId = KeyFactory.stringToKey(bundle.getSubmission().getEvaluationId());
		String evaluationRoundId = bundle.getSubmission().getEvaluationRoundId();
		Long versionNumber = bundle.getSubmissionStatus().getStatusVersion();
		
		
		row.setRowId(id);
		row.setEtag(eTag);
		row.setVersionNumber(versionNumber);
		
		List<String> values = new ArrayList<>();
		
		// Default object fields
		values.add(id.toString());
		values.add(bundle.getSubmission().getName());
		values.add(String.valueOf(bundle.getSubmission().getCreatedOn().getTime()));
		values.add(bundle.getSubmission().getUserId());
		values.add(eTag);
		values.add(String.valueOf(bundle.getSubmissionStatus().getModifiedOn().getTime()));
		values.add(evaluationProject.getId());
		
		// Custom submission fields
		values.add(bundle.getSubmissionStatus().getStatus().name());
		values.add(evaluationId.toString());
		values.add(evaluationRoundId);
		values.add(bundle.getSubmission().getTeamId() == null ? bundle.getSubmission().getUserId() : bundle.getSubmission().getTeamId());
		values.add(bundle.getSubmission().getSubmitterAlias());
		values.add(bundle.getSubmission().getEntityId());
		values.add(bundle.getSubmission().getVersionNumber().toString());
		values.add(bundle.getSubmission().getDockerRepositoryName());
		values.add(bundle.getSubmission().getDockerDigest());
		
		
		Annotations annotations = bundle.getSubmissionStatus().getSubmissionAnnotations();
		
		if (annotations != null) {
			annotations.getAnnotations().entrySet().forEach((entry) -> {
				List<String> annotationValues = entry.getValue().getValue();
				
				values.add(annotationValues == null || annotationValues.isEmpty() ? null : annotationValues.iterator().next());
			});
		}
		
		row.setValues(values);
		
		return row;
	}
	
	private SubmissionView createView(UserInfo user, Entity parent, Evaluation ...evaluations) {
		List<String> scope = Stream.of(evaluations).map(Evaluation::getId).collect(Collectors.toList());
		
		return asyncHelper.createSubmissionView(user, UUID.randomUUID().toString(), parent.getId(), scope);
	}
	
}
