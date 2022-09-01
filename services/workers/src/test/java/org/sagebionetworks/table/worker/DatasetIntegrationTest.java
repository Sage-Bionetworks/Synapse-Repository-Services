package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SumFileSizes;
import org.sagebionetworks.repo.model.table.TableSchemaChangeRequest;
import org.sagebionetworks.repo.model.table.TableSchemaChangeResponse;
import org.sagebionetworks.repo.model.table.TableUpdateRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DatasetIntegrationTest {

	private static final int MAX_WAIT = 1 * 60 * 1000;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	@Autowired
	private TestHelper testHelper;
	@Autowired
	private TableIndexDAO indexDao;
	@Autowired
	private TableRowTruthDAO tableRowTruthDao;
	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private DownloadListManager downloadListManager;
	
	private UserInfo userInfo;
	private Project project;
	private Dataset dataset;
	private ColumnModel stringColumn;
	private Random random;

	@BeforeEach
	public void before() throws Exception {

		tableRowTruthDao.truncateAllRowData();
		fileHandleDao.truncateTable();

		testHelper.before();
		userInfo = testHelper.createUser();
		project = testHelper.createProject(userInfo);

		stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(50L);
		stringColumn = columnModelManager.createColumnModel(userInfo, stringColumn);
		random = new Random();
	}

	@AfterEach
	public void after() {

		downloadListManager.clearDownloadList(userInfo);
		tableRowTruthDao.truncateAllRowData();
		
		testHelper.cleanup();

		if (dataset != null) {
			indexDao.deleteTable(IdAndVersion.parse(dataset.getId()));
		}
		
		fileHandleDao.truncateTable();
	}

	@Test
	public void testQueryDataset()
			throws AssertionError, AsynchJobFailedException, DatastoreException, InterruptedException {

		int numberOfVersions = 3;
		FileEntity fileOne = createFileWithMultipleVersions(1, stringColumn.getName(), numberOfVersions);
		FileEntity fileTwo = createFileWithMultipleVersions(2, stringColumn.getName(), numberOfVersions);
		FileEntity fileThree = createFileWithMultipleVersions(3, stringColumn.getName(), numberOfVersions);
		
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileOne.getId()),
				fileOne.getEtag(), MAX_WAIT);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileTwo.getId()),
				fileTwo.getEtag(), MAX_WAIT);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileThree.getId()),
				fileThree.getEtag(), MAX_WAIT);

		// add one version from each file
		List<EntityRef> items = Arrays.asList(
				new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(1L),
				new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(2L),
				new EntityRef().setEntityId(fileThree.getId()).setVersionNumber(3L)
				);

		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset().setParentId(project.getId())
				.setName("aDataset").setColumnIds(Arrays.asList(stringColumn.getId())).setItems(items));


		// call under test
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId() + " ORDER BY ROW_VERSION ASC",
				(QueryResultBundle result) -> {
					List<Row> rows = result.getQueryResult().getQueryResults().getRows();
					assertEquals(3, rows.size());
					// one
					assertEquals(new Row().setRowId(KeyFactory.stringToKey(fileOne.getId())).setVersionNumber(1L).setEtag(fileOne.getEtag())
							.setValues(Arrays.asList("v-1")), rows.get(0));
					// two
					assertEquals(new Row().setRowId(KeyFactory.stringToKey(fileTwo.getId())).setVersionNumber(2L).setEtag(fileTwo.getEtag())
							.setValues(Arrays.asList("v-2")), rows.get(1));
					// three
					assertEquals(new Row().setRowId(KeyFactory.stringToKey(fileThree.getId())).setVersionNumber(3L).setEtag(fileThree.getEtag())
							.setValues(Arrays.asList("v-3")), rows.get(2));
				}, MAX_WAIT);
	}
	
	// Reproduce PLFM-7025
	@Test
	public void testQueryDatasetWithFileSize()
			throws AssertionError, AsynchJobFailedException, DatastoreException, InterruptedException {

		int numberOfVersions = 2;
		
		FileEntity fileOne = createFileWithMultipleVersions(1, stringColumn.getName(), numberOfVersions);
		FileEntity fileTwo = createFileWithMultipleVersions(2, stringColumn.getName(), numberOfVersions);
		
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileOne.getId()),
				fileOne.getEtag(), MAX_WAIT);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileTwo.getId()),
				fileTwo.getEtag(), MAX_WAIT);
		
		// add one version from each file
		List<EntityRef> items = Arrays.asList(
				new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(1L),
				new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(2L)
		);
		
		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset().setParentId(project.getId())
				.setName("aDataset").setColumnIds(Arrays.asList(stringColumn.getId())).setItems(items));
		
		long sumOfSizes = items.stream().mapToLong(item -> {
			String fileHandleId = entityManager.getFileHandleIdForVersion(userInfo, item.getEntityId(), item.getVersionNumber());
			return fileHandleDao.get(fileHandleId).getContentSize();
		}).sum();
		
		SumFileSizes expectedSumFiles = new SumFileSizes()
				.setSumFileSizesBytes(sumOfSizes)
				.setGreaterThan(false);
		
		Query query = new Query();
		query.setSql("SELECT * FROM " + dataset.getId() + " ORDER BY ROW_VERSION ASC");
		query.setIncludeEntityEtag(true);
		
		QueryOptions options = new QueryOptions()
				.withRunQuery(true)
				.withRunSumFileSizes(true);
		
		asyncHelper.assertQueryResult(userInfo, query, options, (QueryResultBundle result) -> {
			assertEquals(expectedSumFiles, result.getSumFileSizes());
		}, MAX_WAIT);
	}
	
	// Test for PLFM-7034
	@Test
	public void testAddDatasetQueryToDownloadList() throws DatastoreException, InterruptedException, AssertionError, AsynchJobFailedException {
		int numberOfVersions = 2;
		
		FileEntity fileOne = createFileWithMultipleVersions(1, stringColumn.getName(), numberOfVersions);
		FileEntity fileTwo = createFileWithMultipleVersions(2, stringColumn.getName(), numberOfVersions);
		
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileOne.getId()),
				fileOne.getEtag(), MAX_WAIT);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileTwo.getId()),
				fileTwo.getEtag(), MAX_WAIT);
		
		// add one version from each file
		List<EntityRef> items = Arrays.asList(
				new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(1L),
				new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(2L)
		);
		
		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset()
				.setParentId(project.getId())
				.setName("aDataset")
				.setColumnIds(Arrays.asList(stringColumn.getId()))
				.setItems(items));
		
		
		// Query the dataset
		Query query = new Query();
		query.setSql("select * from " + dataset.getId());
		query.setIncludeEntityEtag(true);
		
		List<Row> expectedRows = Arrays.asList(
			new Row().setRowId(KeyFactory.stringToKey(fileOne.getId())).setVersionNumber(1L).setEtag(fileOne.getEtag()).setValues(Arrays.asList("v-1")),
			new Row().setRowId(KeyFactory.stringToKey(fileTwo.getId())).setVersionNumber(2L).setEtag(fileTwo.getEtag()).setValues(Arrays.asList("v-2"))
		);
		
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(expectedRows, result.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT);
		
		AddToDownloadListRequest addToDownloadListrequest = new AddToDownloadListRequest()
				.setQuery(query)
				.setUseVersionNumber(true);
		
		// Call under test
		asyncHelper.assertJobResponse(userInfo, addToDownloadListrequest, (AddToDownloadListResponse response) -> {
			assertEquals(items.size(), response.getNumberOfFilesAdded());
		}, MAX_WAIT);
		
		DownloadListQueryRequest downloadListQueryRequest = new DownloadListQueryRequest().setRequestDetails(new AvailableFilesRequest()); 
		
		asyncHelper.assertJobResponse(userInfo, downloadListQueryRequest, (DownloadListQueryResponse response) -> {
			List<DownloadListItemResult> downloadListItems = ((AvailableFilesResponse)response.getResponseDetails()).getPage();
			assertEquals(items.size(), downloadListItems.size());
			for (int i=0; i<items.size(); i++) {
				EntityRef item = items.get(i);
				DownloadListItemResult downloadItem = downloadListItems.get(i);
				assertEquals(item.getEntityId(), downloadItem.getFileEntityId());
				assertEquals(item.getVersionNumber(), downloadItem.getVersionNumber());
			}
		}, MAX_WAIT);
	}

	@Test
	public void testAddDatasetQueryToDownloadListWithoutVersions() throws DatastoreException, InterruptedException, AssertionError, AsynchJobFailedException {
		int numberOfVersions = 2;
		
		FileEntity fileOne = createFileWithMultipleVersions(1, stringColumn.getName(), numberOfVersions);
		FileEntity fileTwo = createFileWithMultipleVersions(2, stringColumn.getName(), numberOfVersions);
		
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileOne.getId()),
				fileOne.getEtag(), MAX_WAIT);
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileTwo.getId()),
				fileTwo.getEtag(), MAX_WAIT);
		
		// add one version from each file
		List<EntityRef> items = Arrays.asList(
			new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(1L),
			new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(2L)
		);
		
		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset()
			.setParentId(project.getId())
			.setName("aDataset")
			.setColumnIds(Arrays.asList(stringColumn.getId()))
			.setItems(items));
		
		
		// Query the dataset
		Query query = new Query();
		query.setSql("select * from " + dataset.getId());
		query.setIncludeEntityEtag(true);
		
		List<Row> expectedRows = Arrays.asList(
			new Row().setRowId(KeyFactory.stringToKey(fileOne.getId())).setVersionNumber(1L).setEtag(fileOne.getEtag()).setValues(Arrays.asList("v-1")),
			new Row().setRowId(KeyFactory.stringToKey(fileTwo.getId())).setVersionNumber(2L).setEtag(fileTwo.getEtag()).setValues(Arrays.asList("v-2"))
		);
		
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(expectedRows, result.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT);
		
		AddToDownloadListRequest addToDownloadListrequest = new AddToDownloadListRequest()
				.setQuery(query)
				.setUseVersionNumber(false);
		
		// Call under test
		asyncHelper.assertJobResponse(userInfo, addToDownloadListrequest, (AddToDownloadListResponse response) -> {
			assertEquals(items.size(), response.getNumberOfFilesAdded());
		}, MAX_WAIT);
		
		DownloadListQueryRequest downloadListQueryRequest = new DownloadListQueryRequest().setRequestDetails(new AvailableFilesRequest()); 
		
		asyncHelper.assertJobResponse(userInfo, downloadListQueryRequest, (DownloadListQueryResponse response) -> {
			List<DownloadListItemResult> downloadListItems = ((AvailableFilesResponse)response.getResponseDetails()).getPage();
			assertEquals(items.size(), downloadListItems.size());
			for (int i=0; i<items.size(); i++) {
				EntityRef item = items.get(i);
				DownloadListItemResult downloadItem = downloadListItems.get(i);
				assertEquals(item.getEntityId(), downloadItem.getFileEntityId());
				assertNull(downloadItem.getVersionNumber());
			}
		}, MAX_WAIT);
	}


	/**
	 * Create File entity with multiple versions using the annotations for each version.
	 *
	 * @return
	 */
	private FileEntity createFileWithMultipleVersions(int fileNumber, String annotationKey, int numberOfVersions) {
		List<Annotations> annotations = new ArrayList<>(numberOfVersions);
		for(int i=1; i <= numberOfVersions; i++) {
			Annotations annos = new Annotations();
			AnnotationsV2TestUtils.putAnnotations(annos, annotationKey, "v-"+i, AnnotationsValueType.STRING);
			annotations.add(annos);
		}

		// create the entity
		String fileEntityId = null;		
		int version = 1;
		
		for(Annotations annos: annotations) {
						
			long fileContentSize = (long) random.nextInt(128_000);
			
			// Create a new file handle for each version
			S3FileHandle fileHandle = fileHandleDaoHelper.create((f) -> {
				f.setCreatedBy(userInfo.getId().toString());
				f.setFileName("someFile");
				f.setContentSize(fileContentSize);
			});
			
			if (fileEntityId == null) {
				fileEntityId = entityManager.createEntity(userInfo, new FileEntity()
						.setName("afile-"+fileNumber)
						.setParentId(project.getId())
						.setDataFileHandleId(fileHandle.getId()),
						null);
			} else {
				// create a new version for the entity
				FileEntity entity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
				entity.setVersionComment("c-"+version);
				entity.setVersionLabel("v-"+version);
				entity.setDataFileHandleId(fileHandle.getId());
				boolean newVersion = true;
				String activityId = null;
				entityManager.updateEntity(userInfo, entity, newVersion, activityId);
			}
			// get the ID and etag
			FileEntity entity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
			annos.setId(entity.getId());
			annos.setEtag(entity.getEtag());
			entityManager.updateAnnotations(userInfo, fileEntityId, annos);
			version++;
		}
		
		return entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
	}

	@Test
	public void testSchemaUpdateTransaction() throws AssertionError, AsynchJobFailedException, DatastoreException, InterruptedException {
		FileEntity fileOne = createFileWithMultipleVersions(1, stringColumn.getName(), 1);
		List<EntityRef> items = Arrays.asList(
				new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(1L)
		);

		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileOne.getId()),
				fileOne.getEtag(), MAX_WAIT);

		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset().setParentId(project.getId())
				.setName("aDataset").setColumnIds(Arrays.asList(stringColumn.getId())).setItems(items));


		// Create a new ColumnModel
		ColumnModel newColumn = new ColumnModel();
		newColumn.setName("aNewColumnName");
		newColumn.setColumnType(ColumnType.STRING);
		newColumn.setMaximumSize(75L);
		newColumn = columnModelManager.createColumnModel(userInfo, newColumn);

		// Create a transaction request to update the schema
		// Replace the old column with the new column
		TableSchemaChangeRequest schemaChangeRequest = new TableSchemaChangeRequest();
		ColumnChange change = new ColumnChange();
		change.setOldColumnId(stringColumn.getId());
		change.setNewColumnId(newColumn.getId());
		schemaChangeRequest.setChanges(Collections.singletonList(change));

		List<TableUpdateRequest> changes = Collections.singletonList(schemaChangeRequest);

		TableUpdateTransactionRequest request = new TableUpdateTransactionRequest();
		request.setEntityId(dataset.getId());
		request.setChanges(changes);

		List<ColumnModel> expectedSchema = Collections.singletonList(newColumn);

		// call under test
		asyncHelper.assertJobResponse(userInfo,	request,
				(TableUpdateTransactionResponse result) -> {
					TableSchemaChangeResponse schemaChangeResponse = (TableSchemaChangeResponse) result.getResults().get(0);
					assertEquals(expectedSchema, schemaChangeResponse.getSchema());
				},
				MAX_WAIT
		);
	}
	
	// Reproduce https://sagebionetworks.jira.com/browse/PLFM-7076
	@Test
	public void testQueryDatasetWithNoItems() throws AssertionError, AsynchJobFailedException {
		
		List<EntityRef> items = null;
		
		Dataset dataset = asyncHelper.createDataset(userInfo, 
			new Dataset().setParentId(project.getId())
				.setName("aDataset")
				.setColumnIds(Arrays.asList(stringColumn.getId()))
				.setItems(items)
		);
		
		// Query the dataset
		Query query = new Query();
		query.setSql("select * from " + dataset.getId());
		query.setIncludeEntityEtag(true);
		
		List<Row> expectedRows = Collections.emptyList();
		
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(expectedRows, result.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT);
	}
	
	// Reproduce https://sagebionetworks.jira.com/browse/PLFM-7053
	@Test
	public void testDatasetSnapshotWithNoItems() throws AssertionError, AsynchJobFailedException {
		
		List<EntityRef> items = null;
		
		Dataset dataset = asyncHelper.createDataset(userInfo, 
			new Dataset().setParentId(project.getId())
				.setName("aDataset")
				.setColumnIds(Arrays.asList(stringColumn.getId()))
				.setItems(items)
		);
		
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(0, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT);
				
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotComment("Dataset snapshot");

		// Add all of the parts
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(dataset.getId());
		transactionRequest.setCreateSnapshot(true);
		transactionRequest.setSnapshotOptions(snapshotOptions);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			asyncHelper.assertJobResponse(userInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {
				fail("Creating a snapshot for a dataset with no items should throw an exception");
			}, MAX_WAIT);
		}).getMessage();
		
		assertEquals("You cannot create a version of an empty Dataset. Add files to this Dataset before creating a version.", errorMessage);
	}

}
