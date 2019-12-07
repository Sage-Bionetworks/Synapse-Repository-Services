package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;

public class DBOStorageLocationTest {
	private ExternalS3StorageLocationSetting extS3sls;

	@BeforeEach
	public void before() {
		extS3sls = new ExternalS3StorageLocationSetting();
		extS3sls.setUploadType(UploadType.HTTPS);
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupRemoveSlashes() {
		String expected = "remove-trailing-slash";
		extS3sls.setBaseKey(expected + "/");
		DBOStorageLocation result = executeTest(true);
		ExternalS3StorageLocationSetting resultData = (ExternalS3StorageLocationSetting) result.getData();
		assertEquals(expected, resultData.getBaseKey());
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupNoSlashesToRemove() {
		String expected = "remove-trailing-slash";
		extS3sls.setBaseKey(expected);
		DBOStorageLocation result = executeTest(false);
		ExternalS3StorageLocationSetting resultData = (ExternalS3StorageLocationSetting) result.getData();
		assertEquals(expected, resultData.getBaseKey());
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupNullBaseKey() {
		extS3sls.setBaseKey(null);
		DBOStorageLocation result = executeTest(false);
		ExternalS3StorageLocationSetting resultData = (ExternalS3StorageLocationSetting) result.getData();
		assertNull(resultData.getBaseKey());
	}

	/** Test for PLFM-5985 */
	@Test
	public void createDatabaseObjectFromBackup_NullUploadType() {
		extS3sls.setUploadType(null);
		DBOStorageLocation result = executeTest(true);
		assertEquals(UploadType.S3, result.getData().getUploadType());
		assertEquals(UploadType.S3, result.getUploadType());
	}

	/** Test for PLFM-5985 */
	@Test
	public void createDatabaseObjectFromBackup_ExistingUploadType() {
		extS3sls.setUploadType(UploadType.SFTP);
		DBOStorageLocation result = executeTest(false);
		assertEquals(UploadType.SFTP, result.getData().getUploadType());
	}

	private DBOStorageLocation executeTest(boolean isChanged) {
		// Setup backup object.
		String prevHash = StorageLocationUtils.computeHash(extS3sls);

		DBOStorageLocation dbo = new DBOStorageLocation();
		dbo.setData(extS3sls);
		dbo.setDataHash(prevHash);

		// Call under test.
		DBOStorageLocation result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertNotNull(result);

		if (isChanged) {
			assertNotEquals(prevHash, result.getDataHash());
		} else {
			assertEquals(prevHash, result.getDataHash());
		}

		return result;
	}
}
