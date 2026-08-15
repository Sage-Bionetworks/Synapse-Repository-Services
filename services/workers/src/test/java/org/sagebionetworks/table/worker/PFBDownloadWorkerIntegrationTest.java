package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.avro.file.SeekableFileInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.AmazonS3Utility;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadPFBRequest;
import org.sagebionetworks.repo.model.table.DownloadPFBResult;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.avro.RowPFBReader;
import org.sagebionetworks.table.cluster.avro.RowPFBReader.PFBRow;
import org.sagebionetworks.table.cluster.avro.RowPFBUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PFBDownloadWorkerIntegrationTest {
	public static final int MAX_WAIT_MS = 1000 * 60;

	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ColumnModelManager columnManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private SemaphoreManager semphoreManager;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	@Autowired
	private AmazonS3Utility amazonS3Utility;

	private UserInfo adminUserInfo;
	private String tableId;
	private List<S3FileHandle> fileHandles;

	@BeforeEach
	public void before() throws NotFoundException {
		fileHandles = new ArrayList<>();
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true, BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID));
		asynchJobStatusManager.emptyAllQueues();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	@AfterEach
	public void after() {
		entityManager.truncateAll();
		for (S3FileHandle handle : fileHandles) {
			fileHandleDao.delete(handle.getId());
			amazonS3Utility.deleteFromS3(handle.getKey());
		}
	}

	@Test
	public void testRoundTrip() throws Exception {

		String projectId = entityManager.createEntity(adminUserInfo, new Project().setName("test"), null);
		
		List<ColumnModel> schema = List.of(
			new ColumnModel().setName("aString").setColumnType(ColumnType.STRING),
			new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("aBoolean").setColumnType(ColumnType.BOOLEAN)
		);
		
		schema = columnManager.createColumnModels(adminUserInfo, schema);
		
		List<String> colIds = schema.stream().map(c -> c.getId()).collect(Collectors.toList());

		TableEntity table = asyncHelper.createTable(adminUserInfo, "testTable", projectId, colIds, false);
		
		List<Row> rows = List.of(
			new Row().setValues(List.of("row1", "9090", "true")),
			new Row().setValues(List.of("row2", "9091", "false"))
		);

		List<RowReference> rowRef = asyncHelper.appendRowsToTable(adminUserInfo, schema, table.getId(), rows, MAX_WAIT_MS)
			.getRowReferenceSet().getRows();
		
		IntStream.range(0, rows.size()).forEach(i -> {
			rows.get(i).setRowId(rowRef.get(i).getRowId());
			rows.get(i).setVersionNumber(rowRef.get(i).getVersionNumber());
		});

		DownloadPFBRequest request = new DownloadPFBRequest();
		
		// Basic request, the ROW_ID_ROW_VERSION will be used as the PFB entity id
		request.setSql("select * from " + table.getId());
		request.setPfbEntityName("testing");
		request.setEntityId(tableId);

		// call under test
		DownloadPFBResult result = asyncHelper.assertJobResponse(adminUserInfo, request, (DownloadPFBResult response) -> {
			assertNotNull(response);
			assertNotNull(response.getResultsFileHandleId());
		}, MAX_WAIT_MS).getResponse();

		S3FileHandle fileHandle = (S3FileHandle) fileHandleDao.get(result.getResultsFileHandleId());
		
		fileHandles.add(fileHandle);
		
		List<PFBRow> results = readPFBFromS3(fileHandle.getKey());

		List<PFBRow> expected = rows.stream()
			.map(r -> new PFBRow(RowPFBUtils.createEntityIdFromRowId(r), r.getValues()))
			.collect(Collectors.toList());

		assertEquals(results, expected);
		
		// Now try with a custom entity id that uses two (aliased) columns
		request.setSql("select aString as version, anInt as id, aBoolean from " + table.getId());
		request.setPfbEntityIdColumnNames(List.of("id", "version"));
		
		result = asyncHelper.assertJobResponse(adminUserInfo, request, (DownloadPFBResult response) -> {
			assertNotNull(response);
			assertNotNull(response.getResultsFileHandleId());
		}, MAX_WAIT_MS).getResponse();
		
		fileHandle = (S3FileHandle) fileHandleDao.get(result.getResultsFileHandleId());
		
		fileHandles.add(fileHandle);
		
		results = readPFBFromS3(fileHandle.getKey());
		
		expected = rows.stream()
			.map(r -> new PFBRow(RowPFBUtils.createEntityIdFromColumns(r.getValues(), new int[] {1, 0}), r.getValues()))
			.collect(Collectors.toList());

		assertEquals(results, expected);
	}

	/**
	 * Helper to download and read PFB avro file from S3.
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	List<PFBRow> readPFBFromS3(String key) throws IOException {
		File resultFile = amazonS3Utility.downloadFromS3(key);
		try {
			// Read
			List<PFBRow> result = new ArrayList<>();
			try (RowPFBReader reader = new RowPFBReader(new SeekableFileInput(resultFile))) {
				while (reader.hasNext()) {
					result.add(reader.next());
				}
			}
			return result;
		} finally {
			resultFile.delete();
		}
	}

}
