package org.sagebionetworks;


import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.util.ContentDispositionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

// PFLM-5985 - Creating storage locations with UploadType=null, associating them via project setting to a project, and
// then calling getUploadDestinationLocations will result in a NullPointerException. This is now fixed, and this test
// verifies the fix.
public class ITStorageLocation {
	private static Long userToDelete;
	private static StackConfiguration config;
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static SynapseS3Client synapseS3Client;

	@BeforeClass
	public static void beforeClass() throws Exception {
		config = StackConfigurationSingleton.singleton();
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();

		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);

		synapseS3Client = AwsClientFactory.createAmazonS3Client();
	}

	@Before
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
	}

	@AfterClass
	public static void afterClass() {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) {
			// Ignore possible exceptions
		}
	}

	@Test(expected = SynapseBadRequestException.class)
	public void externalObjectStorageMustHaveUploadType() throws SynapseException {
		ExternalObjectStorageLocationSetting storageLocationSetting = new ExternalObjectStorageLocationSetting();
		storageLocationSetting.setBucket("some-bucket");
		storageLocationSetting.setEndpointUrl("https://someurl.com");
		storageLocationSetting.setUploadType(null);
		synapse.createStorageLocationSetting(storageLocationSetting);
	}

	@Test(expected = SynapseBadRequestException.class)
	public void externalStorageMustHaveUploadType() throws SynapseException {
		ExternalStorageLocationSetting storageLocationSetting = new ExternalStorageLocationSetting();
		storageLocationSetting.setUrl("sftp://somewhere.com");
		storageLocationSetting.setUploadType(null);
		synapse.createStorageLocationSetting(storageLocationSetting);
	}

	@Test(expected = SynapseBadRequestException.class)
	public void proxyStorageMustHaveUploadType() throws SynapseException {
		ProxyStorageLocationSettings storageLocationSetting = new ProxyStorageLocationSettings();
		storageLocationSetting.setSecretKey("Super secret key that must be fairly long");
		storageLocationSetting.setProxyUrl("https://host.org");
		storageLocationSetting.setUploadType(null);
		synapse.createStorageLocationSetting(storageLocationSetting);
	}

	@Test
	public void testDefaultUploadType() throws SynapseException {
		// Ensure owner.txt is in our S3 bucket.
		String username = synapse.getUserProfile(userToDelete.toString()).getUserName();
		byte[] bytes = username.getBytes(StandardCharsets.UTF_8);

		String baseKey = "integration-test/ITStorageLocation-" + UUID.randomUUID().toString();
		ObjectMetadata om = new ObjectMetadata();
		om.setContentType("text/plain");
		om.setContentEncoding("UTF-8");
		om.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(baseKey));
		om.setContentLength(bytes.length);
		synapseS3Client.putObject(config.getS3Bucket(), baseKey + "/owner.txt",
				new ByteArrayInputStream(bytes), om);

		// Create an External S3 Storage Location and a Synapse Storage Location w/o upload type.
		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(config.getS3Bucket());
		externalS3StorageLocationSetting.setBaseKey(baseKey);
		externalS3StorageLocationSetting.setUploadType(null);
		externalS3StorageLocationSetting = synapse.createStorageLocationSetting(externalS3StorageLocationSetting);

		S3StorageLocationSetting synapseS3StorageLocationSetting = new S3StorageLocationSetting();
		synapseS3StorageLocationSetting.setUploadType(null);
		synapseS3StorageLocationSetting = synapse.createStorageLocationSetting(synapseS3StorageLocationSetting);

		// Create a test project which we will need.
		Project project = new Project();
		project = synapse.createEntity(project);

		try {
			// Assign these storage locations as a project setting.
			UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
			projectSetting.setProjectId(project.getId());
			projectSetting.setSettingsType(ProjectSettingsType.upload);
			projectSetting.setLocations(ImmutableList.of(externalS3StorageLocationSetting.getStorageLocationId(),
					synapseS3StorageLocationSetting.getStorageLocationId()));
			synapse.createProjectSetting(projectSetting);

			// Get Upload Destination Locations.
			UploadDestinationLocation[] uploadDestinationLocations = synapse.getUploadDestinationLocations(
					project.getId());
			assertEquals(2, uploadDestinationLocations.length);
			assertEquals(externalS3StorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[0]
					.getStorageLocationId());
			assertEquals(synapseS3StorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[1]
					.getStorageLocationId());
		} finally {
			synapse.deleteEntity(project, true);
		}
	}
}
