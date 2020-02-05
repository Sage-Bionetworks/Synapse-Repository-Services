package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.StorageLocationUtils;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;

public class DBOStorageLocationTest {
	private ExternalGoogleCloudStorageLocationSetting storageLocation;

	@BeforeEach
	public void before() {
		storageLocation = new ExternalGoogleCloudStorageLocationSetting();
		storageLocation.setBucket("some.bucket");
		storageLocation.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupRemoveSlashes() {
		String expected = "remove-trailing-slash";
		storageLocation.setBaseKey(expected + "/");
		DBOStorageLocation result = executeTest(true);
		ExternalGoogleCloudStorageLocationSetting resultData = (ExternalGoogleCloudStorageLocationSetting) result.getData();
		assertEquals(expected, resultData.getBaseKey());
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupNoSlashesToRemove() {
		String expected = "remove-trailing-slash";
		storageLocation.setBaseKey(expected);
		DBOStorageLocation result = executeTest(false);
		ExternalGoogleCloudStorageLocationSetting resultData = (ExternalGoogleCloudStorageLocationSetting) result.getData();
		assertEquals(expected, resultData.getBaseKey());
	}

	/**
	 * Test for PLFM-5769
	 */
	@Test
	public void createDatabaseObjectFromBackupNullBaseKey() {
		storageLocation.setBaseKey(null);
		DBOStorageLocation result = executeTest(false);
		ExternalGoogleCloudStorageLocationSetting resultData = (ExternalGoogleCloudStorageLocationSetting) result.getData();
		assertNull(resultData.getBaseKey());
	}

	private DBOStorageLocation executeTest(boolean isChanged) {
		// Setup backup object.
		String prevHash = StorageLocationUtils.computeHash(storageLocation);

		DBOStorageLocation dbo = new DBOStorageLocation();
		dbo.setData(storageLocation);
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
