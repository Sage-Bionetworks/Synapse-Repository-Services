package org.sagebionetworks.repo.manager.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RestoreTypeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is a temporarily test of the TableTransactionBackfillMigrationListener.
 * This test will be removed when the listener is removed.
 */
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
	
	/**
	 * This test is run using a locally provided backup actual data from production to ensure
	 * all transactions are created and bound as expected.
	 * 
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void testBackFillLocal() throws IOException {
		String localFile = "C:/Users/jhill/Downloads/prod-255-TABLE_SEQUENCE-e36f4bd3-f0ce-4b2d-ac0e-762fff3acbae.zip";
		try (FileInputStream input = new FileInputStream(new File(localFile))) {
			MigrationType primaryType = MigrationType.TABLE_SEQUENCE;
			BackupAliasType backupAliasType = BackupAliasType.MIGRATION_TYPE_NAME;
			Long batchSize = 50000L;
			// call under test
			RestoreTypeResponse response = migrationManager.restoreStream(input, primaryType, backupAliasType,
					batchSize);
			System.out.println("Rows restored: " + response.getRestoredRowCount());
		}
	}

}
