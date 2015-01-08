package org.sagebionetworks.repo.manager;

import org.junit.Test;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalUploadDestinationSetting;
import org.sagebionetworks.repo.model.project.S3UploadDestinationSetting;

public class ProjectSettingsUtilTest {

	@Test
	public void testValidateUploadDestinationSettingValid() {
		ExternalUploadDestinationSetting setting = new ExternalUploadDestinationSetting();
		setting.setUploadType(UploadType.HTTPS);
		setting.setUrl("https://someurl");
		// Should not raise exception
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
	}
		
	@Test (expected=IllegalArgumentException.class)
	public void testValidateUploadDestinationSettingsNullUrl() {
		ExternalUploadDestinationSetting setting = new ExternalUploadDestinationSetting();
		setting.setUploadType(UploadType.HTTPS);
		setting.setUrl(null);
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
	}
		

	@Test (expected=IllegalArgumentException.class)
	public void testValidateUploadDestinationSettingNullUploadType() {
		ExternalUploadDestinationSetting setting = new ExternalUploadDestinationSetting();
		setting.setUrl("http://someurl");
		setting.setUploadType(null);
			ProjectSettingsUtil.validateUploadDestinationSetting(setting);		
	}


	@Test (expected=IllegalArgumentException.class)
	public void testValidateUploadDestinationSettingMismatchedProtocols2() {
		ExternalUploadDestinationSetting setting = new ExternalUploadDestinationSetting();
		setting.setUploadType(UploadType.HTTPS);
		setting.setUrl("sftp://someurl");
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testValidateUploadDestinationSettingMismatchedProtocols3() {
		ExternalUploadDestinationSetting setting = new ExternalUploadDestinationSetting();
		setting.setUploadType(UploadType.SFTP);
		setting.setUrl("s3://someurl");
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
	}
	
	@Test
	public void testValidateUploadDestinationSettingMismatchedProtocolS3UploadValid() {
		S3UploadDestinationSetting setting = new S3UploadDestinationSetting();
		ProjectSettingsUtil.validateUploadDestinationSetting(setting);
	}
	
}
