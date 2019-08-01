package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

public class StorageLocationUtilsTest {
	
	@Test
	public void testStorageLocationDBODTOConversion() {
		StorageLocationSetting dto = fillCommon(new S3StorageLocationSetting());
		
		DBOStorageLocation dbo = StorageLocationUtils.convertDTOtoDBO(dto);
		StorageLocationSetting dtoConverted = StorageLocationUtils.convertDBOtoDTO(dbo);
		
		assertEquals(dto, dtoConverted);
	}

	@Test
	public void testS3StorageLocationSettingSameHash() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		S3StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testDifferentStorageLocationImplementation() {
		StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		StorageLocationSetting copy = fillCommon(new ExternalS3StorageLocationSetting());

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testS3StorageLocationSettingWithDifferentId() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		setting.setStorageLocationId(123l);
		StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());
		copy.setStorageLocationId(456l);

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testS3StorageLocationSettingWithDifferentCreationDate() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		setting.setCreatedOn(new Date());
		StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());
		copy.setCreatedOn(Date.from(new Date().toInstant().plusSeconds(10)));

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testS3StorageLocationSettingWithDifferentEtag() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		setting.setEtag("Some etag");
		StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());
		copy.setEtag("Some other etag");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testS3StorageLocationSettingWithDifferentUser() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		setting.setCreatedBy(123l);
		StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());
		copy.setCreatedBy(456l);

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testS3StorageLocationSettingWithDifferentBanner() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		setting.setBanner("Some banner");
		StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());
		copy.setBanner("Some other banner");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testS3StorageLocationSettingWithDifferentDescription() {
		S3StorageLocationSetting setting = fillCommon(new S3StorageLocationSetting());
		setting.setDescription("Some description");
		StorageLocationSetting copy = fillCommon(new S3StorageLocationSetting());
		copy.setDescription("Some other description");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalS3StorageLocationSetting() {
		ExternalS3StorageLocationSetting setting = fillCommon(new ExternalS3StorageLocationSetting());
		ExternalS3StorageLocationSetting copy = fillCommon(new ExternalS3StorageLocationSetting());

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalS3StorageLocationSettingWithDifferentBaseKey() {
		ExternalS3StorageLocationSetting setting = fillCommon(new ExternalS3StorageLocationSetting());
		setting.setBaseKey("Some base key");
		ExternalS3StorageLocationSetting copy = fillCommon(new ExternalS3StorageLocationSetting());
		copy.setBaseKey("Some other base key");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalS3StorageLocationSettingWithDifferentBucket() {
		ExternalS3StorageLocationSetting setting = fillCommon(new ExternalS3StorageLocationSetting());
		setting.setBucket("Some bucket");
		ExternalS3StorageLocationSetting copy = fillCommon(new ExternalS3StorageLocationSetting());
		copy.setBucket("Some other bucket");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalS3StorageLocationSettingWithDifferentEndpoint() {
		ExternalS3StorageLocationSetting setting = fillCommon(new ExternalS3StorageLocationSetting());
		setting.setEndpointUrl("Some endpoint");
		ExternalS3StorageLocationSetting copy = fillCommon(new ExternalS3StorageLocationSetting());
		copy.setEndpointUrl("Some other endpoint");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalStorageLocationSetting() {
		ExternalStorageLocationSetting setting = fillCommon(new ExternalStorageLocationSetting());
		ExternalStorageLocationSetting copy = fillCommon(new ExternalStorageLocationSetting());

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalStorageLocationSettingWithDifferentUrl() {
		ExternalStorageLocationSetting setting = fillCommon(new ExternalStorageLocationSetting());
		setting.setUrl("Some url");
		ExternalStorageLocationSetting copy = fillCommon(new ExternalStorageLocationSetting());
		copy.setUrl("Some other url");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalStorageLocationSettingWithDifferentFolderSupport() {
		ExternalStorageLocationSetting setting = fillCommon(new ExternalStorageLocationSetting());
		setting.setSupportsSubfolders(true);
		ExternalStorageLocationSetting copy = fillCommon(new ExternalStorageLocationSetting());
		copy.setSupportsSubfolders(false);

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalObjectStorageLocationSetting() {
		ExternalObjectStorageLocationSetting setting = fillCommon(new ExternalObjectStorageLocationSetting());
		setting.setEndpointUrl("endpoint");
		setting.setBucket("bucket");
		ExternalObjectStorageLocationSetting copy = fillCommon(new ExternalObjectStorageLocationSetting());
		copy.setEndpointUrl("endpoint");
		copy.setBucket("bucket");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalObjectStorageLocationSettingWithDifferentBucket() {
		ExternalObjectStorageLocationSetting setting = fillCommon(new ExternalObjectStorageLocationSetting());
		setting.setBucket("Some bucket");
		setting.setEndpointUrl("endpoint");
		ExternalObjectStorageLocationSetting copy = fillCommon(new ExternalObjectStorageLocationSetting());
		copy.setBucket("Some other bucket");
		copy.setEndpointUrl("endpoint");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testExternalObjectStorageLocationSettingWithDifferentEndpoint() {
		ExternalObjectStorageLocationSetting setting = fillCommon(new ExternalObjectStorageLocationSetting());
		setting.setBucket("bucket");
		setting.setEndpointUrl("Some endpoint");
		ExternalObjectStorageLocationSetting copy = fillCommon(new ExternalObjectStorageLocationSetting());
		copy.setBucket("bucket");
		copy.setEndpointUrl("Some other endpoint");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testProxyStorageLocationSetting() {
		ProxyStorageLocationSettings setting = fillCommon(new ProxyStorageLocationSettings());
		ProxyStorageLocationSettings copy = fillCommon(new ProxyStorageLocationSettings());

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertEquals(copyHash, settingHash);
	}

	@Test
	public void testProxyStorageLocationSettingWithDifferentBenefactor() {
		ProxyStorageLocationSettings setting = fillCommon(new ProxyStorageLocationSettings());
		setting.setBenefactorId("123");
		ProxyStorageLocationSettings copy = fillCommon(new ProxyStorageLocationSettings());
		copy.setBenefactorId("456");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testProxyStorageLocationSettingWithDifferentProxyUrl() {
		ProxyStorageLocationSettings setting = fillCommon(new ProxyStorageLocationSettings());
		setting.setProxyUrl("Some url");
		ProxyStorageLocationSettings copy = fillCommon(new ProxyStorageLocationSettings());
		copy.setProxyUrl("Some other url");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	@Test
	public void testProxyStorageLocationSettingWithDifferentSecretKey() {
		ProxyStorageLocationSettings setting = fillCommon(new ProxyStorageLocationSettings());
		setting.setSecretKey("Some secret key");
		ProxyStorageLocationSettings copy = fillCommon(new ProxyStorageLocationSettings());
		copy.setSecretKey("Some other secret key");

		String settingHash = StorageLocationUtils.computeHash(setting);
		String copyHash = StorageLocationUtils.computeHash(copy);

		assertNotEquals(copyHash, settingHash);
	}

	private <T extends StorageLocationSetting> T fillCommon(T setting) {
		setting.setStorageLocationId(123l);
		setting.setCreatedBy(1l);
		setting.setCreatedOn(new Date());
		setting.setEtag("Etag");
		setting.setBanner("Some Banner");
		setting.setDescription("Some Description");
		return setting;
	}

}
