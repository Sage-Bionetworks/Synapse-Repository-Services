package org.sagebionetworks.repo.manager.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RestoreTypeRequest;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TransactionBackfillTest {
	
	@Autowired
	private MigrationManager migrationManager;
	@Autowired
	private UserManager userManager;
	
	UserInfo adminUser;
	
	@Before
	public void before() {
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@Ignore
	@Test
	public void testBackFill() throws IOException {
		RestoreTypeRequest restoreRequest = new RestoreTypeRequest();
		restoreRequest.setAliasType(BackupAliasType.MIGRATION_TYPE_NAME);
		restoreRequest.setBackupFileKey("prod-255-TABLE_SEQUENCE-e36f4bd3-f0ce-4b2d-ac0e-762fff3acbae.zip");
		restoreRequest.setBatchSize(50000L);
		restoreRequest.setMinimumRowId(0L);
		restoreRequest.setMaximumRowId(Long.MAX_VALUE);
		restoreRequest.setMigrationType(MigrationType.TABLE_SEQUENCE);
		// call under test
		migrationManager.restoreRequest(adminUser, restoreRequest);
	}
	
	@Test
	public void testBackFillLocal() throws IOException {
		String localFile = "C:/Users/John/Downloads/prod-255-TABLE_SEQUENCE-e36f4bd3-f0ce-4b2d-ac0e-762fff3acbae.zip";
		try(FileInputStream input = new FileInputStream(new File(localFile))){
			MigrationType primaryType = MigrationType.TABLE_SEQUENCE;
			BackupAliasType backupAliasType = BackupAliasType.MIGRATION_TYPE_NAME;
			Long batchSize = 50000L;
			// call under test
			RestoreTypeResponse response = migrationManager.restoreStream(input, primaryType, backupAliasType, batchSize);
			System.out.println("Rows restored: "+response.getRestoredRowCount());
		}
	}

}
