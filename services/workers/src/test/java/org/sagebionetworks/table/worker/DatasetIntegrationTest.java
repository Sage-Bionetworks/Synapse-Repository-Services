package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
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

		int numberOfVersions = 3;
		String fileOne = createFileWithMultipleVersions(1, stringColumn.getName(), numberOfVersions);
		String fileTwo = createFileWithMultipleVersions(2, stringColumn.getName(), numberOfVersions);
		String fileThree = createFileWithMultipleVersions(3, stringColumn.getName(), numberOfVersions);


		// add one version from each file
		List<DatasetItem> items = Arrays.asList(
				new DatasetItem().setEntityId(fileOne).setVersionNumber(1L),
				new DatasetItem().setEntityId(fileTwo).setVersionNumber(2L),
				new DatasetItem().setEntityId(fileThree).setVersionNumber(3L)
				);

		Dataset dataset = asyncHelper.createDataset(userInfo, new Dataset().setParentId(project.getId())
				.setName("aDataset").setColumnIds(Arrays.asList(stringColumn.getId())).setItems(items));
		
		
		// call under test
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId() + " ORDER BY ROW_VERSION ASC",
				(QueryResultBundle result) -> {
					List<Row> rows = result.getQueryResult().getQueryResults().getRows();
					assertEquals(3, rows.size());
					// one
					assertEquals(new Row().setRowId(KeyFactory.stringToKey(fileOne)).setVersionNumber(1L).setEtag("")
							.setValues(Arrays.asList("one")), rows.get(0));
					// two
					assertEquals(new Row().setRowId(KeyFactory.stringToKey(fileTwo)).setVersionNumber(2L).setEtag("")
							.setValues(Arrays.asList("two")), rows.get(0));
					// three
					assertEquals(new Row().setRowId(KeyFactory.stringToKey(fileTwo)).setVersionNumber(3L).setEtag("")
							.setValues(Arrays.asList("two")), rows.get(0));
				}, MAX_WAIT);
	}
	
	
	/**
	 * Create File entity with multiple versions using the annotations for each version.
	 * 
	 * @param annotationVersion
	 * @return
	 */
	public String createFileWithMultipleVersions(int fileNumber, String annotationKey, int numberOfVersions) {
		List<Annotations> annotations = new ArrayList<>(numberOfVersions);
		for(int i=1; i <= numberOfVersions; i++) {
			Annotations annos = new Annotations();
			AnnotationsV2TestUtils.putAnnotations(annos, annotationKey, "v-"+i, AnnotationsValueType.STRING);
			annotations.add(annos);
		}
		
		// create the entity
		String fileId = null;
		int version = 1;
		for(Annotations annos: annotations) {
			if(fileId == null) {
				// create the entity
				fileId = entityManager.createEntity(userInfo,
						new FileEntity().setName("afile-"+fileNumber).setParentId(project.getId()).setDataFileHandleId(fileHandle.getId()),
						null);
			}else {
				// create a new version for the entity
				FileEntity entity = entityManager.getEntity(userInfo, fileId, FileEntity.class);
				entity.setVersionComment("c-"+version);
				entity.setVersionLabel("v-"+version);
				boolean newVersion = true;
				String activityId = null;
				entityManager.updateEntity(userInfo, entity, newVersion, activityId);
			}
			// get the ID and etag
			FileEntity entity = entityManager.getEntity(userInfo, fileId, FileEntity.class);
			annos.setId(entity.getId());
			annos.setEtag(entity.getEtag());
			entityManager.updateAnnotations(userInfo, fileId, annos);
			version++;
		}
		return fileId;
	}

}
