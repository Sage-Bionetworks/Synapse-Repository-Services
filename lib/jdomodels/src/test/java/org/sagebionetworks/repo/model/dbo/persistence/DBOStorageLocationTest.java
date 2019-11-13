package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;

public class DBOStorageLocationTest {

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupRemoveSlashes() {
		String expected = "remove-trailing-slash";
		ExternalS3StorageLocationSetting extS3sls = new ExternalS3StorageLocationSetting();
		extS3sls.setBaseKey(expected + "/");

		String prevHash = StorageLocationUtils.computeHash(extS3sls);

		DBOStorageLocation dbo = new DBOStorageLocation();
		dbo.setData(extS3sls);
		dbo.setDataHash(prevHash);

		// call under test
		DBOStorageLocation result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertNotNull(result);

		ExternalS3StorageLocationSetting resultData = (ExternalS3StorageLocationSetting) result.getData();
		assertEquals(expected, resultData.getBaseKey());

		// The hash should change
		assertNotEquals(prevHash, result.getDataHash());
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupNoSlashesToRemove() {
		String expected = "remove-trailing-slash";
		ExternalS3StorageLocationSetting extS3sls = new ExternalS3StorageLocationSetting();
		extS3sls.setBaseKey(expected);

		String prevHash = StorageLocationUtils.computeHash(extS3sls);

		DBOStorageLocation dbo = new DBOStorageLocation();
		dbo.setData(extS3sls);
		dbo.setDataHash(prevHash);

		// call under test
		DBOStorageLocation result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertNotNull(result);

		ExternalS3StorageLocationSetting resultData = (ExternalS3StorageLocationSetting) result.getData();
		assertEquals(expected, resultData.getBaseKey());
		assertEquals(prevHash, result.getDataHash());
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupNullBaseKey() {
		ExternalS3StorageLocationSetting extS3sls = new ExternalS3StorageLocationSetting();
		extS3sls.setBaseKey(null);

		String prevHash = StorageLocationUtils.computeHash(extS3sls);

		DBOStorageLocation dbo = new DBOStorageLocation();
		dbo.setData(extS3sls);
		dbo.setDataHash(prevHash);

		// call under test
		DBOStorageLocation result = dbo.getTranslator().createDatabaseObjectFromBackup(dbo);
		assertNotNull(result);

		ExternalS3StorageLocationSetting resultData = (ExternalS3StorageLocationSetting) result.getData();
		assertNull(resultData.getBaseKey());
		assertEquals(prevHash, result.getDataHash());
	}

}
