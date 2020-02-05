package org.sagebionetworks.repo.manager.storagelocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.storagelocation.BucketOwnerVerifier.OWNER_MARKER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class BucketOwnerVerifierImplUnitTest {
	
	private static final String USER_NAME = "user-name";
	private static final String USER_EMAIL = "testuser@my.info.net";
	private static final Long USER_ID = 101L;
	private static final String BUCKET_NAME = "bucket.name";
	private static final String BASE_KEY = "baseKey";
	
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;
	
	@Mock
	private BucketObjectReader mockObjectReader;
	
	@Mock
	private Map<Class<? extends BucketOwnerStorageLocationSetting>, BucketObjectReader> mockBucketObjectReaderMap;
	
	@InjectMocks
	private BucketOwnerVerifierImpl bucketOwnerVerifier;
	
	@Mock
	private InputStream mockInputStream;
	
	@Mock
	private BufferedReader mockBufferedReader;
	
	@Mock
	private BucketOwnerStorageLocationSetting mockStorageLocation;
	
	@Mock
	private UserInfo mockUserInfo;
	
	private List<PrincipalAlias> principalAliases;

	@BeforeEach
	public void before() {
		PrincipalAlias username = new PrincipalAlias();
		username.setPrincipalId(USER_ID);
		username.setType(AliasType.USER_NAME);
		username.setAlias(USER_NAME);
		PrincipalAlias email1 = new PrincipalAlias();
		email1.setPrincipalId(USER_ID);
		email1.setType(AliasType.USER_EMAIL);
		email1.setAlias(USER_EMAIL);
		PrincipalAlias email2 = new PrincipalAlias();
		email2.setPrincipalId(USER_ID);
		email2.setType(AliasType.USER_EMAIL);
		email2.setAlias("institutional-email@institution.edu");
		principalAliases = Arrays.asList(username, email1, email2);
	}
	
	@Test
	public void testVerifyBucketOwnershipWithNoReader() {
		when(mockStorageLocation.getBucket()).thenReturn(BUCKET_NAME);
		when(mockBucketObjectReaderMap.get(any())).thenReturn(null);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		});
		
		assertEquals("Unsupported storage location type: " + mockStorageLocation.getClass().getSimpleName(), e.getMessage());
	}
	
	@Test
	public void testVerifyBucketOwnershipWithNoUser() throws IOException {
		UserInfo userInfo = null;
		BucketOwnerStorageLocationSetting storageLocation = mockStorageLocation;
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(userInfo, storageLocation);
		});
		
		assertEquals("The user is required.", e.getMessage());
		
	}
	
	@Test
	public void testVerifyBucketOwnershipWithNoStorageLocation() throws IOException {
		UserInfo userInfo = mockUserInfo;
		BucketOwnerStorageLocationSetting storageLocation = null;
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(userInfo, storageLocation);
		});
		
		assertEquals("The storage location is required.", e.getMessage());
		
	}
	
	@Test
	public void testVerifyBucketOwnershipWithNoBucket() throws IOException {
		when(mockStorageLocation.getBucket()).thenReturn(null);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		});
		
		assertEquals("The bucket is required.", e.getMessage());
		
	}
	
	@Test
	public void testVerifyBucketOwnership() throws IOException {
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockStorageLocation.getBucket()).thenReturn(BUCKET_NAME);
		when(mockBucketObjectReaderMap.get(any())).thenReturn(mockObjectReader);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		when(mockObjectReader.openStream(BUCKET_NAME, OWNER_MARKER)).thenReturn(mockInputStream);
		
		BucketOwnerVerifierImpl bucketOwnerVerifierSpy = Mockito.spy(bucketOwnerVerifier);
		
		when(bucketOwnerVerifierSpy.newStreamReader(mockInputStream)).thenReturn(mockBufferedReader);
		doNothing().when(bucketOwnerVerifierSpy).validateOwnerContent(any(), any(), any(), any());
		
		// Call under test
		bucketOwnerVerifierSpy.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		
		verify(mockObjectReader).openStream(BUCKET_NAME, OWNER_MARKER);
		verify(mockBufferedReader).readLine();
	}
	
	@Test
	public void testVerifyBucketOwnershipWithBaseKey() throws IOException {
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockStorageLocation.getBucket()).thenReturn(BUCKET_NAME);
		when(mockStorageLocation.getBaseKey()).thenReturn(BASE_KEY);
		when(mockBucketObjectReaderMap.get(any())).thenReturn(mockObjectReader);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		when(mockObjectReader.openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER)).thenReturn(mockInputStream);
		
		BucketOwnerVerifierImpl bucketOwnerVerifierSpy = Mockito.spy(bucketOwnerVerifier);
		
		when(bucketOwnerVerifierSpy.newStreamReader(mockInputStream)).thenReturn(mockBufferedReader);
		doNothing().when(bucketOwnerVerifierSpy).validateOwnerContent(any(), any(), any(), any());
		
		// Call under test
		bucketOwnerVerifierSpy.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		
		verify(mockObjectReader).openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER);
		verify(mockBufferedReader).readLine();
	}
	
	@Test
	public void testVerifyBucketOwnershipWithFailedOpenStream() throws IOException {
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockStorageLocation.getBucket()).thenReturn(BUCKET_NAME);
		when(mockStorageLocation.getBaseKey()).thenReturn(BASE_KEY);
		when(mockBucketObjectReaderMap.get(any())).thenReturn(mockObjectReader);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		
		IllegalStateException ex = new IllegalStateException("Failed to open stream");
		
		when(mockObjectReader.openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER)).thenThrow(ex);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		});
		
		assertTrue(e.getMessage().contains(ex.getMessage() + ". For security purposes"));
		
		verify(mockObjectReader).openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER);
	}
	
	@Test
	public void testVerifyBucketOwnershipWithExceptionOnRead() throws IOException {
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockStorageLocation.getBucket()).thenReturn(BUCKET_NAME);
		when(mockStorageLocation.getBaseKey()).thenReturn(BASE_KEY);
		when(mockBucketObjectReaderMap.get(any())).thenReturn(mockObjectReader);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		when(mockObjectReader.openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER)).thenReturn(mockInputStream);
		
		BucketOwnerVerifierImpl bucketOwnerVerifierSpy = Mockito.spy(bucketOwnerVerifier);
		
		when(bucketOwnerVerifierSpy.newStreamReader(mockInputStream)).thenReturn(mockBufferedReader);
		
		IOException ex = new IOException("I/O Exception");
		
		when(mockBufferedReader.readLine()).thenThrow(ex);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifierSpy.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		});
		
		assertTrue(e.getMessage().contains("Could not read username from key " + BASE_KEY + "/" + OWNER_MARKER + " from bucket " + BUCKET_NAME + ". For security purposes"));
		
		verify(mockObjectReader).openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER);
		verify(mockInputStream).close();
	}
	
	@Test
	public void testValidateOwnerContent() {
		// Call under test
		bucketOwnerVerifier.validateOwnerContent(USER_NAME, principalAliases, BUCKET_NAME, OWNER_MARKER);
	}

	@Test
	public void testValidateOwnerContentWithEmailAddress() {
		// Call under test
		bucketOwnerVerifier.validateOwnerContent(USER_EMAIL, principalAliases, BUCKET_NAME, OWNER_MARKER);
	}

	@Test
	public void testValidateOwnerContentNullUsername() {
		String userName = null;

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.validateOwnerContent(userName, principalAliases, BUCKET_NAME, OWNER_MARKER);
		});
		
		assertTrue(e.getMessage().contains("No username found"));
	}

	@Test
	public void testValidateOwnerContentBlankUsername() throws IOException {
		String userName = "";
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.validateOwnerContent(userName, principalAliases, BUCKET_NAME, OWNER_MARKER);
		});
		
		assertTrue(e.getMessage().contains("No username found"));
	}

	@Test
	public void testValidateOwnerContentUnexpected() {
		String userName = USER_NAME + "-incorrect";
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.validateOwnerContent(userName, principalAliases, BUCKET_NAME, OWNER_MARKER);
		});
		
		assertTrue(e.getMessage().contains("The username " + USER_NAME + "-incorrect found under"));
	}
	
}
