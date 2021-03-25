package org.sagebionetworks.file.worker;

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.Table;

// Note that we disabled this test since delivery from kinesis is setup after 15 minutes. This was used to test the wiring of the workers.
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@Disabled
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

		FileHandle handle = TestUtils.createS3FileHandle(user.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		
		fileHandleId = fileHandleDao.createFile(handle).getId();
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
