package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModelMapper;
import org.sagebionetworks.repo.manager.table.metadata.providers.DatasetCollectionMetadataIndexProvider;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetCollection;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.worker.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DatasetCollectionIntegrationTest {

	private static final int MAX_WAIT = 80_000;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;
	@Autowired
	private TestHelper testHelper;
	@Autowired
	private TableRowTruthDAO tableRowTruthDao;
	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private DefaultColumnModelMapper defaultColumnMapper;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private DatasetCollectionMetadataIndexProvider provider;
	
	private UserInfo userInfo;
	private Project project;
	private ColumnModel stringColumn;
	private List<String> defaultColumnIdList = new ArrayList<>();

	@BeforeEach
	public void before() throws Exception {

		tableRowTruthDao.truncateAllRowData();
		fileHandleDao.truncateTable();

		testHelper.before();
		userInfo = testHelper.createUser();
		project = testHelper.createProject(userInfo);

		stringColumn = new ColumnModel();
		stringColumn.setName("string_column");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(50L);
		stringColumn = columnModelManager.createColumnModel(userInfo, stringColumn);

		DefaultColumnModel defaultColumnModel = provider.getDefaultColumnModel(null);
		List<ColumnModel> columnModels = defaultColumnMapper.map(defaultColumnModel).stream().filter(c->{
			return !ObjectField.path.name().equals(c.getName());
		}).collect(Collectors.toList());
		defaultColumnIdList = columnModels.stream().map(ColumnModel::getId).collect(Collectors.toList());
	}

	@AfterEach
	public void after() {

		tableRowTruthDao.truncateAllRowData();
		
		testHelper.cleanup();
		
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testCreateAndQueryDatasetCollection() throws Exception {
		Dataset datasetOne = createDatasetAndSnapshot();
		Dataset datasetTwo = createDatasetAndSnapshot();

		Long snapshotVersion = 1L;
		DatasetCollection collection = asyncHelper.createDatasetCollection(userInfo, new DatasetCollection()
			.setParentId(project.getId())
			.setName("Dataset Collection")
				.setColumnIds(defaultColumnIdList)
			.setItems(List.of(
				new EntityRef().setEntityId(datasetOne.getId()).setVersionNumber(snapshotVersion),
				new EntityRef().setEntityId(datasetTwo.getId()).setVersionNumber(snapshotVersion)
			)));

		List<Row> expectedRows = Arrays.asList(
			new Row().setRowId(KeyFactory.stringToKey(datasetOne.getId())).setVersionNumber(snapshotVersion)
					.setEtag(datasetOne.getEtag()).setValues(Arrays.asList(datasetOne.getId(), datasetOne.getName(),
							datasetOne.getDescription(), Long.toString(datasetOne.getCreatedOn().getTime()), datasetOne.getCreatedBy(),
							datasetOne.getEtag(), Long.toString(datasetOne.getModifiedOn().getTime()), datasetOne.getModifiedBy(),
							datasetOne.getSize().toString(), datasetOne.getChecksum(), datasetOne.getCount().toString())),
				new Row().setRowId(KeyFactory.stringToKey(datasetTwo.getId())).setVersionNumber(snapshotVersion)
						.setEtag(datasetTwo.getEtag()).setValues(Arrays.asList(datasetTwo.getId(), datasetTwo.getName(),
								datasetTwo.getDescription(), Long.toString(datasetTwo.getCreatedOn().getTime()), datasetTwo.getCreatedBy(),
								datasetTwo.getEtag(), Long.toString(datasetTwo.getModifiedOn().getTime()), datasetTwo.getModifiedBy(),
								datasetTwo.getSize().toString(), datasetTwo.getChecksum(), datasetTwo.getCount().toString()))
		);

		// call under test
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + collection.getId() + " ORDER BY ROW_VERSION ASC", (QueryResultBundle result) -> {
			assertEquals(expectedRows, result.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT);

	}

	@Test
	public void testCreateAndQueryDatasetCollectionHavingDatasetWithNoItem() throws Exception {
		Dataset datasetOne = asyncHelper.createDataset(userInfo, new Dataset()
				.setParentId(project.getId())
				.setName(UUID.randomUUID().toString())
				.setDescription(UUID.randomUUID().toString())
				.setColumnIds(Collections.singletonList(stringColumn.getId()))
				.setItems(Collections.emptyList())
		);

		Long snapshotVersion = 1L;

 		DatasetCollection collection = asyncHelper.createDatasetCollection(userInfo, new DatasetCollection()
				.setParentId(project.getId())
				.setName("Dataset Collection")
				.setColumnIds(defaultColumnIdList)
				.setItems(List.of(
						new EntityRef().setEntityId(datasetOne.getId()).setVersionNumber(snapshotVersion)
				)));

		List<Row> expectedRows = Arrays.asList(
				new Row().setRowId(KeyFactory.stringToKey(datasetOne.getId())).setVersionNumber(snapshotVersion)
						.setEtag(datasetOne.getEtag()).setValues(Arrays.asList(datasetOne.getId(), datasetOne.getName(),
								datasetOne.getDescription(), Long.toString(datasetOne.getCreatedOn().getTime()), datasetOne.getCreatedBy(),
								datasetOne.getEtag(), Long.toString(datasetOne.getModifiedOn().getTime()), datasetOne.getModifiedBy(),
								"0", null, "0"))
		);

		// call under test
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + collection.getId() + " ORDER BY ROW_VERSION ASC", (QueryResultBundle result) -> {
			assertEquals(expectedRows, result.getQueryResult().getQueryResults().getRows());
		}, MAX_WAIT);

	}
	
	private Dataset createDatasetAndSnapshot() throws Exception {
		FileEntity fileOne = createFileEntityAndWaitForReplication();
		FileEntity fileTwo = createFileEntityAndWaitForReplication();
		
		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset()
			.setParentId(project.getId())
			.setName(UUID.randomUUID().toString())
			.setDescription(UUID.randomUUID().toString())
			.setColumnIds(Arrays.asList(stringColumn.getId()))
			.setItems(List.of(
				new EntityRef().setEntityId(fileOne.getId()).setVersionNumber(fileOne.getVersionNumber()),
				new EntityRef().setEntityId(fileTwo.getId()).setVersionNumber(fileTwo.getVersionNumber())
			))
		);
		
		Annotations annotations = new Annotations()
				.setId(dataset.getId())
				.setEtag(dataset.getEtag());
		
		AnnotationsV2TestUtils.putAnnotations(annotations, stringColumn.getName(), dataset.getId(), AnnotationsValueType.STRING);
		
		entityManager.updateAnnotations(userInfo, dataset.getId(), annotations);
		
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(dataset.getItems().size(), result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT);
				
		SnapshotRequest snapshotOptions = new SnapshotRequest();
		snapshotOptions.setSnapshotComment("Dataset snapshot");

		// Add all of the parts
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		
		transactionRequest.setEntityId(dataset.getId());
		transactionRequest.setCreateSnapshot(true);
		transactionRequest.setSnapshotOptions(snapshotOptions);
		
		asyncHelper.assertJobResponse(userInfo, transactionRequest, (TableUpdateTransactionResponse response) -> {
			assertEquals(1L, response.getSnapshotVersionNumber());
		}, MAX_WAIT).getResponse().getSnapshotVersionNumber();

		// Etag is not available for snapshot version.To get etag, need the latest version of dataset
		Dataset datasetSnapshot = entityManager.getEntityForVersion(userInfo, dataset.getId(), dataset.getVersionNumber(), Dataset.class);
		Dataset latestDataset = entityManager.getEntity(userInfo, dataset.getId(), Dataset.class);
		datasetSnapshot.setEtag(latestDataset.getEtag());

		return datasetSnapshot;
	}
	
	private FileEntity createFileEntityAndWaitForReplication() throws Exception {
		String fileEntityId = entityManager.createEntity(userInfo, new FileEntity()
			.setName(UUID.randomUUID().toString())
			.setParentId(project.getId())
			.setDataFileHandleId(fileHandleDaoHelper.create((f) -> {
				f.setCreatedBy(userInfo.getId().toString());
				f.setFileName(UUID.randomUUID().toString());
				f.setContentSize(128_000L);
			}).getId()),
			null);
			
		FileEntity fileEntity = entityManager.getEntity(userInfo, fileEntityId, FileEntity.class);
		
		asyncHelper.waitForObjectReplication(ReplicationType.ENTITY, KeyFactory.stringToKey(fileEntity.getId()), fileEntity.getEtag(), MAX_WAIT);
		
		return fileEntity;
	}
	
}
