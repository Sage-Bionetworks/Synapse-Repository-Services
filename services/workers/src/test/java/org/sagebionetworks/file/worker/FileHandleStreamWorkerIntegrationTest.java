package org.sagebionetworks.file.worker;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.dbo.FileMetadataUtils;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileHandleStreamWorkerIntegrationTest {
	
	private static final int TIMEOUT = 2 * 60 * 1000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private AthenaSupport athenaSupport;
	
	private UserInfo user;
	
	private String fileHandleId;

	@BeforeEach
	public void before() {
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		fileHandleDao.truncateTable();

		// We need to set an old timestamp manually so that the file handle is not filtered out
		Timestamp createdOn = Timestamp.from(Instant.now().minus(FileHandleStreamWorker.UPDATED_ON_DAYS_FILTER + 1, ChronoUnit.DAYS));
		
		fileHandleId = idGenerator.generateNewId(IdType.FILE_IDS).toString();
		
		DBOFileHandle handle = FileMetadataUtils.createDBOFromDTO(TestUtils.createS3FileHandle(user.getId().toString(), fileHandleId));
		
		handle.setCreatedOn(createdOn);
		handle.setUpdatedOn(createdOn);
		
		fileHandleDao.createBatchDbo(Collections.singletonList(handle));
	}
 
	@Test
	public void testWorker() throws Exception {
		
		Database dataBase = athenaSupport.getDatabase("firehoseLogs");
		Table table = athenaSupport.getTable(dataBase, "fileHandleDataRecords");
		
		String query = "SELECT COUNT(*) FROM " + table.getName() + " WHERE id = " + fileHandleId;
		
		TimeUtils.waitFor(TIMEOUT, 5000, () -> {
			
			AthenaQueryResult<Long> q = athenaSupport.executeQuery(dataBase, query, (row) -> {
				return Long.valueOf(row.getData().get(0).getVarCharValue());
			});
			
			Iterator<Long> it = q.getQueryResultsIterator();
			
			Long count = 0L;
			
			if (it.hasNext()) {
				count = q.getQueryResultsIterator().next();
			} else {
				throw new IllegalStateException("No results from Athena, expected 1");
			}
			
			return new Pair<>(count >= 1, null);
		});
	}

}
