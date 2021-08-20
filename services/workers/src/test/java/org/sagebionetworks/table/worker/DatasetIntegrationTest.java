package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	private static final int MAX_WAIT = 1 * 60 * 1000*10;

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

	private UserInfo userInfo;
	private Project project;
	private FileEntity file;
	private Dataset dataset;

	@BeforeEach
	public void before() throws Exception {

		tableRowTruthDao.truncateAllRowData();

		testHelper.before();
		userInfo = testHelper.createUser();
		project = testHelper.createProject(userInfo);

		S3FileHandle fh = fileHandleDaoHelper.create((f) -> {
			f.setCreatedBy(userInfo.getId().toString());
			f.setFileName("someFile");
			f.setContentSize(123L);
		});
		file = testHelper.createEntity(userInfo,
				new FileEntity().setName("afile").setParentId(project.getId()).setDataFileHandleId(fh.getId()));
		asyncHelper.waitForObjectReplication(MainType.ENTITY, KeyFactory.stringToKey(file.getId()), file.getEtag(), MAX_WAIT);
	}

	@AfterEach
	public void after() {

		tableRowTruthDao.truncateAllRowData();

		testHelper.cleanup();

		if (dataset != null) {
			indexDao.deleteTable(IdAndVersion.parse(dataset.getId()));
		}
	}

	@Test
	public void testQueryDataset() throws AssertionError, AsynchJobFailedException, DatastoreException, InterruptedException {
		List<DatasetItem> items = Arrays
				.asList(new DatasetItem().setEntityId(file.getId()).setVersionNumber(file.getVersionNumber()));
		Dataset dataset = asyncHelper.createDataset(userInfo, "aDataset", project.getId(), items);
		// call under test
		asyncHelper.assertQueryResult(userInfo, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			List<Row> rows = result.getQueryResult().getQueryResults().getRows();
			assertEquals(1, rows.size());
		}, MAX_WAIT);
	}

}
