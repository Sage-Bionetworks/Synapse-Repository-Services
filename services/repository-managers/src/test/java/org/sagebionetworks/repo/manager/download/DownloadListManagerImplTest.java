package org.sagebionetworks.repo.manager.download;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.authentication.TwoFactorAuthManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandlePackageManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityAccessCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.model.dbo.file.download.v2.ManifestKeys;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.AvailableFilter;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.EnableTwoFa;
import org.sagebionetworks.repo.model.download.FilesStatisticsRequest;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortDirection;
import org.sagebionetworks.repo.model.download.SortField;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.ZipFileFormat;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.util.FileHandler;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class DownloadListManagerImplTest {

	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private TwoFactorAuthManager mockTwofactorAuthManager;
	@Mock
	private DownloadListDAO mockDownloadListDao;
	@Mock
	private TableQueryManager mockTableQueryManager;
	@Mock
	private ProgressCallback mockProgressCallback;
	@Mock
	private FileHandlePackageManager mockFileHandlePackageManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private File mockFile;
	@Mock
	private File mockFileTwo;
	@Mock
	private BufferedWriter mockBufferedWritter;
	@Mock
	private BufferedReader mockBufferedReader;
	@Mock
	private JSONObject mockDetails;
	@Mock
	private CSVWriter mockCSVWriter;
	@Mock
	private NodeDAO mockNodeDao;
	@Captor
	private ArgumentCaptor<Iterator<DownloadListItemResult>>  iteratorCaptor;
	@Captor
	private ArgumentCaptor<FileHandler<String>> fileHandlerStringCaptor;
	@Captor
	private ArgumentCaptor<Set<String>> annotationNamesCaptor;
	@Captor
	private ArgumentCaptor<String[]> rowCaptor;

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
	private AddToDownloadListRequest addRequest;
	private AddToDownloadListResponse addResponse;
	private DownloadListItemResult downloadListItemResult;
	private List<DownloadListItemResult> downloadListItems;
	private BulkFileDownloadResponse bulkFileDownloadResponse;
	private CsvTableDescriptor csvTableDescriptor;
	private DownloadListManifestRequest downloadListManifestRequest;
	private boolean fileSizesChecked;
	
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
		permissionsState = new UserEntityPermissionsState(entityId).withBenefactorId(benefactorId).withDoesEntityExist(true);
		restrictionStatus = new UsersRestrictionStatus(entityId, userOne.getId());
		accessContext = new AccessContext().withUser(userOne).withPermissionsState(permissionsState)
				.withRestrictionStatus(restrictionStatus);

		addRequest = new AddToDownloadListRequest();
		addResponse = new AddToDownloadListResponse().setNumberOfFilesAdded(2L);

		downloadListItemResult = (DownloadListItemResult) new DownloadListItemResult().setFileHandleId("987")
				.setFileSizeBytes(101L).setFileEntityId("syn123");
		downloadListItems = Arrays.asList(downloadListItemResult);
		bulkFileDownloadResponse = new BulkFileDownloadResponse().setResultZipFileHandleId("987");
		
		csvTableDescriptor = new CsvTableDescriptor().setSeparator(",");
		downloadListManifestRequest = new DownloadListManifestRequest().setCsvTableDescriptor(csvTableDescriptor);
		fileSizesChecked = true;
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
		when(mockDownloadListDao.filterUnsupportedTypes(anyList()))
				.thenReturn(toAddRequest.getBatchToAdd().subList(0, 1));
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
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				toAddRequest.getBatchToAdd().subList(0, 1));
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithFilteredNonFilesOverMaxFilesPerUser() {
		when(mockDownloadListDao.filterUnsupportedTypes(anyList()))
				.thenReturn(toAddRequest.getBatchToAdd().subList(1, 2));
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
				new UsersEntityAccessInfo().withEntityId(1L).withAuthorizationStatus(AuthorizationStatus.authorized()),
				new UsersEntityAccessInfo().withEntityId(2L)
						.withAuthorizationStatus(AuthorizationStatus.accessDenied("nope")),
				new UsersEntityAccessInfo().withEntityId(3L).withAuthorizationStatus(AuthorizationStatus.authorized()));
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
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(null),
				eq(availableRequest.getSort()), eq(51L), eq(0L));
	}

	@Test
	public void testQueryAvailableFilesWithNullFilter() {
		List<DownloadListItemResult> resultPage = Arrays.asList(
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"),
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		availableRequest.setFilter(null);
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		assertNull(response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(null),
				eq(availableRequest.getSort()), eq(51L), eq(0L));
	}

	@Test
	public void testQueryAvailableFilesWithFilter() {
		List<DownloadListItemResult> resultPage = Arrays.asList(
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn2"),
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn3"));
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);
		availableRequest.setFilter(AvailableFilter.eligibleForPackaging);
		// call under test
		AvailableFilesResponse response = manager.queryAvailableFiles(userOne, availableRequest);
		assertNotNull(response);
		assertEquals(resultPage, response.getPage());
		assertNull(response.getNextPageToken());
		verify(mockEntityAuthorizationManager).batchHasAccess(userOne, ids, ACCESS_TYPE.DOWNLOAD);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging), eq(availableRequest.getSort()), eq(51L), eq(0L));
	}

	@Test
	public void testQueryAvailableFilesWithNullRequest() {
		List<DownloadListItemResult> resultPage = Arrays.asList(
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
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
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(null),
				eq(expectedSort), eq(51L), eq(0L));
	}

	@Test
	public void testQueryAvailableFilesWithNullSort() {
		List<DownloadListItemResult> resultPage = Arrays.asList(
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
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
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(null),
				eq(expectedSort), eq(51L), eq(0L));
	}

	@Test
	public void testQueryAvailableFilesWithEmptySort() {
		List<DownloadListItemResult> resultPage = Arrays.asList(
				(DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn1"),
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
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(null),
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
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(null),
				eq(availableRequest.getSort()), eq(expectedLimit), eq(expectedOffest));
	}

	@Test
	public void testQueryAvailableFilesWithNullUser() {
		userOne = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.queryAvailableFiles(userOne, availableRequest);
		}).getMessage();
		assertEquals("userInfo is required.", message);
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testGetListStatisticsWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
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
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.queryDownloadList(userOne, queryRequestBody);
		}).getMessage();
		assertEquals("requestBody is required.", message);
	}

	@Test
	public void testQueryAvailableFilesWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
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
		AvailableFilesResponse expectedAvailable = new AvailableFilesResponse().setNextPageToken(null)
				.setPage(resultPage);

		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		setupAvailableCallback(resultPage, ids);

		// call under test
		DownloadListQueryResponse response = manager.queryDownloadList(userOne, queryRequestBody);
		assertNotNull(response);
		assertEquals(expectedAvailable, response.getResponseDetails());
	}

	@Test
	public void testQueryDownloadListWithRequiresAction() {
		List<ActionRequiredCount> page = Arrays.asList(new ActionRequiredCount().setCount(3L));
		ActionRequiredRequest request = new ActionRequiredRequest();
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
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit),
				eq(offest));
		
	}

	@Test
	public void testCreateActionRequiredWithAuthorized() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.authorized()));

		List<FileActionRequired> expected = Collections.emptyList();
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}

	@Test
	public void testCreateActionRequiredWithNoRestrictions() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));

		List<FileActionRequired> expected = Arrays.asList(new FileActionRequired().withFileId(entityId)
				.withAction(new RequestDownload().setBenefactorId(benefactorId)));
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}

	@Test
	public void testCreateActionRequiredWithNonExistentEntity() {
		permissionsState.withDoesEntityExist(false);

		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		
		List<FileActionRequired> expected = Arrays.asList();
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
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
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
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
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}

	@Test
	public void testCreateActionRequiredWithNullRestrictionStatus() {
		accessContext.withRestrictionStatus(null);
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		
		boolean hasTwoFactorAuthEnabled = false;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
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
						.withAction(new MeetAccessRequirement().setAccessRequirementId(321L)));
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithUnmetRestrictionsAndUnmetTwoFaRestriction() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(432L).withIsTwoFaRequired(false));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(true).withRequirementId(789L).withIsTwoFaRequired(true));
		restrictionStatus.setHasUnmet(true);

		List<FileActionRequired> expected = Arrays.asList(
			new FileActionRequired().withFileId(entityId)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(432L)),
			new FileActionRequired().withFileId(entityId)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(789L)),
			new FileActionRequired().withFileId(entityId)
				.withAction(new EnableTwoFa().setAccessRequirementId(789L))
		);
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithMetTwoFaRestriction() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L).withIsTwoFaRequired(true));
		restrictionStatus.setHasUnmet(false);

		List<FileActionRequired> expected = Arrays.asList(new FileActionRequired().withFileId(entityId)
				.withAction(new RequestDownload().setBenefactorId(benefactorId)));
		
		boolean hasTwoFactorAuthEnabled = true;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testCreateActionRequiredWithUnmetTwoFaRestriction() {
		List<UsersEntityAccessInfo> batchInfo = Arrays
				.asList(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")));
		restrictionStatus.addRestrictionStatus(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L).withIsTwoFaRequired(true));
		restrictionStatus.setHasUnmet(false);

		List<FileActionRequired> expected = Arrays.asList(new FileActionRequired().withFileId(entityId)
				.withAction(new EnableTwoFa().setAccessRequirementId(432L)));
		
		boolean hasTwoFactorAuthEnabled = false;
		
		// call under test
		List<FileActionRequired> actions = DownloadListManagerImpl.createActionRequired(batchInfo, hasTwoFactorAuthEnabled);
		assertEquals(expected, actions);
	}
	
	@Test
	public void testQueryActionRequired() {
		List<ActionRequiredCount> page = Arrays.asList(new ActionRequiredCount().setCount(3L));
		ActionRequiredRequest request = new ActionRequiredRequest();
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
		List<ActionRequiredCount> page = createActionRequiredPageOfSize((int) (limit + 1));
		ActionRequiredRequest request = new ActionRequiredRequest()
				.setNextPageToken(new NextPageToken(limit, offset).toToken());
		when(mockDownloadListDao.getActionsRequiredFromDownloadList(any(), any(), any(), any())).thenReturn(page);
		
		// call under test
		ActionRequiredResponse resonse = manager.queryActionRequired(userOne, request);
		assertNotNull(resonse);
		assertEquals(page, resonse.getPage());
		Long nextOffset = 30L;
		assertEquals(new NextPageToken(limit, nextOffset).toToken(), resonse.getNextPageToken());
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit + 1L),
				eq(offset));
		
	}

	@Test
	public void testQueryActionRequiredWithAnonymous() {
		ActionRequiredRequest request = new ActionRequiredRequest();
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.queryActionRequired(anonymousUser, request);
		}).getMessage();
		assertEquals("You must login to access your download list", message);
		verify(mockDownloadListDao, never()).getActionsRequiredFromDownloadList(any(), any(), any(), any());
	}

	/**
	 * Helper to create a page of results of the given size.
	 * 
	 * @param size
	 * @return
	 */
	List<ActionRequiredCount> createActionRequiredPageOfSize(int size) {
		List<ActionRequiredCount> page = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			page.add(new ActionRequiredCount().setCount(new Long(i)));
		}
		return page;
	}

	/**
	 * Helper to setup both the return of
	 * getFilesAvailableToDownloadFromDownloadList() and ensure that the callback is
	 * called with the provided forwardToCallback
	 * 
	 * @param results
	 * @param forwardToCallback A list of IDs to be forwarded to the callback.
	 */
	public void setupAvailableCallback(List<DownloadListItemResult> results, List<Long> forwardToCallback) {
		doAnswer((InvocationOnMock invocation) -> {
			EntityAccessCallback accessCallback = invocation.getArgument(0);
			accessCallback.filter(forwardToCallback);
			return results;
		}).when(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(),
				any());
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

	@Test
	public void testAddToDownloadListWithQuery() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setQuery(new Query());
		addRequest.setUseVersionNumber(null);
		doReturn(addResponse).when(managerSpy).addQueryResultsToDownloadList(any(), any(), any(Query.class),
				anyBoolean(), anyLong(), anyLong());
		long numberOfFilesOnDownloadList = 0L;
		long usersDownloadListCapactity = DownloadListManagerImpl.MAX_FILES_PER_USER - numberOfFilesOnDownloadList;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(numberOfFilesOnDownloadList);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addQueryResultsToDownloadList(mockProgressCallback, userOne, addRequest.getQuery(), true,
				DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE, usersDownloadListCapactity);
	}

	@Test
	public void testAddToDownloadListWithQueryWithVersion() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setQuery(new Query().setSql("select * from syn123"));
		addRequest.setUseVersionNumber(false);
		doReturn(addResponse).when(managerSpy).addQueryResultsToDownloadList(any(), any(), any(Query.class),
				anyBoolean(), anyLong(), anyLong());
		long numberOfFilesOnDownloadList = 101L;
		long usersDownloadListCapactity = DownloadListManagerImpl.MAX_FILES_PER_USER - numberOfFilesOnDownloadList;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(numberOfFilesOnDownloadList);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addQueryResultsToDownloadList(mockProgressCallback, userOne, addRequest.getQuery(), false,
				DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE, usersDownloadListCapactity);
	}

	@Test
	public void testAddToDownloadListWithFolder() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(null);
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(UserInfo.class), any(String.class), anyBoolean(),
				anyLong());
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(0L);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), true,
				DownloadListManagerImpl.MAX_FILES_PER_USER);
	}

	@Test
	public void testAddToDownloadListWithFolderWithVersion() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(false);
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(), any(String.class), anyBoolean(), anyLong());
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(0L);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), false,
				DownloadListManagerImpl.MAX_FILES_PER_USER);
	}

	@Test
	public void testAddToDownloadListWithFolderWithUnderLimit() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(true);
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(), any(String.class), anyBoolean(), anyLong());
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any()))
				.thenReturn(DownloadListManagerImpl.MAX_FILES_PER_USER - 1);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		long expectedLimit = 1L;
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), true, expectedLimit);
	}

	@Test
	public void testAddToDownloadListWithFolderWithAtLimit() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(false);
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any()))
				.thenReturn(DownloadListManagerImpl.MAX_FILES_PER_USER);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		}).getMessage();
		assertEquals(String.format(DownloadListManagerImpl.YOUR_DOWNLOAD_LIST_ALREADY_HAS_THE_MAXIMUM_NUMBER_OF_FILES,
				DownloadListManagerImpl.MAX_FILES_PER_USER), message);
		verify(managerSpy, never()).addToDownloadList(any(), anyString(), anyBoolean(), anyLong());
	}

	@Test
	public void testAddToDownloadListWithAnonymous() {
		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.addToDownloadList(mockProgressCallback, anonymousUser, addRequest);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
	}

	@Test
	public void testAddToDownloadListWithNullQueryAndFolderId() {
		addRequest.setParentId(null);
		addRequest.setQuery(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addToDownloadList(mockProgressCallback, userOne, addRequest);
		}).getMessage();
		assertEquals("Must include either request.parentId or request.query().", message);
	}

	@Test
	public void testAddToDownloadListWithQueryAndFolderId() {
		addRequest.setParentId("syn123");
		addRequest.setQuery(new Query());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addToDownloadList(mockProgressCallback, userOne, addRequest);
		}).getMessage();
		assertEquals("Please provide request.parentId or request.query() but not both.", message);
	}

	@Test
	public void testAddToDownloadListWithNullRequest() {
		addRequest = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addToDownloadList(mockProgressCallback, userOne, addRequest);
		}).getMessage();
		assertEquals("requestBody is required.", message);
	}

	@Test
	public void testAddToDownloadListFolder() {
		Long count = 99L;
		String parentId = "syn123";
		boolean useVersion = false;
		long limit = 100L;
		when(mockDownloadListDao.addChildrenToDownloadList(any(), anyLong(), anyBoolean(), anyLong()))
				.thenReturn(count);
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.folder);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		// Call under test
		AddToDownloadListResponse response = manager.addToDownloadList(userOne, parentId, useVersion, limit);
		AddToDownloadListResponse expected = new AddToDownloadListResponse().setNumberOfFilesAdded(count);
		assertEquals(expected, response);
		verify(mockDownloadListDao).addChildrenToDownloadList(userOne.getId(), 123L, useVersion, limit);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testAddToDownloadListWithDatasetAsParentId() {
		Long count = 2L;
		String parentId = "syn123";
		long limit = 100L;
		List<EntityRef> items = Arrays.asList(new EntityRef().setEntityId("123"),
				new EntityRef().setEntityId("234"));
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.dataset);
		when(mockNodeDao.getNodeItems(any())).thenReturn(items);
		when(mockDownloadListDao.addDatasetItemsToDownloadList(any(), any(), anyLong()))
				.thenReturn(count);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		// Call under test
		AddToDownloadListResponse response = manager.addToDownloadList(userOne, parentId, true, limit);
		AddToDownloadListResponse expected = new AddToDownloadListResponse().setNumberOfFilesAdded(count);
		assertEquals(expected, response);
		verify(mockNodeDao).getNodeItems(123L);
		verify(mockDownloadListDao).addDatasetItemsToDownloadList(userOne.getId(), items, limit);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}

	@Test
	public void testAddToDownloadListFolderWithUnauthorized() {
		String parentId = "syn123";
		boolean useVersion = false;
		long limit = 100L;
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));
		assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.addToDownloadList(userOne, parentId, useVersion, limit);
		});
		verifyNoMoreInteractions(mockDownloadListDao);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}

	@Test
	public void testCloneQuery() {
		Query query = new Query().setSql("select * from syn123").setIncludeEntityEtag(true).setLimit(101L).setOffset(0L)
				.setSort(Arrays.asList(new SortItem().setColumn("foo")))
				.setSelectedFacets(Arrays.asList(new FacetColumnRangeRequest().setColumnName("bar")))
				.setAdditionalFilters(Arrays.asList(new ColumnSingleValueQueryFilter().setColumnName("foobar")));
		// call under test
		Query result = DownloadListManagerImpl.cloneQuery(query);
		assertFalse(query == result);
		Query expected = new Query().setSql("select * from syn123").setIncludeEntityEtag(true).setLimit(101L)
				.setOffset(0L).setSort(Arrays.asList(new SortItem().setColumn("foo")))
				.setSelectedFacets(Arrays.asList(new FacetColumnRangeRequest().setColumnName("bar")))
				.setAdditionalFilters(Arrays.asList(new ColumnSingleValueQueryFilter().setColumnName("foobar")));
		assertEquals(expected, result);
	}

	@Test
	public void testCreateDownloadsListItemFromRowWithVersionFalse() {
		boolean userVersion = false;
		Row row = new Row().setRowId(123L).setVersionNumber(4L);
		// call under test
		DownloadListItem result = manager.createDownloadsListItemFromRow(userVersion, row);
		assertEquals(new DownloadListItem().setFileEntityId("123").setVersionNumber(null), result);
	}

	@Test
	public void testCreateDownloadsListItemFromRowWithVersionTrue() {
		boolean userVersion = true;
		Row row = new Row().setRowId(123L).setVersionNumber(4L);
		// call under test
		DownloadListItem result = manager.createDownloadsListItemFromRow(userVersion, row);
		assertEquals(new DownloadListItem().setFileEntityId("123").setVersionNumber(4L), result);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithVersionTrue() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setRowId(111L).setVersionNumber(1L),
				new Row().setRowId(222L).setVersionNumber(2L)
		);
		// @formatter:on
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(
				new QueryResultBundle().setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(rows))));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, maxQueryPageSize, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).querySinglePage(any(), any(), any(), any());
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithUserVersionFalse() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setRowId(111L).setVersionNumber(1L),
				new Row().setRowId(222L).setVersionNumber(2L)
		);
		// @formatter:on
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(
				new QueryResultBundle().setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(rows))));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, maxQueryPageSize, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).querySinglePage(any(), any(), any(), any());
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(null),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(null)));
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithDataset() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setRowId(111L).setVersionNumber(1L),
				new Row().setRowId(222L).setVersionNumber(2L)
		);
		// @formatter:on
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.dataset);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(
				new QueryResultBundle().setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(rows))));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, maxQueryPageSize, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).querySinglePage(any(), any(), any(), any());
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithCapacityLessThanPageSize() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setRowId(111L).setVersionNumber(1L),
				new Row().setRowId(222L).setVersionNumber(2L)
		);
		// @formatter:on
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(
				new QueryResultBundle().setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(rows))));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 2L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, maxQueryPageSize, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).querySinglePage(any(), any(), any(), any());
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(usersDownloadListCapacity).setOffset(0L),
				new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false)
						.withReturnLastUpdatedOn(false));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(null),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(null)));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithCapacityMoreThanPageSize() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setRowId(111L).setVersionNumber(1L),
				new Row().setRowId(222L).setVersionNumber(2L)
		);
		// @formatter:on
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(
				new QueryResultBundle().setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(rows))));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long maxQueryPageSize = 5L;
		long usersDownloadListCapacity = 101L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, maxQueryPageSize, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).querySinglePage(any(), any(), any(), any());
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(maxQueryPageSize).setOffset(0L),
				new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false)
						.withReturnLastUpdatedOn(false));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(null),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(null)));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithMultiplePages() throws Exception {
		// @formatter:off
		List<Row> pageOne = Arrays.asList(
				new Row().setRowId(111L).setVersionNumber(1L),
				new Row().setRowId(222L).setVersionNumber(2L)
		);
		List<Row> pageTwo = Arrays.asList(
				new Row().setRowId(333L).setVersionNumber(3L),
				new Row().setRowId(444L).setVersionNumber(4L)
		);
		List<Row> pageThree = Arrays.asList(
				new Row().setRowId(555L).setVersionNumber(5L)
		);
		// @formatter:on
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenReturn(
				new QueryResultBundle()
						.setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(pageOne))),
				new QueryResultBundle()
						.setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(pageTwo))),
				new QueryResultBundle()
						.setQueryResult(new QueryResult().setQueryResults(new RowSet().setRows(pageThree))));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(2L, 2L, 1L);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 2L;
		long usersDownloadListCapacity = 101L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, maxQueryPageSize, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(5L), result);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(3)).querySinglePage(any(), any(), any(), any());
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(maxQueryPageSize).setOffset(0L),
				new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false)
						.withReturnLastUpdatedOn(false));
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(maxQueryPageSize).setOffset(2L),
				new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false)
						.withReturnLastUpdatedOn(false));
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(maxQueryPageSize).setOffset(4L),
				new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false)
						.withReturnLastUpdatedOn(false));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("333").setVersionNumber(3L),
						new DownloadListItem().setFileEntityId("444").setVersionNumber(4L)));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("555").setVersionNumber(5L)));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithNonViewOrDataset() throws Exception {
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.table);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		}).getMessage();
		assertEquals("'syn123' is not a file view or a dataset", message);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager, never()).querySinglePage(any(), any(), any(), any());
		verify(mockDownloadListDao, never()).addBatchOfFilesToDownloadList(anyLong(), any());
	}

	@Test
	public void testAddQueryResultsToDownloadListWithBadQuery() throws Exception {
		Query query = new Query().setSql("this is not sql");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		}).getMessage();
		assertTrue(message.contains("<regular_identifier> \"this \"\" at line 1, column 1."));

		verifyNoMoreInteractions(mockTableQueryManager, mockDownloadListDao);
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithJoiny() throws Exception {
		Query query = new Query().setSql("select * from syn123 join syn456");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);

		verifyNoMoreInteractions(mockTableQueryManager, mockDownloadListDao);
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithNullQuery() throws Exception {
		Query query = new Query().setSql(null);
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		}).getMessage();
		assertEquals("query.sql is required.", message);

		verifyNoMoreInteractions(mockTableQueryManager, mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithTableUnavailable() throws Exception {

		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenThrow(new TableUnavailableException(new TableStatus().setState(TableState.PROCESSING_FAILED)));

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		});

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithLockUnavailableException() throws Exception {

		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any()))
				.thenThrow(new LockUnavilableException("no lock for you"));

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		});

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithTableFailedException() throws Exception {
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		TableFailedException exception = new TableFailedException(
				new TableStatus().setState(TableState.PROCESSING_FAILED));
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenThrow(exception);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		Throwable cause = assertThrows(RuntimeException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		}).getCause();
		assertEquals(exception, cause);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithDatastoreException() throws Exception {
		when(mockTableQueryManager.getTableEntityType(any())).thenReturn(TableType.entityview);
		DatastoreException exception = new DatastoreException("wrong");
		when(mockTableQueryManager.querySinglePage(any(), any(), any(), any())).thenThrow(exception);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long maxQueryPageSize = 10L;
		long usersDownloadListCapacity = 100L;
		Throwable cause = assertThrows(RuntimeException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, maxQueryPageSize,
					usersDownloadListCapacity);
		}).getCause();
		assertEquals(exception, cause);

		verify(mockTableQueryManager).getTableEntityType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).querySinglePage(mockProgressCallback, userOne,
				new Query().setSql("SELECT ROW_ID FROM syn123").setLimit(10L).setOffset(0L), new QueryOptions()
						.withRunQuery(true).withRunCount(false).withReturnFacets(false).withReturnLastUpdatedOn(false));
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testCreateAssociationForItem() {

		// call under test
		FileHandleAssociation fha = DownloadListManagerImpl.createAssociationForItem(downloadListItemResult);
		FileHandleAssociation expected = new FileHandleAssociation()
				.setAssociateObjectId(downloadListItemResult.getFileEntityId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setFileHandleId(downloadListItemResult.getFileHandleId());
		assertEquals(expected, fha);
	}

	@Test
	public void testPackageFiles() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean())).thenReturn(bulkFileDownloadResponse);

		// call under test
		DownloadListPackageResponse response = managerSpy.packageFiles(mockProgressCallback, userOne, request);

		DownloadListPackageResponse expected = new DownloadListPackageResponse()
				.setResultFileHandleId(bulkFileDownloadResponse.getResultZipFileHandleId());
		assertEquals(expected, response);
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult)));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		List<DownloadListItem> expectedRemoveItems = Arrays.asList(downloadListItemResult);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(), expectedRemoveItems);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithNullIncludeManifest() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		request.setIncludeManifest(null);
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean())).thenReturn(bulkFileDownloadResponse);

		// call under test
		DownloadListPackageResponse response = managerSpy.packageFiles(mockProgressCallback, userOne, request);

		DownloadListPackageResponse expected = new DownloadListPackageResponse()
				.setResultFileHandleId(bulkFileDownloadResponse.getResultZipFileHandleId());
		assertEquals(expected, response);
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult)));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		List<DownloadListItem> expectedRemoveItems = Arrays.asList(downloadListItemResult);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(), expectedRemoveItems);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithIncludeManifestFalse() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		request.setIncludeManifest(false);
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean())).thenReturn(bulkFileDownloadResponse);

		// call under test
		DownloadListPackageResponse response = managerSpy.packageFiles(mockProgressCallback, userOne, request);

		DownloadListPackageResponse expected = new DownloadListPackageResponse()
				.setResultFileHandleId(bulkFileDownloadResponse.getResultZipFileHandleId());
		assertEquals(expected, response);
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult)));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		List<DownloadListItem> expectedRemoveItems = Arrays.asList(downloadListItemResult);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(), expectedRemoveItems);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithIncludeManifest() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		request.setIncludeManifest(true);
		request.setCsvTableDescriptor(csvTableDescriptor);
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean())).thenReturn(bulkFileDownloadResponse);
		String manifestFileHandleId = "999";
		doReturn(manifestFileHandleId).when(managerSpy).buildManifest(any(), any(), any());
		FileHandleAssociation expectedManifestAssociation = new FileHandleAssociation()
				.setFileHandleId(manifestFileHandleId).setAssociateObjectType(FileHandleAssociateType.FileEntity)
				.setAssociateObjectId(DownloadListManagerImpl.ZERO_FILE_ID);

		// call under test
		DownloadListPackageResponse response = managerSpy.packageFiles(mockProgressCallback, userOne, request);

		DownloadListPackageResponse expected = new DownloadListPackageResponse()
				.setResultFileHandleId(bulkFileDownloadResponse.getResultZipFileHandleId());
		assertEquals(expected, response);
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult),
								expectedManifestAssociation));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		List<DownloadListItem> expectedRemoveItems = Arrays.asList(downloadListItemResult);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(), expectedRemoveItems);
				
		verify(managerSpy).buildManifest(eq(userOne), eq(csvTableDescriptor), iteratorCaptor.capture());
		Iterator<DownloadListItemResult> iterator = iteratorCaptor.getValue();
		assertTrue(iterator.hasNext());
		assertEquals(downloadListItemResult, iterator.next());
		assertFalse(iterator.hasNext());
	}
	
	@Test
	public void testPackageFilesWithFileSizeAtLimit() throws IOException {
		downloadListItemResult.setFileSizeBytes(FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES);
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean())).thenReturn(bulkFileDownloadResponse);

		// call under test
		DownloadListPackageResponse response = managerSpy.packageFiles(mockProgressCallback, userOne, request);

		DownloadListPackageResponse expected = new DownloadListPackageResponse()
				.setResultFileHandleId(bulkFileDownloadResponse.getResultZipFileHandleId());
		assertEquals(expected, response);
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult)));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		List<DownloadListItem> expectedRemoveItems = Arrays.asList(downloadListItemResult);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(), expectedRemoveItems);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithFileSizeOverLimit() throws IOException {
		downloadListItemResult.setFileSizeBytes(FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES+1);
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
		.thenReturn(downloadListItems);

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.packageFiles(mockProgressCallback, userOne, request);
		}).getMessage();
		
		assertEquals(DownloadListManagerImpl.NO_FILES_ARE_ELIGIBLE_FOR_PACKAGING, message);
		
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		
		verifyNoMoreInteractions(mockFileHandlePackageManager);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithEmptyDownloadList() throws IOException {
		downloadListItemResult.setFileSizeBytes(FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES+1);
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
		.thenReturn(Collections.emptyList());

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			managerSpy.packageFiles(mockProgressCallback, userOne, request);
		}).getMessage();
		
		assertEquals(DownloadListManagerImpl.NO_FILES_ARE_ELIGIBLE_FOR_PACKAGING, message);
		
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		
		verifyNoMoreInteractions(mockFileHandlePackageManager);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithAnonymous() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();

		String message = assertThrows(UnauthorizedException.class, ()->{
			// call under test
			managerSpy.packageFiles(mockProgressCallback, anonymousUser, request);
		}).getMessage();
		
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);
		verifyNoMoreInteractions(mockDownloadListDao);
		verifyNoMoreInteractions(mockFileHandlePackageManager);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithFilesWithDuplicateFileHandleIds() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		
		// two files with the same file handle ID
		DownloadListItemResult one = (DownloadListItemResult) new DownloadListItemResult().setFileHandleId("987")
				.setFileSizeBytes(101L).setFileEntityId("syn123").setVersionNumber(1L);
		DownloadListItemResult two = (DownloadListItemResult) new DownloadListItemResult().setFileHandleId("987")
				.setFileSizeBytes(101L).setFileEntityId("syn123").setVersionNumber(2L);
		
		downloadListItems = Arrays.asList(one, two);
		
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean())).thenReturn(bulkFileDownloadResponse);

		// call under test
		DownloadListPackageResponse response = managerSpy.packageFiles(mockProgressCallback, userOne, request);

		DownloadListPackageResponse expected = new DownloadListPackageResponse()
				.setResultFileHandleId(bulkFileDownloadResponse.getResultZipFileHandleId());
		assertEquals(expected, response);
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(one)));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		// both files should get deleted
		List<DownloadListItem> expectedRemoveItems = Arrays.asList(one, two);
		verify(mockDownloadListDao).removeBatchOfFilesFromDownloadList(userOne.getId(), expectedRemoveItems);
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}

	@Test
	public void testCreateManifest() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems, Collections.emptyList());
		String fileHandleId = "999";
		doReturn(fileHandleId).when(managerSpy).buildManifest(any(), any(), any());
		
		// call under test
		DownloadListManifestResponse response = managerSpy.createManifest(mockProgressCallback, userOne,
				downloadListManifestRequest);
		
		assertEquals(new DownloadListManifestResponse().setResultFileHandleId(fileHandleId), response);
		verify(managerSpy).buildManifest(eq(userOne), eq(downloadListManifestRequest.getCsvTableDescriptor()),
				iteratorCaptor.capture());
		Iterator<DownloadListItemResult> iterator = iteratorCaptor.getValue();
		assertTrue(iterator.hasNext());
		assertEquals(downloadListItemResult, iterator.next());
		assertFalse(iterator.hasNext());
		verify(managerSpy, times(1)).createAccessCallback(userOne);
		AvailableFilter filter = null;
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileName).setDirection(SortDirection.ASC));
		// pane one
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()), eq(filter),
				eq(sort), eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
	}
	
	@Test
	public void testCreateManifestWithAnonymous() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		String message = assertThrows(UnauthorizedException.class, ()->{
			// call under test
			managerSpy.createManifest(mockProgressCallback, anonymousUser,
					downloadListManifestRequest);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST, message);

		verify(managerSpy, never()).buildManifest(any(), any(), any());
		verify(managerSpy, never()).createAccessCallback(any());
	}
	
	@Test
	public void testBuildManifest() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		when(mockFileProvider.createBufferedWriter(any(), any())).thenReturn(mockBufferedWritter);
		when(mockDownloadListDao.getItemManifestDetails(any())).thenReturn(mockDetails);
		LinkedHashMap<String, Integer> keyToIndexMap = new LinkedHashMap<String, Integer>();
		doReturn(keyToIndexMap).when(managerSpy).mapKeysToColumnIndex(any());
		String fileHandleId = "999";
		doReturn(fileHandleId).when(managerSpy).buildManifestCSV(any(), any(), any(), any());
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		
		// start with the default keys
		Set<String> keys = Stream.of(ManifestKeys.values()).map(k->k.name()).collect(Collectors.toSet());
		keys.add("one");
		keys.add("two");
		keys.add("three");
		when(mockDetails.keySet()).thenReturn(keys);
		
		String detailsJson = "{\"a\":[1,2]}";
		when(mockDetails.toString()).thenReturn(detailsJson);
		
		// call under test
		String resultFileHandleId = managerSpy.buildManifest(userOne, csvTableDescriptor, downloadListItems.iterator());
		assertEquals(fileHandleId, resultFileHandleId);
		
		verify(mockFileProvider).createTemporaryFile(eq("items"), eq(".txt"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String result = handler.apply(mockFile);
		assertEquals(fileHandleId, result);
		
		verify(mockFileProvider).createBufferedWriter(mockFile, StandardCharsets.UTF_8);
		verify(mockBufferedWritter).close();
		
		verify(mockBufferedWritter).append(detailsJson);
		verify(mockBufferedWritter).newLine();
		
		verify(managerSpy).mapKeysToColumnIndex(annotationNamesCaptor.capture());
		// only the annotation keys should have been captured.
		assertEquals(Sets.newHashSet("one","two","three"), annotationNamesCaptor.getValue());
		
		verify(managerSpy).buildManifestCSV(userOne, csvTableDescriptor, mockFile, keyToIndexMap);
		
	}
	
	
	@Test
	public void testBuildManifestWithNoFiles() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		when(mockFileProvider.createBufferedWriter(any(), any())).thenReturn(mockBufferedWritter);
		String fileHandleId = "999";
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		
		// call under test part one
		String resultFileHandleId = managerSpy.buildManifest(userOne, csvTableDescriptor, Collections.emptyIterator());
		assertEquals(fileHandleId, resultFileHandleId);
		
		verify(mockFileProvider).createTemporaryFile(eq("items"), eq(".txt"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test part two
			handler.apply(mockFile);
		}).getMessage();
		assertEquals(DownloadListManagerImpl.NO_FILES_AVAILABLE_FOR_DOWNLOAD, message);
	
		verify(mockFileProvider).createBufferedWriter(mockFile, StandardCharsets.UTF_8);
		verify(mockBufferedWritter).close();
		
		verify(mockBufferedWritter, never()).append(any());
		verify(mockBufferedWritter, never()).newLine();
		
		verify(managerSpy, never()).mapKeysToColumnIndex(any());
		verify(managerSpy, never()).buildManifestCSV(any(), any(), any(), any());
	}
	
	@Test
	public void testMapKeysToColumnIndex() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b","c","a");
		// call under test
		LinkedHashMap<String, Integer> result = managerSpy.mapKeysToColumnIndex(annotationNames);
		LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
		int index = 0;
		for(ManifestKeys defaultKey: ManifestKeys.values()) {
			expected.put(defaultKey.name(), index);
			index++;
		}
		// annotations added in alphabetical order at the end.
		expected.put("a", index++);
		expected.put("b", index++);
		expected.put("c", index++);
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildManifestCSVWithComma() throws IOException {
		csvTableDescriptor.setSeparator(",");
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");

		String fileHandleId = "999";
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		when(mockFileProvider.createBufferedReader(any(), any())).thenReturn(mockBufferedReader);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

		doReturn(mockCSVWriter).when(managerSpy).createCSVWriter(any(), any());
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		// call under test
		String resultFileHandleId = managerSpy.buildManifestCSV(userOne, csvTableDescriptor, mockFile, keyToIndexMap);
		assertEquals(fileHandleId, resultFileHandleId);

		verify(mockFileProvider).createTemporaryFile(eq("manifest"), eq(".csv"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String result = handler.apply(mockFileTwo);
		assertEquals(fileHandleId, result);

		verify(mockFileProvider).createBufferedReader(mockFile, StandardCharsets.UTF_8);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest().withContentType("text/csv")
				.withFileName("manifest.csv").withFileToUpload(mockFileTwo).withUserId(userOne.getId().toString()));
		boolean includeHeader = true;
		verify(managerSpy).copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);
	}
	
	@Test
	public void testBuildManifestCSVWithNullSepeartor() throws IOException {
		csvTableDescriptor.setSeparator(null);
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");

		String fileHandleId = "999";
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		when(mockFileProvider.createBufferedReader(any(), any())).thenReturn(mockBufferedReader);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

		doReturn(mockCSVWriter).when(managerSpy).createCSVWriter(any(), any());
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		// call under test
		String resultFileHandleId = managerSpy.buildManifestCSV(userOne, csvTableDescriptor, mockFile, keyToIndexMap);
		assertEquals(fileHandleId, resultFileHandleId);

		verify(mockFileProvider).createTemporaryFile(eq("manifest"), eq(".csv"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String result = handler.apply(mockFileTwo);
		assertEquals(fileHandleId, result);

		verify(mockFileProvider).createBufferedReader(mockFile, StandardCharsets.UTF_8);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest().withContentType("text/csv")
				.withFileName("manifest.csv").withFileToUpload(mockFileTwo).withUserId(userOne.getId().toString()));
		boolean includeHeader = true;
		verify(managerSpy).copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);
	}
	
	@Test
	public void testBuildManifestCSVWithNullDescriptor() throws IOException {
		csvTableDescriptor = null;
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");

		String fileHandleId = "999";
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		when(mockFileProvider.createBufferedReader(any(), any())).thenReturn(mockBufferedReader);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

		doReturn(mockCSVWriter).when(managerSpy).createCSVWriter(any(), any());
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		// call under test
		String resultFileHandleId = managerSpy.buildManifestCSV(userOne, csvTableDescriptor, mockFile, keyToIndexMap);
		assertEquals(fileHandleId, resultFileHandleId);

		verify(mockFileProvider).createTemporaryFile(eq("manifest"), eq(".csv"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String result = handler.apply(mockFileTwo);
		assertEquals(fileHandleId, result);

		verify(mockFileProvider).createBufferedReader(mockFile, StandardCharsets.UTF_8);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest().withContentType("text/csv")
				.withFileName("manifest.csv").withFileToUpload(mockFileTwo).withUserId(userOne.getId().toString()));
		boolean includeHeader = true;
		verify(managerSpy).copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);
	}
	
	@Test
	public void testBuildManifestCSVWithTab() throws IOException {
		csvTableDescriptor.setSeparator("\t");
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		String fileHandleId = "999";
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		when(mockFileProvider.createBufferedReader(any(), any())).thenReturn(mockBufferedReader);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

		doReturn(mockCSVWriter).when(managerSpy).createCSVWriter(any(), any());


		// call under test
		String resultFileHandleId = managerSpy.buildManifestCSV(userOne, csvTableDescriptor, mockFile, keyToIndexMap);
		assertEquals(fileHandleId, resultFileHandleId);

		verify(mockFileProvider).createTemporaryFile(eq("manifest"), eq(".tsv"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String result = handler.apply(mockFileTwo);
		assertEquals(fileHandleId, result);

		verify(mockFileProvider).createBufferedReader(mockFile, StandardCharsets.UTF_8);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest().withContentType("text/tsv")
				.withFileName("manifest.tsv").withFileToUpload(mockFileTwo).withUserId(userOne.getId().toString()));
		boolean includeHeader = true;
		verify(managerSpy).copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);
	}
	
	@Test
	public void testBuildManifestCSVWithNoHeader() throws IOException {
		csvTableDescriptor.setIsFirstLineHeader(false);
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		String fileHandleId = "999";
		when(mockFileProvider.createTemporaryFile(any(), any(), any())).thenReturn(fileHandleId);
		when(mockFileProvider.createBufferedReader(any(), any())).thenReturn(mockBufferedReader);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(new S3FileHandle().setId(fileHandleId));

		doReturn(mockCSVWriter).when(managerSpy).createCSVWriter(any(), any());


		// call under test
		String resultFileHandleId = managerSpy.buildManifestCSV(userOne, csvTableDescriptor, mockFile, keyToIndexMap);
		assertEquals(fileHandleId, resultFileHandleId);

		verify(mockFileProvider).createTemporaryFile(eq("manifest"), eq(".csv"), fileHandlerStringCaptor.capture());
		FileHandler<String> handler = fileHandlerStringCaptor.getValue();
		String result = handler.apply(mockFileTwo);
		assertEquals(fileHandleId, result);

		verify(mockFileProvider).createBufferedReader(mockFile, StandardCharsets.UTF_8);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest().withContentType("text/csv")
				.withFileName("manifest.csv").withFileToUpload(mockFileTwo).withUserId(userOne.getId().toString()));
		boolean includeHeader = false;
		verify(managerSpy).copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);
	}

	@Test
	public void testCopyFromTextToCSV() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		JSONObject itemOne = createJSONObjectForItem(1);
		itemOne.put("a", "one");
		itemOne.put("c", "two");
		JSONObject itemTwo = createJSONObjectForItem(2);
		itemTwo.put("c", "three");
		itemTwo.put("b", "four");
		when(mockBufferedReader.readLine()).thenReturn(itemOne.toString(), itemTwo.toString(), null);
		boolean includeHeader = true;

		// call under test
		managerSpy.copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);

		List<String> headerList = Stream.of(ManifestKeys.values()).map(k -> k.name()).collect(Collectors.toList());
		headerList.addAll(Arrays.asList("a", "b", "c"));

		verify(mockCSVWriter, times(3)).writeNext(any());
		// First row is the header
		verify(mockCSVWriter).writeNext(new String[] { "ID", "name", "versionNumber", "contentType",
				"dataFileSizeBytes", "createdBy", "createdOn", "modifiedBy", "modifiedOn", "parentId", "synapseURL",
				"dataFileMD5Hex", "a", "b", "c" });
		verify(mockCSVWriter).writeNext(new String[] { "1-0", "1-1", "1-2", "1-3", "1-4", "1-5", "1-6", "1-7", "1-8",
				"1-9", "1-10", "1-11", "one", null, "two" });
		verify(mockCSVWriter).writeNext(new String[] { "2-0", "2-1", "2-2", "2-3", "2-4", "2-5", "2-6", "2-7", "2-8",
				"2-9", "2-10", "2-11", null, "four", "three" });
	}
	
	@Test
	public void testCopyFromTextToCSVWithNoHeader() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		Set<String> annotationNames = Sets.newHashSet("b", "c", "a");
		LinkedHashMap<String, Integer> keyToIndexMap = managerSpy.mapKeysToColumnIndex(annotationNames);

		JSONObject itemOne = createJSONObjectForItem(1);
		itemOne.put("a", "one");
		itemOne.put("c", "two");
		JSONObject itemTwo = createJSONObjectForItem(2);
		itemTwo.put("c", "three");
		itemTwo.put("b", "four");
		when(mockBufferedReader.readLine()).thenReturn(itemOne.toString(), itemTwo.toString(), null);
		boolean includeHeader = false;

		// call under test
		managerSpy.copyFromTextToCSV(keyToIndexMap, mockBufferedReader, mockCSVWriter, includeHeader);

		List<String> headerList = Stream.of(ManifestKeys.values()).map(k -> k.name()).collect(Collectors.toList());
		headerList.addAll(Arrays.asList("a", "b", "c"));

		// First row is the header
		verify(mockCSVWriter, times(2)).writeNext(any());
		verify(mockCSVWriter).writeNext(new String[] { "1-0", "1-1", "1-2", "1-3", "1-4", "1-5", "1-6", "1-7", "1-8",
				"1-9", "1-10", "1-11", "one", null, "two" });
		verify(mockCSVWriter).writeNext(new String[] { "2-0", "2-1", "2-2", "2-3", "2-4", "2-5", "2-6", "2-7", "2-8",
				"2-9", "2-10", "2-11", null, "four", "three" });
	}
	
	/**
	 * Create a test JSONObject populated from the manifest.
	 * 
	 * @param index
	 * @return
	 */
	public JSONObject createJSONObjectForItem(int index) {
		JSONObject itemOne = new JSONObject();
		int i = 0;
		for(ManifestKeys key: ManifestKeys.values()) {
			itemOne.put(key.name(), index+"-"+i);
			i++;
		}
		return itemOne;
	}
}
