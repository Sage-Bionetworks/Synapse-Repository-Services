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
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class BucketOwnerVerifierImplUnitTest {
	
	private static final String USER_NAME = "user-name";
	private static final String USER_EMAIL = "testuser@my.info.net";
	private static final Long USER_ID = 101L;
	private static final String BUCKET_NAME = "bucket.name";
	private static final String BASE_KEY = "baseKey";
	private static final String OWNER_KEY = BASE_KEY + "/" + OWNER_MARKER;
	
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
		
		BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader(OWNER_MARKER);
		
		doNothing().when(bucketOwnerVerifier).validateOwnerContent(any(), any(), any(), any());
		
		// Call under test
		bucketOwnerVerifier.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		
		verify(mockObjectReader).openStream(BUCKET_NAME, OWNER_MARKER);
		verify(mockBufferedReader).lines();
	}
	
	@Test
	public void testVerifyBucketOwnershipWithBaseKey() throws IOException {
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockStorageLocation.getBucket()).thenReturn(BUCKET_NAME);
		when(mockStorageLocation.getBaseKey()).thenReturn(BASE_KEY);
		when(mockBucketObjectReaderMap.get(any())).thenReturn(mockObjectReader);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
				
		BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		doNothing().when(bucketOwnerVerifier).validateOwnerContent(any(), any(), any(), any());
		
		// Call under test
		bucketOwnerVerifier.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		
		verify(mockObjectReader).openStream(BUCKET_NAME, OWNER_KEY);
		verify(mockBufferedReader).lines();
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

		BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		when(mockBufferedReader.lines()).thenThrow(UncheckedIOException.class);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.verifyBucketOwnership(mockUserInfo, mockStorageLocation);
		});
		
		assertTrue(e.getMessage().contains("Could not read key " + BASE_KEY + "/" + OWNER_MARKER + " from bucket " + BUCKET_NAME + ". For security purposes"));
		
		verify(mockObjectReader).openStream(BUCKET_NAME, BASE_KEY + "/" + OWNER_MARKER);
		verify(mockInputStream).close();
	}
	
	@Test
	public void testValidateOwnerContentWithUserName() {
		List<String> ownerContent = ImmutableList.of(USER_NAME);
		Set<String> userAliases = ImmutableSet.of(USER_NAME, USER_EMAIL);
		// Call under test
		bucketOwnerVerifier.validateOwnerContent(ownerContent, userAliases, BUCKET_NAME, OWNER_MARKER);
	}

	@Test
	public void testValidateOwnerContentWithEmailAddress() {
		List<String> ownerContent = ImmutableList.of(USER_NAME);
		Set<String> userAliases = ImmutableSet.of(USER_NAME, USER_EMAIL);
		// Call under test
		bucketOwnerVerifier.validateOwnerContent(ownerContent, userAliases, BUCKET_NAME, OWNER_MARKER);
	}
	
	@Test
	public void testValidateOwnerContentWithMultipleOwners() {
		List<String> ownerContent = ImmutableList.of(USER_NAME, USER_ID.toString());
		Set<String> userAliases = ImmutableSet.of(USER_ID.toString(), USER_NAME, USER_EMAIL);
		// Call under test
		bucketOwnerVerifier.validateOwnerContent(ownerContent, userAliases, BUCKET_NAME, OWNER_MARKER);
	}

	@Test
	public void testValidateOwnerContentWithEmptyOwnerContent() {
		List<String> ownerContent = Collections.emptyList();
		Set<String> userAliases = ImmutableSet.of(USER_EMAIL);

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.validateOwnerContent(ownerContent, userAliases, BUCKET_NAME, OWNER_MARKER);
		});
		
		assertTrue(e.getMessage().contains("No user identifier found"));
	}

	@Test
	public void testValidateOwnerContentWithNoMatch() throws IOException {
		List<String> ownerContent = ImmutableList.of(USER_EMAIL + "_different");
		Set<String> userAliases = ImmutableSet.of(USER_EMAIL);
		
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			bucketOwnerVerifier.validateOwnerContent(ownerContent, userAliases, BUCKET_NAME, OWNER_MARKER);
		});
		
		assertTrue(e.getMessage().contains("Could not find a valid user identifier"));
	}
	
	@Test
	public void testReadOwnerContentWithNoLines() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = Collections.emptyList();
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void testReadOwnerContentWithEmptyLines() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = ImmutableList.of(
			USER_EMAIL,
			"",
			"   "
		);
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		List<String> expected = ImmutableList.of(USER_EMAIL);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testReadOwnerContent() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = ImmutableList.of(
			USER_EMAIL, USER_ID.toString(), USER_NAME
		);
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		List<String> expected = ImmutableList.of(USER_EMAIL, USER_ID.toString(), USER_NAME);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testReadOwnerContentWithSameLineValues() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = ImmutableList.of(
			String.join(BucketOwnerVerifier.SAME_LINE_SEPARATOR, USER_EMAIL, USER_NAME)
		);
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		List<String> expected = ImmutableList.of(USER_EMAIL, USER_NAME);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testReadOwnerContentWithSameLineValuesAndEmpty() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = ImmutableList.of(
			String.join(BucketOwnerVerifier.SAME_LINE_SEPARATOR, USER_EMAIL, " ")
		);
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		List<String> expected = ImmutableList.of(USER_EMAIL);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testReadOwnerContentWithSameLineValuesAndMultipleLines() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = ImmutableList.of(
			String.join(BucketOwnerVerifier.SAME_LINE_SEPARATOR, USER_EMAIL, USER_NAME),
			USER_ID.toString()
		);
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		List<String> expected = ImmutableList.of(USER_EMAIL, USER_NAME, USER_ID.toString());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testReadOwnerContentWithSameLineValuesWithSpaces() {
		
	    BucketOwnerVerifierImpl bucketOwnerVerifier = setupMockObjectReader();
		
		List<String> ownerContent = ImmutableList.of(
			String.join(BucketOwnerVerifier.SAME_LINE_SEPARATOR, "  " + USER_EMAIL + "  ", USER_NAME+ " "),
			USER_ID.toString() + "   "
		);
		
		when(mockBufferedReader.lines()).thenReturn(ownerContent.stream());
		
		List<String> result = bucketOwnerVerifier.readOwnerContent(mockObjectReader, BUCKET_NAME, OWNER_KEY);
		List<String> expected = ImmutableList.of(USER_EMAIL, USER_NAME, USER_ID.toString());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetBucketOwnerAliases() {
		
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		
		Set<String> ownerAliases = bucketOwnerVerifier.getBucketOwnerAliases(mockUserInfo);
		
		Set<String> expected = new HashSet<>();

		expected.add(USER_ID.toString());
		expected.addAll(principalAliases.stream()
				.map(PrincipalAlias::getAlias)
				.map(String::toLowerCase)
				.collect(Collectors.toSet())
		);
		
		assertEquals(expected, ownerAliases);
	
	}
	
	@Test
	public void testGetBucketOwnerAliasesExcludingTeams() {
		
		Long teamId = 1234L;
		
		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockPrincipalAliasDao.listPrincipalAliases(USER_ID, AliasType.USER_NAME, AliasType.USER_EMAIL)).thenReturn(principalAliases);
		
		when(mockUserInfo.getGroups()).thenReturn(ImmutableSet.of(
				teamId, 
				BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId(),  
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId())
		);
		
		Set<String> ownerAliases = bucketOwnerVerifier.getBucketOwnerAliases(mockUserInfo);
		
		Set<String> expected = new HashSet<>();

		expected.add(USER_ID.toString());
		expected.add(teamId.toString());
		expected.addAll(principalAliases.stream()
				.map(PrincipalAlias::getAlias)
				.map(String::toLowerCase)
				.collect(Collectors.toSet())
		);
		
		assertEquals(expected, ownerAliases);
	
	}
	
	private BucketOwnerVerifierImpl setupMockObjectReader() {
		
		return setupMockObjectReader(OWNER_KEY);
	}
	
	private BucketOwnerVerifierImpl setupMockObjectReader(String ownerKey) {
		
		when(mockObjectReader.openStream(BUCKET_NAME, ownerKey)).thenReturn(mockInputStream);
		
		BucketOwnerVerifierImpl bucketOwnerVerifierSpy = Mockito.spy(bucketOwnerVerifier);
		
		when(bucketOwnerVerifierSpy.newStreamReader(mockInputStream)).thenReturn(mockBufferedReader);
		
		return bucketOwnerVerifierSpy;
	}
	
}
