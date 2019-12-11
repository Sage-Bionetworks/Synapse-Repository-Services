package org.sagebionetworks;


import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

// PFLM-5985 - Creating storage locations with UploadType=null, associating them via project setting to a project, and
// then calling getUploadDestinationLocations will result in a NullPointerException. This is now fixed, and this test
// verifies the fix.
public class ITStorageLocation {
	private static Long userToDelete;
	private static StackConfiguration config;
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static SynapseS3Client synapseS3Client;

	private Project projectToDelete;

	@BeforeAll
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

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		projectToDelete = null;
	}

	@AfterEach
	public void after() throws SynapseException {
		if (projectToDelete != null) {
			synapse.deleteEntity(projectToDelete, true);
		}
	}

	@AfterAll
	public static void afterClass() {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) {
			// Ignore possible exceptions
		}
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

		// Create Storage Locations w/o upload type.
		ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting =
				new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBucket("some-bucket");
		externalObjectStorageLocationSetting.setEndpointUrl("https://someurl.com");
		externalObjectStorageLocationSetting.setUploadType(null);
		externalObjectStorageLocationSetting = synapse.createStorageLocationSetting(
				externalObjectStorageLocationSetting);
		assertEquals(UploadType.NONE, externalObjectStorageLocationSetting.getUploadType());

		ExternalS3StorageLocationSetting externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(config.getS3Bucket());
		externalS3StorageLocationSetting.setBaseKey(baseKey);
		externalS3StorageLocationSetting.setUploadType(null);
		externalS3StorageLocationSetting = synapse.createStorageLocationSetting(externalS3StorageLocationSetting);
		assertEquals(UploadType.S3, externalS3StorageLocationSetting.getUploadType());

		ExternalStorageLocationSetting externalStorageLocationSetting = new ExternalStorageLocationSetting();
		externalStorageLocationSetting.setUrl("sftp://somewhere.com");
		externalStorageLocationSetting.setUploadType(null);
		externalStorageLocationSetting = synapse.createStorageLocationSetting(externalStorageLocationSetting);
		assertEquals(UploadType.NONE, externalStorageLocationSetting.getUploadType());

		ProxyStorageLocationSettings proxyStorageLocationSetting = new ProxyStorageLocationSettings();
		proxyStorageLocationSetting.setSecretKey("Super secret key that must be fairly long");
		proxyStorageLocationSetting.setProxyUrl("https://host.org");
		proxyStorageLocationSetting.setUploadType(null);
		proxyStorageLocationSetting = synapse.createStorageLocationSetting(proxyStorageLocationSetting);
		assertEquals(UploadType.NONE, proxyStorageLocationSetting.getUploadType());

		S3StorageLocationSetting synapseS3StorageLocationSetting = new S3StorageLocationSetting();
		synapseS3StorageLocationSetting.setUploadType(null);
		synapseS3StorageLocationSetting = synapse.createStorageLocationSetting(synapseS3StorageLocationSetting);
		assertEquals(UploadType.S3, synapseS3StorageLocationSetting.getUploadType());

		// Create a test project which we will need.
		Project project = new Project();
		project = synapse.createEntity(project);
		projectToDelete = project;

		// Assign these storage locations as a project setting.
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setProjectId(project.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		projectSetting.setLocations(ImmutableList.of(
				externalObjectStorageLocationSetting.getStorageLocationId(),
				externalS3StorageLocationSetting.getStorageLocationId(),
				externalStorageLocationSetting.getStorageLocationId(),
				proxyStorageLocationSetting.getStorageLocationId(),
				synapseS3StorageLocationSetting.getStorageLocationId()));
		synapse.createProjectSetting(projectSetting);

		// Get Upload Destination Locations.
		UploadDestinationLocation[] uploadDestinationLocations = synapse.getUploadDestinationLocations(
				project.getId());
		assertEquals(5, uploadDestinationLocations.length);
		assertEquals(externalObjectStorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[0]
				.getStorageLocationId());
		assertEquals(externalS3StorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[1]
				.getStorageLocationId());
		assertEquals(externalStorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[2]
				.getStorageLocationId());
		assertEquals(proxyStorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[3]
				.getStorageLocationId());
		assertEquals(synapseS3StorageLocationSetting.getStorageLocationId(), uploadDestinationLocations[4]
				.getStorageLocationId());
	}
}
