package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.S3UploadDestinationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationSetting;

public class ProjectSettingsUtilTest {

	@Test
	public void testValidateUploadDestinationSetting() {
		ExternalUploadDestinationSetting setting = new ExternalUploadDestinationSetting();
		// Null uploadType
		Exception ex = null;
		setting.setUploadType(UploadType.HTTPS);
		setting.setUrl("https://someurl");
		// Should not raise exception
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
		setting.setUrl(null);
		try {
			ProjectSettingsUtil.validateUploadDestinationSetting(setting);
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			ex = e;
		}
		assertNotNull(ex);
		// Null url
		ex = null;
		setting.setUrl("http://someurl");
		setting.setUploadType(null);
		try {
			ProjectSettingsUtil.validateUploadDestinationSetting(setting);		
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			ex = e;
		}
		assertNotNull(ex);
		// Mismatched protocols
		ex = null;
		setting.setUploadType(UploadType.S3);
		try {
			ProjectSettingsUtil.validateUploadDestinationSetting(setting);		
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			ex = e;
		}
		assertNotNull(ex);
		ex = null;
		setting.setUploadType(UploadType.HTTPS);
		setting.setUrl("sftp://someurl");
		try {
			ProjectSettingsUtil.validateUploadDestinationSetting(setting);		
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			ex = e;
		}
		assertNotNull(ex);
		ex = null;
		setting.setUploadType(UploadType.SFTP);
		setting.setUrl("s3://someurl");
		try {
			ProjectSettingsUtil.validateUploadDestinationSetting(setting);		
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			ex = e;
		}
		assertNotNull(ex);
		setting.setUrl("sftp://someurl");
		// Should not raise exception
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
		
		// S3UploadDestinationSetting
		ex = null;
		S3UploadDestinationSetting setting2 = new S3UploadDestinationSetting();
		setting2.setUploadType(UploadType.SFTP);
		try {
			ProjectSettingsUtil.validateUploadDestinationSetting(setting2);
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
			ex = e;
		}
		assertNotNull(ex);
		setting2.setUploadType(UploadType.S3);
		// Should not raise exception
		ProjectSettingsUtil.validateUploadDestinationSetting(setting2);
	}
	
}
