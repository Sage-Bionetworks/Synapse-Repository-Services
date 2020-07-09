package org.sagebionetworks.repo.manager.storagelocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.ImmutableList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BucketOwnerVerifierImplAutowireTest {
	
	private static final long TEST_TEAM_ID = 123L;

	@Autowired
	private BucketOwnerVerifierImpl bucketOwnerVerifier;
	
	@Autowired
	private SynapseS3Client s3Client;
	
	@Autowired
	private UserManager userManager;
	
	private List<Long> users;
	
	@BeforeEach
	public void before() {
		users = new ArrayList<Long>();
	}
	
	@AfterEach
	public void after() {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		users.forEach(userId -> userManager.deletePrincipal(adminUserInfo, userId));
	}
	
	@Test
	public void testS3StorageLocationWiring() {
		BucketOwnerStorageLocationSetting storageLocation = new ExternalS3StorageLocationSetting();
		BucketObjectReader reader = bucketOwnerVerifier.getObjectReader(storageLocation);
		assertNotNull(reader);
		assertEquals(ExternalS3StorageLocationSetting.class, reader.getSupportedStorageLocationType());
	}
	
	@Test
	public void testGCStorageLocationWiring() {
		BucketOwnerStorageLocationSetting storageLocation = new ExternalGoogleCloudStorageLocationSetting();
		BucketObjectReader reader = bucketOwnerVerifier.getObjectReader(storageLocation);
		assertNotNull(reader);
		assertEquals(ExternalGoogleCloudStorageLocationSetting.class, reader.getSupportedStorageLocationType());
	}
	
	@Test
	public void testUnsupportedStorageLocation() {
		BucketOwnerStorageLocationSetting storageLocation = Mockito.mock(BucketOwnerStorageLocationSetting.class);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			bucketOwnerVerifier.getObjectReader(storageLocation);
		});
		assertEquals("Unsupported storage location type: " + storageLocation.getClass().getSimpleName(), e.getMessage());
	}
	
	@Test
	public void testVerificationWithMultipleUsers() throws Exception {
		UserInfo user1 = createUser();
		UserInfo user2 = createUser();
		
		List<String> ownerList = ImmutableList.of(user1.getId().toString(), user2.getId().toString());
		
		BucketOwnerStorageLocationSetting storageLocation = linkBucket(ownerList);
		
		bucketOwnerVerifier.verifyBucketOwnership(user1, storageLocation);
		bucketOwnerVerifier.verifyBucketOwnership(user2, storageLocation);
	}
	
	@Test
	public void testVerificationWithMultipleUsersOnSameLine() throws Exception {
		UserInfo user1 = createUser();
		UserInfo user2 = createUser();
		
		List<String> ownerList = ImmutableList.of(String.join(BucketOwnerVerifier.SAME_LINE_SEPARATOR, user1.getId().toString(), user2.getId().toString()));
		
		BucketOwnerStorageLocationSetting storageLocation = linkBucket(ownerList);
		
		bucketOwnerVerifier.verifyBucketOwnership(user1, storageLocation);
		bucketOwnerVerifier.verifyBucketOwnership(user2, storageLocation);
	}
	
	@Test
	public void testVerificationWithDifferentOwner() throws Exception {
		UserInfo user1 = createUser();
		UserInfo user2 = createUser();
		
		// Only user1 is in the owner list
		List<String> ownerList = ImmutableList.of(user1.getId().toString());
		
		BucketOwnerStorageLocationSetting storageLocation = linkBucket(ownerList);
		
		bucketOwnerVerifier.verifyBucketOwnership(user1, storageLocation);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(user2, storageLocation);
		});
		
		assertTrue(ex.getMessage().startsWith("Could not find a valid user identifier"));
	}
	
	@Test
	public void testVerificationWithMultipleUsersThroughTeam() throws Exception {
		UserInfo user1 = createUser();
		UserInfo user2 = createUser();
		
		List<String> ownerList = ImmutableList.of(String.valueOf(TEST_TEAM_ID));
		
		BucketOwnerStorageLocationSetting storageLocation = linkBucket(ownerList);
		
		bucketOwnerVerifier.verifyBucketOwnership(user1, storageLocation);
		bucketOwnerVerifier.verifyBucketOwnership(user2, storageLocation);
	}
	
	
	private BucketOwnerStorageLocationSetting linkBucket(List<String> ownersList) throws Exception {
		
		String baseKey = "test_externalS3MultipleUsers_" + UUID.randomUUID();
		
		BucketOwnerStorageLocationSetting storageLocation = new ExternalS3StorageLocationSetting();
		
		storageLocation.setBucket(StackConfigurationSingleton.singleton().getExternalS3TestBucketName());
		storageLocation.setBaseKey(baseKey);
		
		s3Client.createBucket(storageLocation.getBucket());
			
		String ownerContent = String.join("\n", ownersList);

		ObjectMetadata metadata = new ObjectMetadata();
		
		metadata.setContentLength(ownerContent.length());
		
		String key = baseKey + "/owner.txt";
		
		s3Client.putObject(storageLocation.getBucket(), key, new StringInputStream(ownerContent), metadata);
		
		waitForS3Object(storageLocation.getBucket(), key);
		
		return storageLocation;
		
	}
	
	private UserInfo createUser() {
		NewUser user = new NewUser();
		
		String username = UUID.randomUUID().toString();
		
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		
		long userId = userManager.createUser(user);

		UserInfo userInfo = userManager.getUserInfo(userId);
		
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		userInfo.getGroups().add(TEST_TEAM_ID);

		return userInfo;
	}
	
	private void waitForS3Object(String bucket, String key) throws InterruptedException {
		long start = System.currentTimeMillis();
		while(!s3Client.doesObjectExist(bucket, key)) {
			if (System.currentTimeMillis() - start > 60 * 1000) {
				throw new IllegalStateException("Timed out while waiting for S3 object: " + bucket + "/" + key);
			}
			Thread.sleep(1000);
		}
	}
	
}
