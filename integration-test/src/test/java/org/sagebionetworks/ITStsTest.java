package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseStsCredentialsProvider;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.sts.StsPermission;
import org.sagebionetworks.util.ContentDispositionUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;

@ExtendWith(ITTestExtension.class)
public class ITStsTest {
	private static String externalS3Bucket;
	private static SynapseS3Client synapseS3Client;
	private static String username;

	private Folder folder;
	private Project project;
	
	private StackConfiguration config;
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	
	public ITStsTest(StackConfiguration config, SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.config = config;
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}


	@BeforeAll
	public static void beforeClass(StackConfiguration config, SynapseClient synapse) throws Exception {
		externalS3Bucket = config.getExternalS3TestBucketName();
		username = synapse.getMyProfile().getUserName();

		// Set up S3 client.
		synapseS3Client = AwsClientFactory.createAmazonS3Client();
	}

	@BeforeEach
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();

		// Create a test project which we will need.
		project = new Project();
		project = synapse.createEntity(project);

		// Create folder, which is required for STS.
		folder = new Folder();
		folder.setParentId(project.getId());
		folder = synapse.createEntity(folder);
	}

	@AfterEach
	public void after() throws SynapseException {
		synapse.deleteEntity(project, true);
	}

	@Test
	public void test() throws Exception {
		// Only run this test if the STS Arn is set up.
		Assumptions.assumeTrue(config.getTempCredentialsIamRoleArn() != null);

		// Set up a test folder in S3 and an owner.txt. This is required to make the STS Storage Location.
		String baseKey = "integration-test/ITStsTest-" + UUID.randomUUID().toString();
		byte[] bytes = username.getBytes(StandardCharsets.UTF_8);

		ObjectMetadata ownerTxtMetadata = new ObjectMetadata();
		ownerTxtMetadata.setContentType("text/plain");
		ownerTxtMetadata.setContentEncoding("UTF-8");
		ownerTxtMetadata.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(baseKey));
		ownerTxtMetadata.setContentLength(bytes.length);
		synapseS3Client.putObject(externalS3Bucket, baseKey + "/owner.txt",
				new ByteArrayInputStream(bytes), ownerTxtMetadata);

		// Create another file, to test reading using STS credentials.
		byte[] fileTxtContent = "Dummy content".getBytes(StandardCharsets.UTF_8);
		ObjectMetadata fileTxtMetadata = new ObjectMetadata();
		fileTxtMetadata.setContentType("text/plain");
		fileTxtMetadata.setContentEncoding("UTF-8");
		fileTxtMetadata.setContentDisposition(ContentDispositionUtils.getContentDispositionValue(baseKey));
		fileTxtMetadata.setContentLength(fileTxtContent.length);
		synapseS3Client.putObject(externalS3Bucket, baseKey + "/file.txt",
				new ByteArrayInputStream(fileTxtContent), fileTxtMetadata);

		// Create StsStorageLocation.
		ExternalS3StorageLocationSetting storageLocationSetting = new ExternalS3StorageLocationSetting();
		storageLocationSetting.setBaseKey(baseKey);
		storageLocationSetting.setBucket(externalS3Bucket);
		storageLocationSetting.setStsEnabled(true);
		storageLocationSetting.setUploadType(UploadType.S3);
		storageLocationSetting = synapse.createStorageLocationSetting(storageLocationSetting);

		// Create project setting.
		UploadDestinationListSetting projectSetting = new UploadDestinationListSetting();
		projectSetting.setLocations(ImmutableList.of(storageLocationSetting.getStorageLocationId()));
		projectSetting.setProjectId(folder.getId());
		projectSetting.setSettingsType(ProjectSettingsType.upload);
		synapse.createProjectSetting(projectSetting);

		// Get read-only credentials.
		AWSCredentialsProvider awsCredentialsProvider = new SynapseStsCredentialsProvider(synapse, folder.getId(),
				StsPermission.read_only);
		AmazonS3 tempClient = AmazonS3ClientBuilder.standard()
				.withCredentials(awsCredentialsProvider).withRegion(Regions.US_EAST_1).build();

		// Validate we can list the bucket from the base key.
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(externalS3Bucket)
				.withDelimiter("/").withPrefix(baseKey);
		tempClient.listObjects(listObjectsRequest);

		// Validate can read file.txt.
		tempClient.getObject(externalS3Bucket, baseKey + "/file.txt");

		// We cannot read owner.txt.
		AmazonServiceException ex = assertThrows(AmazonServiceException.class, () -> tempClient.getObject(
				externalS3Bucket, baseKey + "owner.txt"));
		assertEquals(403, ex.getStatusCode());
	}
}
