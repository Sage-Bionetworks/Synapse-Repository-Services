package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.DatasetItem;
import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
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

	private UserInfo userInfo;
	private Project project;
	private S3FileHandle fileHandle;
	private Dataset dataset;
	private ColumnModel stringColumn;

	@BeforeEach
	public void before() throws Exception {

		tableRowTruthDao.truncateAllRowData();

		testHelper.before();
		userInfo = testHelper.createUser();
		project = testHelper.createProject(userInfo);

		fileHandle = fileHandleDaoHelper.create((f) -> {
			f.setCreatedBy(userInfo.getId().toString());
			f.setFileName("someFile");
			f.setContentSize(123L);
		});

		stringColumn = new ColumnModel();
		stringColumn.setName("aString");
		stringColumn.setColumnType(ColumnType.STRING);
		stringColumn.setMaximumSize(50L);
		stringColumn = columnModelManager.createColumnModel(userInfo, stringColumn);
	}

	@AfterEach
	public void after() {

		tableRowTruthDao.truncateAllRowData();

		testHelper.cleanup();

		if (dataset != null) {
			indexDao.deleteTable(IdAndVersion.parse(dataset.getId()));
		}
	}

	@Disabled
	@Test
	public void testQueryDataset()
			throws AssertionError, AsynchJobFailedException, DatastoreException, InterruptedException {

		String fileId = entityManager.createEntity(userInfo,
				new FileEntity().setName("afile").setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()),
				null);

		Annotations annos = entityManager.getAnnotations(userInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "one", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(userInfo, fileId, annos);

		FileEntity file = entityManager.getEntity(userInfo, fileId, FileEntity.class);
		file.setVersionComment("comment 2");
		file.setVersionLabel("v2");
		final long fileIdLong = KeyFactory.stringToKey(file.getId());
		boolean newVersion = true;
		String activityId = null;
		entityManager.updateEntity(userInfo, file, newVersion, activityId);

		// change the annotation for the new version
		annos = entityManager.getAnnotations(userInfo, fileId);
		AnnotationsV2TestUtils.putAnnotations(annos, stringColumn.getName(), "two", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(userInfo, fileId, annos);

		file = entityManager.getEntity(userInfo, fileId, FileEntity.class);
		assertEquals(2L, file.getVersionNumber());

		asyncHelper.waitForObjectReplication(MainType.ENTITY, KeyFactory.stringToKey(file.getId()), file.getEtag(),
				MAX_WAIT);

		// add both the first and second version of the file to the dataset.s
		List<DatasetItem> items = Arrays.asList(new DatasetItem().setEntityId(file.getId()).setVersionNumber(1L),
				new DatasetItem().setEntityId(file.getId()).setVersionNumber(2L));

		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset().setParentId(project.getId())
				.setName("aDataset").setColumnIds(Arrays.asList(stringColumn.getId())).setItems(items));
		
		final String fileEtag = file.getEtag();
		
		// call under test
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId() + " ORDER BY ROW_VERSION ASC",
				(QueryResultBundle result) -> {
					List<Row> rows = result.getQueryResult().getQueryResults().getRows();
					assertEquals(2, rows.size());
					// one
					assertEquals(new Row().setRowId(fileIdLong).setVersionNumber(1L).setEtag(fileEtag)
							.setValues(Arrays.asList("one")), rows.get(0));
					// two
					assertEquals(new Row().setRowId(fileIdLong).setVersionNumber(2L).setEtag(fileEtag)
							.setValues(Arrays.asList("two")), rows.get(0));
				}, MAX_WAIT);
	}

}
