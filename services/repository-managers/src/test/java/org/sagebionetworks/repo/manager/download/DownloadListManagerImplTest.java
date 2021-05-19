package org.sagebionetworks.repo.manager.download;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityAccessCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.model.download.ActionReqiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.FilesStatisticsRequest;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortDirection;
import org.sagebionetworks.repo.model.download.SortField;

@ExtendWith(MockitoExtension.class)
public class DownloadListManagerImplTest {

	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private DownloadListDAO mockDownloadListDao;

	@InjectMocks
	private DownloadListManagerImpl manager;

	private UserInfo userOne;
	private UserInfo anonymousUser;
	private AddBatchOfFilesToDownloadListRequest toAddRequest = null;
	private RemoveBatchOfFilesFromDownloadListRequest toRemoveRequest = null;
	private AvailableFilesRequest availableRequest;
	private DownloadListQueryRequest queryRequestBody;
	private AccessContext accessContext;
	private UserEntityPermissionsState permissionsState;
	private UsersRestrictionStatus restrictionStatus;
	private long entityId;
	private long benefactorId;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		userOne = new UserInfo(isAdmin, 222L);
		anonymousUser = new UserInfo(isAdmin, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		toAddRequest = new AddBatchOfFilesToDownloadListRequest();
		List<DownloadListItem> batchOfItems = Arrays.asList(
				new DownloadListItem().setFileEntityId("syn123").setVersionNumber(null),
				new DownloadListItem().setFileEntityId("syn456").setVersionNumber(1L));
		toAddRequest.setBatchToAdd(batchOfItems);

		toRemoveRequest = new RemoveBatchOfFilesFromDownloadListRequest();
		toRemoveRequest.setBatchToRemove(batchOfItems);

		availableRequest = new AvailableFilesRequest();
		availableRequest.setSort(Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.ASC)));
		availableRequest.setNextPageToken(null);
		
		queryRequestBody = new DownloadListQueryRequest();
		queryRequestBody.setRequestDetails(availableRequest);
		
		entityId = 999L;
		benefactorId = 888L;
		permissionsState = new UserEntityPermissionsState(entityId).withBenefactorId(benefactorId);
		restrictionStatus = new UsersRestrictionStatus(entityId, userOne.getId());
		accessContext = new AccessContext().withUser(userOne).withPermissionsState(permissionsState).withRestrictionStatus(restrictionStatus);
	}

	@Test
	public void testAddBatchOfFilesToDownloadList() {
		when(mockDownloadListDao.filterUnsupportedTypes(anyList())).thenReturn(toAddRequest.getBatchToAdd());
		long addedCount = 2L;
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(any(), any())).thenReturn(addedCount);
		long totalNumberOfFilesOnList = 0L;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(totalNumberOfFilesOnList);

		// call under test
		AddBatchOfFilesToDownloadListResponse response = manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		AddBatchOfFilesToDownloadListResponse expected = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(addedCount);
		assertEquals(expected, response);
		verify(mockDownloadListDao).getTotalNumberOfFilesOnDownloadList(userOne.getId());
		verify(mockDownloadListDao).filterUnsupportedTypes(toAddRequest.getBatchToAdd());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), toAddRequest.getBatchToAdd());
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithAtMaxFilesPerUser() {
		when(mockDownloadListDao.filterUnsupportedTypes(anyList())).thenReturn(toAddRequest.getBatchToAdd());
		long addedCount = 2L;
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(any(), any())).thenReturn(addedCount);
		long totalNumberOfFilesOnList = DownloadListManagerImpl.MAX_FILES_PER_USER - addedCount;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(totalNumberOfFilesOnList);

		// call under test
		AddBatchOfFilesToDownloadListResponse response = manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		AddBatchOfFilesToDownloadListResponse expected = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(addedCount);
		assertEquals(expected, response);
		verify(mockDownloadListDao).getTotalNumberOfFilesOnDownloadList(userOne.getId());
		verify(mockDownloadListDao).filterUnsupportedTypes(toAddRequest.getBatchToAdd());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), toAddRequest.getBatchToAdd());
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithOverMaxFilesPerUser() {
		when(mockDownloadListDao.filterUnsupportedTypes(anyList())).thenReturn(toAddRequest.getBatchToAdd());
		long totalNumberOfFilesOnList = DownloadListManagerImpl.MAX_FILES_PER_USER - 1L;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(totalNumberOfFilesOnList);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals(
				"Adding '2' files to your download list would exceed the maximum number of '100000' files.  You currently have '99999' files on you download list.",
				message);

		verify(mockDownloadListDao).getTotalNumberOfFilesOnDownloadList(userOne.getId());
		verify(mockDownloadListDao).filterUnsupportedTypes(toAddRequest.getBatchToAdd());
		verify(mockDownloadListDao, never()).addBatchOfFilesToDownloadList(any(), any());
	}
	
	@Test
	public void testAddBatchOfFilesToDownloadListWithNonFilesFiltered() {
		// filter out the second value
		when(mockDownloadListDao.filterUnsupportedTypes(anyList())).thenReturn(toAddRequest.getBatchToAdd().subList(0, 1));
		long addedCount = 2L;
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(any(), any())).thenReturn(addedCount);
		long totalNumberOfFilesOnList = DownloadListManagerImpl.MAX_FILES_PER_USER - addedCount;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(totalNumberOfFilesOnList);

		// call under test
		AddBatchOfFilesToDownloadListResponse response = manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		AddBatchOfFilesToDownloadListResponse expected = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(addedCount);
		assertEquals(expected, response);
		verify(mockDownloadListDao).getTotalNumberOfFilesOnDownloadList(userOne.getId());
		verify(mockDownloadListDao).filterUnsupportedTypes(toAddRequest.getBatchToAdd());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), toAddRequest.getBatchToAdd().subList(0, 1));
	}
	
	@Test
	public void testAddBatchOfFilesToDownloadListWithFilteredNonFilesOverMaxFilesPerUser() {
		when(mockDownloadListDao.filterUnsupportedTypes(anyList())).thenReturn(toAddRequest.getBatchToAdd().subList(1, 2));
		long totalNumberOfFilesOnList = DownloadListManagerImpl.MAX_FILES_PER_USER;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(totalNumberOfFilesOnList);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals(
				"Adding '1' files to your download list would exceed the maximum number of '100000' files.  You currently have '100000' files on you download list.",
				message);

		verify(mockDownloadListDao).getTotalNumberOfFilesOnDownloadList(userOne.getId());
		verify(mockDownloadListDao).filterUnsupportedTypes(toAddRequest.getBatchToAdd());
		verify(mockDownloadListDao, never()).addBatchOfFilesToDownloadList(any(), any());
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullUser() {
		userOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullRequest() {
		toAddRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals("toAdd is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullBatch() {
		toAddRequest.setBatchToAdd(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals("batch is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithEmptyBatch() {
		toAddRequest.setBatchToAdd(Collections.emptyList());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals("Batch must contain at least one item", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithOverBatchLimit() {
		List<DownloadListItem> batchToAdd = new LinkedList<>();
		for (int i = 0; i < DownloadListManagerImpl.MAX_FILES_PER_BATCH + 1; i++) {
			batchToAdd.add(new DownloadListItem().setFileEntityId("syn" + i));
		}
		toAddRequest.setBatchToAdd(batchToAdd);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.addBatchOfFilesToDownloadList(userOne, toAddRequest);
		}).getMessage();
		assertEquals("Batch size of '1001' exceeds the maximum of '1000'", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			manager.addBatchOfFilesToDownloadList(anonymousUser, toAddRequest);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadList() {

		long removeCount = 2L;
		when(mockDownloadListDao.removeBatchOfFilesFromDownloadList(any(), any())).thenReturn(removeCount);

		// call under test
		RemoveBatchOfFilesFromDownloadListResponse response = manager.removeBatchOfFilesFromDownloadList(userOne,
				toRemoveRequest);
		RemoveBatchOfFilesFromDownloadListResponse expected = new RemoveBatchOfFilesFromDownloadListResponse()
				.setNumberOfFilesRemoved(removeCount);
		assertEquals(expected, response);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(),
				toRemoveRequest.getBatchToRemove());
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithNullUser() {
		userOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.removeBatchOfFilesFromDownloadList(userOne, toRemoveRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithNullRequest() {
		toRemoveRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.removeBatchOfFilesFromDownloadList(userOne, toRemoveRequest);
		}).getMessage();
		assertEquals("toRemove is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithNullBatch() {
		toRemoveRequest.setBatchToRemove(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.removeBatchOfFilesFromDownloadList(userOne, toRemoveRequest);
		}).getMessage();
		assertEquals("batch is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithEmptyBatch() {
		toRemoveRequest.setBatchToRemove(Collections.emptyList());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.removeBatchOfFilesFromDownloadList(userOne, toRemoveRequest);
		}).getMessage();
		assertEquals("Batch must contain at least one item", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithNullAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			manager.removeBatchOfFilesFromDownloadList(anonymousUser, toRemoveRequest);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testClearDownloadList() {
		// call under test
		manager.clearDownloadList(userOne);
		verify(mockDownloadListDao).clearDownloadList(userOne.getId());
	}

	@Test
	public void testClearDownloadListWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.clearDownloadList(anonymousUser);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testCreateAccessCallbackFilter() {
		List<Long> inputIds = Arrays.asList(1L, 2L, 3L);
		List<UsersEntityAccessInfo> accessResults = Arrays.asList(
				new UsersEntityAccessInfo().withEntityId(1L).withAuthroizationStatus(AuthorizationStatus.authorized()),
				new UsersEntityAccessInfo().withEntityId(2L)
						.withAuthroizationStatus(AuthorizationStatus.accessDenied("nope")),
				new UsersEntityAccessInfo().withEntityId(3L).withAuthroizationStatus(AuthorizationStatus.authorized()));
		when(mockEntityAuthorizationManager.batchHasAccess(any(), any(), any())).thenReturn(accessResults);

		EntityAccessCallback callback = manager.createAccessCallback(userOne);
		assertNotNull(callback);

		// call under test
		List<Long> results = callback.filter(inputIds);
		List<Long> expected = Arrays.asList(1L, 3L);
		assertEquals(results, expected);
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, inputIds, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testGetDefaultSort() {
		// call under test
		List<Sort> sort = DownloadListManagerImpl.getDefaultSort();
		List<Sort> expected = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.ASC),
				new Sort().setField(SortField.versionNumber).setDirection(SortDirection.ASC));
		assertEquals(expected, sort);
	}

	@Test
	public void testQueryAvailableFiles() {
		List<DownloadListItemResult> resultPage = Arrays.asList(
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"),
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		assertNull(response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(availableRequest.getSort()), eq(51L), eq(0L));
	}
	
	@Test
	public void testQueryAvailableFilesWithNullRequest() {
		List<DownloadListItemResult> resultPage = Arrays
				.asList((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
						(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"),
						(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		
		availableRequest = null;
		List<Sort> expectedSort = DownloadListManagerImpl.getDefaultSort();
		
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		assertNull(response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(expectedSort), eq(51L), eq(0L));
	}
	
	@Test
	public void testQueryAvailableFilesWithNullSort() {
		List<DownloadListItemResult> resultPage = Arrays
				.asList((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
						(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"),
						(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		
		availableRequest.setSort(null);
		List<Sort> expectedSort = DownloadListManagerImpl.getDefaultSort();
		
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		assertNull(response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(expectedSort), eq(51L), eq(0L));
	}
	
	@Test
	public void testQueryAvailableFilesWithEmptySort() {
		List<DownloadListItemResult> resultPage = Arrays
				.asList((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
						(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"),
						(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		
		availableRequest.setSort(Collections.emptyList());
		List<Sort> expectedSort = DownloadListManagerImpl.getDefaultSort();
		
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		assertNull(response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(expectedSort), eq(51L), eq(0L));
	}
	
	@Test
	public void testQueryAvailableFilesWithNextPageToken() {
		List<DownloadListItemResult> resultPage = new ArrayList<>(4);
		resultPage.add((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"));
		resultPage.add((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"));
		resultPage.add((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));

		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		
		long limit = 2;
		long offset = 0;
		availableRequest.setNextPageToken(new NextPageToken(limit, offset).toToken());
		
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		// results will be trimmed due to the next page token
		assertEquals(2, response.getPage().size());
		String expectedNextPageToken = new NextPageToken(2L, 2L).toToken();
		assertEquals(expectedNextPageToken, response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		long expectedLimit = 3L;
		long expectedOffest = 0L;
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(availableRequest.getSort()), eq(expectedLimit), eq(expectedOffest));
	}
	
	@Test
	public void testQueryAvailableFilesWithNullUser() {
		userOne = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.queryAvailableFiles(userOne, availableRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}
	
	@Test
	public void testGetListStatisticsWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.getListStatistics(anonymousUser, new FilesStatisticsRequest());
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}
	
	@Test
	public void testQueryDownloadListWithAvaiable() {
		
		FilesStatisticsResponse details = new FilesStatisticsResponse();
		details.setNumberOfFilesAvailableForDownload(2L);
		details.setNumberOfFilesRequiringAction(3L);
		details.setTotalNumberOfFiles(5L);
		details.setTotalNumberOfFiles(123L);

		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupStatisticsCallback(details, ids);
		
		queryRequestBody.setRequestDetails(new FilesStatisticsRequest());
		
		// call under test
		DownloadListQueryResponse response = manager.queryDownloadList(userOne, queryRequestBody);
		assertNotNull(response);
		assertEquals(details, response.getResponseDetails());
		verify(mockDownloadListDao).getListStatistics(any(), eq(userOne.getId()));
	}
	
	@Test
	public void testQueryDownloadListWithNullRequest() {
		queryRequestBody = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.queryDownloadList(userOne, queryRequestBody);
		}).getMessage();
		assertEquals("requestBody is required.", message);
	}
	
	@Test
	public void testQueryAvailableFilesWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.queryAvailableFiles(anonymousUser, availableRequest);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}
	
	@Test
	public void testQueryDownloadListWithStatistics() {
		
		List<DownloadListItemResult> resultPage = new ArrayList<>(4);
		resultPage.add((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"));
		resultPage.add((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"));
		resultPage.add((DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		AvailableFilesResponse expectedAvailable = new AvailableFilesResponse().setNextPageToken(null).setPage(resultPage);

		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		
		// call under test
		DownloadListQueryResponse response = manager.queryDownloadList(userOne, queryRequestBody);
		assertNotNull(response);
		assertEquals(expectedAvailable, response.getResponseDetails());
	}
	
	@Test
	public void testQueryDownloadListWithRequiresAction() {
		List<ActionRequiredCount> page = Arrays.asList(new ActionRequiredCount().setNumberOfFilesRequiringAction(3L));
		ActionReqiredRequest request = new ActionReqiredRequest();
		queryRequestBody.setRequestDetails(request);
		when(mockDownloadListDao.getActionsRequiredFromDownloadList(any(), any(), any(), any())).thenReturn(page);
	
		ActionRequiredResponse expected = new ActionRequiredResponse();
		expected.setNextPageToken(null);
		expected.setPage(page);
		
		// call under test
		DownloadListQueryResponse response = manager.queryDownloadList(userOne, queryRequestBody);
		assertNotNull(response);
		assertEquals(expected, response.getResponseDetails());
		
		Long limit = 51L;
		Long offest = 0L;
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit), eq(offest));
	}
	
	@Test
	public void testCreateActionRequiredWithAuthorized() {
		List<UsersEntityAccessInfo> batchInfo = Arrays.asList(
				new UsersEntityAccessInfo(accessContext,
						AuthorizationStatus.authorized()));

		List<FileActionRequired> expected = Collections.emptyList();
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithNoRestrictions() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		
		List<FileActionRequired> expected = Arrays.asList(new FileActionRequired().withFileId(entityId)
				.withAction(new RequestDownload().setBenefactorId(benefactorId)));
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithMetRestrictions() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L));
		restrictionStatus.setHasUnmet(false);

		List<FileActionRequired> expected = Arrays.asList(new FileActionRequired().withFileId(entityId)
				.withAction(new RequestDownload().setBenefactorId(benefactorId)));
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithMixedRestrictions() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(321L));
		restrictionStatus.setHasUnmet(true);

		List<FileActionRequired> expected = Arrays.asList(new FileActionRequired().withFileId(entityId)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(321L)));
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithNullRestrictionStatus() {
		accessContext.withRestrictionStatus(null);
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			DownloadListManagerImpl.createActionRequired(batchInfo);
		}).getMessage();

		assertEquals("info.accessRestrictions() is required.", message);
	}
	
	
	@Test
	public void testCreateActionRequiredWithMultipleUnmetRestrictions() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(432L));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(321L));
		restrictionStatus.setHasUnmet(true);

		List<FileActionRequired> expected = Arrays.asList(
				new FileActionRequired().withFileId(entityId)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(432L)),
				new FileActionRequired().withFileId(entityId)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(321L))
		);
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testQueryActionRequired() {
		List<ActionRequiredCount> page = Arrays.asList(new ActionRequiredCount().setNumberOfFilesRequiringAction(3L));
		ActionReqiredRequest request = new ActionReqiredRequest();
		when(mockDownloadListDao.getActionsRequiredFromDownloadList(any(), any(), any(), any())).thenReturn(page);
		// call under test
		ActionRequiredResponse resonse = manager.queryActionRequired(userOne, request);
		assertNotNull(resonse);
		assertEquals(page, resonse.getPage());
		assertNull(resonse.getNextPageToken());
		Long limit = 51L;
		Long offest = 0L;
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit), eq(offest));
	}
	
	@Test
	public void testQueryActionRequiredWithNextPage() {
		Long limit = 10L;
		Long offset = 20L;
		List<ActionRequiredCount> page = createActionRequiredPageOfSize((int) (limit+1));
		ActionReqiredRequest request = new ActionReqiredRequest().setNextPageToken(new NextPageToken(limit,offset).toToken());
		when(mockDownloadListDao.getActionsRequiredFromDownloadList(any(), any(), any(), any())).thenReturn(page);
		// call under test
		ActionRequiredResponse resonse = manager.queryActionRequired(userOne, request);
		assertNotNull(resonse);
		assertEquals(page, resonse.getPage());
		Long nextOffset = 30L;
		assertEquals(new NextPageToken(limit,nextOffset).toToken(), resonse.getNextPageToken());
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit+1L), eq(offset));
	}
	
	@Test
	public void testQueryActionRequiredWithAnonymous() {
		ActionReqiredRequest request = new ActionReqiredRequest();
		String message = assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.queryActionRequired(anonymousUser, request);
		}).getMessage();
		assertEquals("You must login to access your download list", message);
		verify(mockDownloadListDao, never()).getActionsRequiredFromDownloadList(any(), any(), any(), any());
	}
	
	/**
	 * Helper to create a page of results of the given size.
	 * @param size
	 * @return
	 */
	List<ActionRequiredCount> createActionRequiredPageOfSize(int size){
		List<ActionRequiredCount> page = new ArrayList<>(size);
		for(int i=0; i<size; i++) {
			page.add(new ActionRequiredCount().setNumberOfFilesRequiringAction(new Long(i)));
		}
		return page;
	}

	/**
	 * Helper to setup both the return of getFilesAvailableToDownloadFromDownloadList() 
	 * and ensure that the callback is called with the provided forwardToCallback
	 * @param results
	 * @param forwardToCallback A list of IDs to be forwarded to the callback.
	 */
	public void setupAvailableCallback(List<DownloadListItemResult> results, List<Long> forwardToCallback) {
		doAnswer((InvocationOnMock invocation) -> {
			EntityAccessCallback accessCallback = invocation.getArgument(0);
			accessCallback.filter(forwardToCallback);
			return results;
		}).when(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any());
	}
	
	/**
	 * 
	 * @param reponse
	 * @param forwardToCallback
	 */
	public void setupStatisticsCallback(FilesStatisticsResponse reponse, List<Long> forwardToCallback) {
		doAnswer((InvocationOnMock invocation) -> {
			EntityAccessCallback accessCallback = invocation.getArgument(0);
			accessCallback.filter(forwardToCallback);
			return reponse;
		}).when(mockDownloadListDao).getListStatistics(any(), any());
	}

}
