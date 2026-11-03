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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils.createColumn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.authentication.TwoFactorAuthManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandlePackageManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.table.RowHandlerProvider;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeConstants.BOOTSTRAP_NODES;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityAccessCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityActionRequiredCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileAndSize;
import org.sagebionetworks.repo.model.dbo.file.download.v2.ManifestKeys;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsResponse;
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
import org.sagebionetworks.repo.model.download.FilesStatisticsRequest;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
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
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.FileHandler;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockType;
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
	private TableManagerSupport mockTableManagerSupport;
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
		userOne = new UserInfo(isAdmin, 222L, AuthorizationConstants.DEFAULT_REALM_ID);
		anonymousUser = new UserInfo(isAdmin, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(), AuthorizationConstants.DEFAULT_REALM_ID);
		anonymousUser.setRealmAnonymousUserId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
				
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
		when(mockDownloadListDao.filterItemsByType(anyList(), any())).thenReturn(toAddRequest.getBatchToAdd());
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
		verify(mockDownloadListDao).filterItemsByType(toAddRequest.getBatchToAdd(), List.of(EntityType.file, EntityType.recordset));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), toAddRequest.getBatchToAdd());
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithAtMaxFilesPerUser() {
		when(mockDownloadListDao.filterItemsByType(anyList(), any())).thenReturn(toAddRequest.getBatchToAdd());
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
		verify(mockDownloadListDao).filterItemsByType(toAddRequest.getBatchToAdd(), List.of(EntityType.file, EntityType.recordset));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), toAddRequest.getBatchToAdd());
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithOverMaxFilesPerUser() {
		when(mockDownloadListDao.filterItemsByType(anyList(), any())).thenReturn(toAddRequest.getBatchToAdd());
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
		verify(mockDownloadListDao).filterItemsByType(toAddRequest.getBatchToAdd(), List.of(EntityType.file, EntityType.recordset));
		verify(mockDownloadListDao, never()).addBatchOfFilesToDownloadList(any(), any());
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNonFilesFiltered() {
		// filter out the second value
		when(mockDownloadListDao.filterItemsByType(anyList(), any()))
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
		verify(mockDownloadListDao).filterItemsByType(toAddRequest.getBatchToAdd(), List.of(EntityType.file, EntityType.recordset));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				toAddRequest.getBatchToAdd().subList(0, 1));
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithFilteredNonFilesOverMaxFilesPerUser() {
		when(mockDownloadListDao.filterItemsByType(anyList(), any()))
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
		verify(mockDownloadListDao).filterItemsByType(toAddRequest.getBatchToAdd(), List.of(EntityType.file, EntityType.recordset));
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
	public void testQueryActionRequired() {
		
		List<ActionRequiredCount> page = Arrays.asList(new ActionRequiredCount().setCount(3L));
		
		List<Long> ids = Arrays.asList(1L, 2L, 3L);
		
		setupActionRequiredCallback(page, ids);
		
		ActionRequiredRequest request = new ActionRequiredRequest();
				
		// call under test
		ActionRequiredResponse resonse = manager.queryActionRequired(userOne, request);
		assertNotNull(resonse);
		assertEquals(page, resonse.getPage());
		assertNull(resonse.getNextPageToken());
		Long limit = 51L;
		Long offest = 0L;
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit), eq(offest));
		verify(mockEntityAuthorizationManager).getActionsRequiredForDownload(userOne, ids);
		
	}

	@Test
	public void testQueryActionRequiredWithNextPage() {
		Long limit = 10L;
		Long offset = 20L;
		List<ActionRequiredCount> page = createActionRequiredPageOfSize((int) (limit + 1));
		
		List<Long> ids = List.of(1L, 2L, 3L);
		
		setupActionRequiredCallback(page, ids);
		
		ActionRequiredRequest request = new ActionRequiredRequest().setNextPageToken(new NextPageToken(limit, offset).toToken());
				
		// call under test
		ActionRequiredResponse resonse = manager.queryActionRequired(userOne, request);
		assertNotNull(resonse);
		assertEquals(page, resonse.getPage());
		Long nextOffset = 30L;
		assertEquals(new NextPageToken(limit, nextOffset).toToken(), resonse.getNextPageToken());
		
		verify(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), eq(userOne.getId()), eq(limit + 1L),
				eq(offset));
		
		verify(mockEntityAuthorizationManager).getActionsRequiredForDownload(userOne, ids);
		
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
			page.add(new ActionRequiredCount().setCount(Long.valueOf(i)));
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

	public void setupActionRequiredCallback(List<ActionRequiredCount> response, List<Long> forwardToCallback) {
		doAnswer((InvocationOnMock invocation) -> {
			EntityActionRequiredCallback accessCallback = invocation.getArgument(0);
			accessCallback.filter(forwardToCallback);
			return response;
		}).when(mockDownloadListDao).getActionsRequiredFromDownloadList(any(), any(), any(), any());
	}

	/**
	 * The size of each file row of the query results that {@link #setupGetFilesWithSizeAsAllFiles()} and
	 * {@link #setupItemTypes(Map...)} setup.
	 */
	static final long FILE_ROW_SIZE = 10L;

	/**
	 * The current version that the same helpers give each file row without a version.
	 */
	static final long FILE_ROW_CURRENT_VERSION = 1L;

	/**
	 * Helper to create a file row as returned by {@link DownloadListDAO#getFilesWithSize(List, boolean)}:
	 * {@link #FILE_ROW_SIZE} bytes, at its own version or, without one, at
	 * {@link #FILE_ROW_CURRENT_VERSION}.
	 */
	static FileAndSize fileRow(DownloadListItem item) {
		long actualVersion = item.getVersionNumber() != null ? item.getVersionNumber() : FILE_ROW_CURRENT_VERSION;
		return new FileAndSize(item, actualVersion, FILE_ROW_SIZE);
	}

	/**
	 * Helper to setup {@link DownloadListDAO#getFilesWithSize(List, boolean)} to treat every item in the
	 * given batch as a {@link #fileRow(DownloadListItem) file row}.
	 */
	public void setupGetFilesWithSizeAsAllFiles() {
		when(mockDownloadListDao.getFilesWithSize(any(), anyBoolean())).thenAnswer(invocation -> {
			List<DownloadListItem> batch = invocation.getArgument(0);
			return batch.stream().map(DownloadListManagerImplTest::fileRow).collect(Collectors.toList());
		});
	}

	/**
	 * Helper to create an {@link IdAndVersion}.
	 */
	static IdAndVersion idAndVersion(long id, Long version) {
		return IdAndVersion.newBuilder().setId(id).setVersion(version).build();
	}

	/**
	 * Helper to create a file of a Dataset, as returned by
	 * {@link DownloadListDAO#getFilesInDatasets(List, long)}.
	 */
	static FileAndSize member(String id, long version, long sizeBytes) {
		return new FileAndSize(new DownloadListItem().setFileEntityId(id).setVersionNumber(version), version, sizeBytes);
	}

	/**
	 * Helper to create a one byte file of a Dataset, at the given version, for each id from start
	 * (inclusive) to end (exclusive).
	 */
	static List<FileAndSize> memberRange(long start, long end, long version) {
		return LongStream.range(start, end).mapToObj(id -> member("syn" + id, version, 1L)).collect(Collectors.toList());
	}

	/**
	 * Helper to create a query result row, holding the id and version 1, for each id from start
	 * (inclusive) to end (exclusive).
	 */
	static List<Row> rowRange(long start, long end) {
		return LongStream.range(start, end).mapToObj(id -> new Row().setValues(List.of(String.valueOf(id), "1")))
				.collect(Collectors.toList());
	}

	/**
	 * Helper to create the download list item that {@link #rowRange(long, long)} rows are read as.
	 */
	static List<DownloadListItem> itemRange(long start, long end) {
		return LongStream.range(start, end)
				.mapToObj(id -> new DownloadListItem().setFileEntityId(String.valueOf(id)).setVersionNumber(1L))
				.collect(Collectors.toList());
	}

	/**
	 * Helper to setup {@link TableQueryManager#runQueryAsStream(ProgressCallback, UserInfo, Query, RowHandlerProvider, ACCESS_TYPE...)}
	 * to stream the given rows to the handler that the caller provides.
	 */
	private void setupRunQueryAsStream(List<Row> rows) throws Exception {
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any())).thenAnswer(invocation -> {
			RowHandlerProvider provider = invocation.getArgument(3);
			try (RowHandler handler = provider.getHandler(null)) {
				rows.forEach(handler::nextRow);
			}
			return new QueryResultBundle();
		});
	}

	/**
	 * Helper to setup the DAO lookups of the entity type of each item of a batch of query results.
	 * {@link DownloadListDAO#getFilesWithSize(List, boolean)} keeps each item listed under one of
	 * {@link EntityTypeUtils#getFileTypes()}, as a {@link #fileRow(DownloadListItem) file row}, and
	 * {@link DownloadListDAO#filterItemsByType(List, Collection)} keeps each item listed under one of the
	 * requested types. An item that is not listed is treated as an entity that does not exist.
	 *
	 * @param itemsByType The items of each type, for example one map per batch of query results.
	 */
	@SafeVarargs
	private void setupItemTypes(Map<EntityType, List<DownloadListItem>>... itemsByType) {
		Map<String, EntityType> idToType = new HashMap<>();
		for (Map<EntityType, List<DownloadListItem>> page : itemsByType) {
			page.forEach((type, items) -> items.forEach(item -> idToType.put(item.getFileEntityId(), type)));
		}
		when(mockDownloadListDao.getFilesWithSize(any(), anyBoolean())).thenAnswer(invocation -> {
			List<DownloadListItem> batch = invocation.getArgument(0);
			return batch.stream()
					.filter(item -> isOfType(idToType, item, EntityTypeUtils.getFileTypes()))
					.map(DownloadListManagerImplTest::fileRow)
					.collect(Collectors.toList());
		});
		when(mockDownloadListDao.filterItemsByType(any(), any())).thenAnswer(invocation -> {
			List<DownloadListItem> batch = invocation.getArgument(0);
			Collection<EntityType> types = invocation.getArgument(1);
			return batch.stream().filter(item -> isOfType(idToType, item, types)).collect(Collectors.toList());
		});
	}

	private static boolean isOfType(Map<String, EntityType> idToType, DownloadListItem item, Collection<EntityType> types) {
		return idToType.containsKey(item.getFileEntityId()) && types.contains(idToType.get(item.getFileEntityId()));
	}

	@Test
	public void testAddToDownloadListWithQuery() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setQuery(new Query());
		addRequest.setUseVersionNumber(null);
		doReturn(addResponse).when(managerSpy).addQueryResultsToDownloadList(any(), any(), any(Query.class),
				anyBoolean(), anyLong());
		long numberOfFilesOnDownloadList = 0L;
		long usersDownloadListCapactity = DownloadListManagerImpl.MAX_FILES_PER_USER - numberOfFilesOnDownloadList;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(numberOfFilesOnDownloadList);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addQueryResultsToDownloadList(mockProgressCallback, userOne, addRequest.getQuery(), true, usersDownloadListCapactity);
	}

	@Test
	public void testAddToDownloadListWithQueryWithVersion() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setQuery(new Query().setSql("select * from syn123"));
		addRequest.setUseVersionNumber(false);
		doReturn(addResponse).when(managerSpy).addQueryResultsToDownloadList(any(), any(), any(Query.class),
				anyBoolean(), anyLong());
		long numberOfFilesOnDownloadList = 101L;
		long usersDownloadListCapactity = DownloadListManagerImpl.MAX_FILES_PER_USER - numberOfFilesOnDownloadList;
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(numberOfFilesOnDownloadList);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addQueryResultsToDownloadList(mockProgressCallback, userOne, addRequest.getQuery(), false, usersDownloadListCapactity);
	}

	@Test
	public void testAddToDownloadListWithFolder() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(null);
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(UserInfo.class), any(String.class), anyBoolean(), anyBoolean(),
				anyLong());
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(0L);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), true, false, DownloadListManagerImpl.MAX_FILES_PER_USER);
	}

	@Test
	public void testAddToDownloadListWithFolderWithVersion() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(false);
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(), any(String.class), anyBoolean(), anyBoolean(), anyLong());
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(0L);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), false, false, DownloadListManagerImpl.MAX_FILES_PER_USER);
	}

	@Test
	public void testAddToDownloadListWithFolderWithUnderLimit() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(true);
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(), any(String.class), anyBoolean(), anyBoolean(), anyLong());
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any()))
				.thenReturn(DownloadListManagerImpl.MAX_FILES_PER_USER - 1);
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		assertEquals(addResponse, response);
		long expectedLimit = 1L;
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), true, false, expectedLimit);
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
		verify(managerSpy, never()).addToDownloadList(any(), anyString(), anyBoolean(), anyBoolean(), anyLong());
	}
	
	@Test
	public void testAddToDownloadListWithFolderAndRecursive() {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		addRequest.setParentId("syn123");
		addRequest.setUseVersionNumber(null);
		addRequest.setRecursive(true);
		
		doReturn(addResponse).when(managerSpy).addToDownloadList(any(UserInfo.class), any(String.class), anyBoolean(), anyBoolean(), anyLong());
		
		when(mockDownloadListDao.getTotalNumberOfFilesOnDownloadList(any())).thenReturn(0L);
		
		// call under test
		AddToDownloadListResponse response = managerSpy.addToDownloadList(mockProgressCallback, userOne, addRequest);
		
		assertEquals(addResponse, response);
		
		verify(managerSpy).addToDownloadList(userOne, addRequest.getParentId(), true, true, DownloadListManagerImpl.MAX_FILES_PER_USER);
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
	public void testAddToDownloadListWithQueryAndRecurisve() {
		addRequest.setQuery(new Query()).setRecursive(true);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addToDownloadList(mockProgressCallback, userOne, addRequest);
		}).getMessage();
		
		assertEquals("The recursive option is only supported when specifying a parentId.", message);
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
		boolean recursive = false;
		long limit = 100L;
		when(mockDownloadListDao.addChildrenToDownloadList(any(), anyLong(), anyBoolean(), anyLong())).thenReturn(count);
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.folder);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		// Call under test
		AddToDownloadListResponse response = manager.addToDownloadList(userOne, parentId, useVersion, recursive, limit);
		AddToDownloadListResponse expected = new AddToDownloadListResponse().setNumberOfFilesAdded(count);
		assertEquals(expected, response);
		verify(mockDownloadListDao).addChildrenToDownloadList(userOne.getId(), 123L, useVersion, limit);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testAddToDownloadListFolderAndRecursive() {
		Long count = 99L;
		String parentId = "syn123";
		boolean useVersion = false;
		boolean recursive = true;
		long limit = 100L;
		when(mockDownloadListDao.addDescendantsToDownloadList(any(), anyLong(), anyBoolean(), anyLong())).thenReturn(count);
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.folder);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		// Call under test
		AddToDownloadListResponse response = manager.addToDownloadList(userOne, parentId, useVersion, recursive, limit);
		AddToDownloadListResponse expected = new AddToDownloadListResponse().setNumberOfFilesAdded(count);
		assertEquals(expected, response);
		verify(mockDownloadListDao).addDescendantsToDownloadList(userOne.getId(), 123L, useVersion, limit);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testAddToDownloadListWithDatasetAsParentId() {
		Long count = 2L;
		String parentId = "syn123";
		boolean recursive = false;
		long limit = 100L;
		List<EntityRef> items = Arrays.asList(new EntityRef().setEntityId("123"),
				new EntityRef().setEntityId("234"));
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.dataset);
		when(mockNodeDao.getNodeItems(any())).thenReturn(items);
		when(mockDownloadListDao.addFileEntityRefToDownloadList(any(), any(), anyLong()))
				.thenReturn(count);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		// Call under test
		AddToDownloadListResponse response = manager.addToDownloadList(userOne, parentId, true, recursive, limit);
		AddToDownloadListResponse expected = new AddToDownloadListResponse().setNumberOfFilesAdded(count);
		assertEquals(expected, response);
		verify(mockNodeDao).getNodeItems(123L);
		verify(mockDownloadListDao).addFileEntityRefToDownloadList(userOne.getId(), items, limit);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testAddToDownloadListWithDatasetAndRecursive() {
		String parentId = "syn123";
		boolean recursive = true;
		long limit = 100L;
		
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.dataset);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any())) .thenReturn(AuthorizationStatus.authorized());
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.addToDownloadList(userOne, parentId, true, recursive, limit);
		}).getMessage();
		
		assertEquals("The recursive option is not supported for a dataset.", errorMessage);
		
		verifyNoMoreInteractions(mockNodeDao, mockDownloadListDao);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testAddToDownloadListWithDatasetCollectionAsParentId() {
		Long count = 2L;
		String parentId = "syn123";
		boolean recursive = false;
		long limit = 100L;
		List<EntityRef> items = Arrays.asList(new EntityRef().setEntityId("123"),
				new EntityRef().setEntityId("234"));
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.datasetcollection);
		when(mockNodeDao.getNodeItems(any())).thenReturn(items);
		when(mockDownloadListDao.addDatasetEntityRefFilesToDownloadList(any(), any(), anyLong()))
				.thenReturn(count);
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.authorized());
		// Call under test
		AddToDownloadListResponse response = manager.addToDownloadList(userOne, parentId, true, recursive, limit);
		AddToDownloadListResponse expected = new AddToDownloadListResponse().setNumberOfFilesAdded(count);
		assertEquals(expected, response);
		verify(mockNodeDao).getNodeItems(123L);
		verify(mockDownloadListDao).addDatasetEntityRefFilesToDownloadList(userOne.getId(), items, limit);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}

	@Test
	public void testAddToDownloadListFolderWithUnauthorized() {
		String parentId = "syn123";
		boolean useVersion = false;
		boolean recursive = false;
		long limit = 100L;
		when(mockEntityAuthorizationManager.hasAccess(any(), any(), any()))
				.thenReturn(AuthorizationStatus.accessDenied("nope"));
		assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.addToDownloadList(userOne, parentId, useVersion, recursive, limit);
		});
		verifyNoMoreInteractions(mockDownloadListDao);
		verify(mockEntityAuthorizationManager).hasAccess(userOne, parentId, ACCESS_TYPE.READ);
	}
	
	@ParameterizedTest
	@EnumSource(NodeConstants.BOOTSTRAP_NODES.class)
	public void testAddToDownloadListFolderWithBootstrapNodes(BOOTSTRAP_NODES node) {
		String parentId = node.getId().toString();
		boolean useVersion = false;
		boolean recursive = false;
		long limit = 100L;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.addToDownloadList(userOne, parentId, useVersion, recursive, limit);
		}).getMessage();
		
		assertEquals("Invalid parentId.", errorMessage);
		
		verifyNoMoreInteractions(mockDownloadListDao, mockEntityAuthorizationManager);
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
	public void testCreateDownloadsListItemFromRowValues() {
		boolean includeVersion = false;
		
		List<String> rowValues = List.of("123");
		
		// call under test
		DownloadListItem result = manager.createDownloadsListItemFromRowValues(rowValues, includeVersion);
		
		assertEquals(new DownloadListItem().setFileEntityId("123").setVersionNumber(null), result);
	}
	
	@Test
	public void testCreateDownloadsListItemFromRowValuesWithIncludeVersion() {
		boolean includeVersion = true;
		
		List<String> rowValues = List.of("123", "456");
		
		// call under test
		DownloadListItem result = manager.createDownloadsListItemFromRowValues(rowValues, includeVersion);
		
		assertEquals(new DownloadListItem().setFileEntityId("123").setVersionNumber(456L), result);
	}
	
	@Test
	public void testCreateDownloadsListItemFromRowValuesWithIncludeVersionAndWrongSize() {
		boolean includeVersion = true;
		
		List<String> rowValues = List.of("123");
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			manager.createDownloadsListItemFromRowValues(rowValues, includeVersion);
		}).getMessage();
		
		assertEquals("Expected at least two elements in row values.", message);
	}
	
	@Test
	public void testCreateDownloadsListItemFromRowValuesWithEmptyList() {
		boolean includeVersion = true;
		
		List<String> rowValues = Collections.emptyList();
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			manager.createDownloadsListItemFromRowValues(rowValues, includeVersion);
		}).getMessage();
		
		assertEquals("rowValues is required and must not be empty.", message);
	}
	
	@Test
	public void testCreateDownloadsListItemFromRowValuesWithNullList() {
		boolean includeVersion = true;
		
		List<String> rowValues = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			manager.createDownloadsListItemFromRowValues(rowValues, includeVersion);
		}).getMessage();
		
		assertEquals("rowValues is required and must not be empty.", message);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithVersionTrue() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111", "1")),
				new Row().setValues(List.of("222", "2"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
	}

	/**
	 * A query that matches no rows hands on no batch, so nothing is looked up or added.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithEmptyResult() throws Exception {
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(Collections.emptyList());

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(0L), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verifyNoInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithUserVersionFalse() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111")),
				new Row().setValues(List.of("222"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupGetFilesWithSizeAsAllFiles();
		
		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\" FROM syn123 ORDER BY \"ROW_ID\"")), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(null),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(null)));
	}
	
	/**
	 * The query targets a Dataset as the table, so every row is a file. Not to be confused with
	 * {@link #testAddQueryResultsToDownloadListWithDatasetRows()}, where the rows themselves are Datasets.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithDataset() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111", "1")),
				new Row().setValues(List.of("222", "2"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.dataset);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithSelectFileColumn() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111")),
				new Row().setValues(List.of("222"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.ENTITYID)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123").setSelectFileColumn(123L);
		boolean userVersion = false;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"foo\" FROM syn123 ORDER BY \"foo\"").setSelectFileColumn(123L)), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111"),
						new DownloadListItem().setFileEntityId("222")));
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithSelectFileColumnAndUSeVersion() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111", "1")),
				new Row().setValues(List.of("222", "2"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.ENTITYID)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123").setSelectFileColumn(123L);
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"foo\", \"ROW_VERSION\" FROM syn123 ORDER BY \"foo\", \"ROW_VERSION\"").setSelectFileColumn(123L)), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithSelectFileColumnAndFileVersionColumn() throws Exception {
		long filesAdded = 2L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setRowId(1111L).setValues(List.of("111", "1")),
				new Row().setRowId(2222L).setValues(List.of("222", "2"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(
			createColumn(123L, "foo", ColumnType.ENTITYID), 
			createColumn(456L, "bar", ColumnType.INTEGER))
		);
		
		setupRunQueryAsStream(rows);
		
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123")
			.setSelectFileColumn(123L)
			.setSelectFileVersionColumn(456L);
		
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"foo\", \"bar\" FROM syn123 ORDER BY \"foo\", \"bar\"")
				.setSelectFileColumn(123L)
				.setSelectFileVersionColumn(456L)), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
						new DownloadListItem().setFileEntityId("222").setVersionNumber(2L)));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithFilesExceedingCapacity() throws Exception {
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111")),
				new Row().setValues(List.of("222")),
				new Row().setValues(List.of("333"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(3L);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long usersDownloadListCapacity = 2L;
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getMessage();
		
		assertEquals("Adding the files from the given query to your download list would exceed the maximum number of '100000' files."
				+ "  You currently have '99998' files on you download list.", result);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\" FROM syn123 ORDER BY \"ROW_ID\"")), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), List.of(
			new DownloadListItem().setFileEntityId("111").setVersionNumber(null),
			new DownloadListItem().setFileEntityId("222").setVersionNumber(null),
			new DownloadListItem().setFileEntityId("333").setVersionNumber(null)
		));
	}

	/**
	 * The stream is handed on in batches of {@link DownloadListManagerImpl#MAX_QUERY_PAGE_SIZE}, with
	 * the last partial batch flushed once the stream ends.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithMultipleBatches() throws Exception {
		int batchSize = (int) DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE;
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rowRange(0, batchSize + 1));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn((long) batchSize, 1L);
		setupGetFilesWithSizeAsAllFiles();

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = DownloadListManagerImpl.MAX_FILES_PER_USER;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(batchSize + 1L), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), itemRange(0, batchSize));
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), itemRange(batchSize, batchSize + 1));
	}

	@Test
	public void testAddQueryResultsToDownloadListWithBadQuery() throws Exception {
		Query query = new Query().setSql("this is not sql");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getMessage();
		assertTrue(message.contains("<regular_identifier> \"this \"\" at line 1, column 1."));

		verifyNoMoreInteractions(mockTableQueryManager, mockDownloadListDao);
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithJoiny() throws Exception {
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		Query query = new Query().setSql("select * from syn123 join syn456");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getMessage();
		assertEquals(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEX_MESSAGE, message);

		verifyNoMoreInteractions(mockTableQueryManager, mockDownloadListDao);
	}
	
	@Test
	public void testAddQueryResultsToDownloadListWithNullQuery() throws Exception {
		Query query = new Query().setSql(null);
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getMessage();
		assertEquals("query.sql is required.", message);

		verifyNoMoreInteractions(mockTableQueryManager, mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithTableUnavailable() throws Exception {

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any()))
				.thenThrow(new TableUnavailableException(new TableStatus().setState(TableState.PROCESSING_FAILED)));

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		});

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithLockUnavailableException() throws Exception {

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any()))
				.thenThrow(new LockUnavilableException(LockType.Read, "key", "context"));

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		});

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithTableFailedException() throws Exception {
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		TableFailedException exception = new TableFailedException(
				new TableStatus().setState(TableState.PROCESSING_FAILED));
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any())).thenThrow(exception);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		Throwable cause = assertThrows(RuntimeException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getCause();
		
		assertEquals(exception, cause);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verifyNoMoreInteractions(mockDownloadListDao);
	}

	@Test
	public void testAddQueryResultsToDownloadListWithDatastoreException() throws Exception {
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		DatastoreException exception = new DatastoreException("wrong");
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any())).thenThrow(exception);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		Throwable cause = assertThrows(RuntimeException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getCause();
		assertEquals(exception, cause);

		verify(mockTableManagerSupport).getTableType(IdAndVersion.parse("syn123"));
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verifyNoMoreInteractions(mockDownloadListDao);
	}
	
	/**
	 * A row of an unsupported type, such as a folder, is dropped.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithNonFileEntities() throws Exception {
		long filesAdded = 1L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111", "1")),
				new Row().setValues(List.of("222", "2"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.ENTITYID)));
		setupRunQueryAsStream(rows);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);
		setupItemTypes(Map.of(EntityType.file,
				List.of(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L))));

		Query query = new Query().setSql("select * from syn123").setSelectFileColumn(123L);
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"foo\", \"ROW_VERSION\" FROM syn123 ORDER BY \"foo\", \"ROW_VERSION\"").setSelectFileColumn(123L)), any());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
			Arrays.asList(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L))
		);
	}

	/**
	 * The files of a Dataset row are added together with the file rows. The expansion is limited to the
	 * remaining capacity plus one (100 - 0 + 1).
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithDatasetRows() throws Exception {
		long filesAdded = 3L;
		// @formatter:off
		List<Row> rows = Arrays.asList(
				new Row().setValues(List.of("111", "1")),
				new Row().setValues(List.of("222", "2"))
		);
		// @formatter:on
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rows);

		DownloadListItem fileItem = new DownloadListItem().setFileEntityId("111").setVersionNumber(1L);
		DownloadListItem datasetItem = new DownloadListItem().setFileEntityId("222").setVersionNumber(2L);
		setupItemTypes(Map.of(EntityType.file, List.of(fileItem), EntityType.dataset, List.of(datasetItem)));

		List<FileAndSize> datasetFiles = List.of(member("syn500", 1L, 5L), member("syn501", 3L, 6L));
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(datasetFiles);
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(filesAdded);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(filesAdded), result);

		verify(mockDownloadListDao).getFilesInDatasets(List.of(new EntityRef().setEntityId("222").setVersionNumber(2L)), 101L);
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(),
				List.of(fileItem, datasetFiles.get(0).item(), datasetFiles.get(1).item()));
	}

	/**
	 * The first batch exactly fills the capacity, so the second batch's Dataset is expanded with a limit
	 * of one (10000 - 10000 + 1), and the second batch overfills the capacity.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithDatasetRowInLaterBatchExceedingCapacity() throws Exception {
		int batchSize = (int) DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE;
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rowRange(0, batchSize + 2));

		List<DownloadListItem> files = itemRange(0, batchSize + 1);
		DownloadListItem dataset = itemRange(batchSize + 1, batchSize + 2).get(0);
		setupItemTypes(Map.of(EntityType.file, files, EntityType.dataset, List.of(dataset)));
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(memberRange(50_000, 50_001, 1L));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn((long) batchSize, 2L);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = batchSize;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query, userVersion, usersDownloadListCapacity);
		}).getMessage();

		assertEquals("Adding the files from the given query to your download list would exceed the maximum number of '100000' files."
				+ "  You currently have '90000' files on you download list.", message);

		verify(mockDownloadListDao).getFilesInDatasets(
				List.of(new EntityRef().setEntityId(dataset.getFileEntityId()).setVersionNumber(1L)), 1L);
		verify(mockDownloadListDao, times(2)).addBatchOfFilesToDownloadList(anyLong(), any());
	}

	/**
	 * The remaining capacity carries the running total across batches: 20000 - 0 + 1 = 20001 for the
	 * first batch's Dataset, and 20000 - 10002 + 1 = 9999 for the second batch's.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithDatasetRowsInMultipleBatches() throws Exception {
		int batchSize = (int) DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE;
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rowRange(0, batchSize + 1));

		// The last row of each batch is a Dataset.
		List<DownloadListItem> files = itemRange(0, batchSize - 1);
		DownloadListItem batchOneDataset = itemRange(batchSize - 1, batchSize).get(0);
		DownloadListItem batchTwoDataset = itemRange(batchSize, batchSize + 1).get(0);
		setupItemTypes(Map.of(EntityType.file, files, EntityType.dataset, List.of(batchOneDataset, batchTwoDataset)));
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(memberRange(50_000, 50_003, 1L),
				memberRange(60_000, 60_002, 1L));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(batchSize - 1L + 3L, 2L);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = true;
		long usersDownloadListCapacity = 20000L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(batchSize - 1L + 3L + 2L), result);

		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockDownloadListDao).getFilesInDatasets(
				List.of(new EntityRef().setEntityId(batchOneDataset.getFileEntityId()).setVersionNumber(1L)), 20001L);
		verify(mockDownloadListDao).getFilesInDatasets(
				List.of(new EntityRef().setEntityId(batchTwoDataset.getFileEntityId()).setVersionNumber(1L)), 9999L);
	}

	/**
	 * Without useVersion, a file row is stored against its current version and a Dataset member against
	 * the version recorded in the Dataset. When both refer to the same version, the file is added once, as
	 * the row, which comes first in the batch.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithFileRowAndDatasetMemberAtSameVersion() throws Exception {
		List<Row> rows = Arrays.asList(new Row().setValues(List.of("111")), new Row().setValues(List.of("222")));
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rows);

		DownloadListItem fileItem = new DownloadListItem().setFileEntityId("111").setVersionNumber(null);
		DownloadListItem datasetItem = new DownloadListItem().setFileEntityId("222").setVersionNumber(null);
		setupItemTypes(Map.of(EntityType.file, List.of(fileItem), EntityType.dataset, List.of(datasetItem)));
		when(mockNodeDao.getCurrentRevisionNumbers(List.of("222")))
				.thenReturn(List.of(new Reference().setTargetId("222").setTargetVersionNumber(9L)));
		// The Dataset holds the row's file at the file's current version, and one other file.
		FileAndSize sameFile = member("syn111", FILE_ROW_CURRENT_VERSION, FILE_ROW_SIZE);
		FileAndSize otherFile = member("syn500", 3L, 5L);
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(List.of(sameFile, otherFile));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn(2L);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long usersDownloadListCapacity = 100L;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded(2L), result);

		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), List.of(fileItem, otherFile.item()));
	}

	/**
	 * The versions already added are remembered across batches: a Dataset member added by the first batch
	 * is not added again as a file row at the same version in the second batch.
	 */
	@Test
	public void testAddQueryResultsToDownloadListWithDatasetMemberAndFileRowAtSameVersionInLaterBatch() throws Exception {
		int batchSize = (int) DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE;
		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(rowRange(0, batchSize + 1));

		// Without useVersion only the id of each row is read, so the items have no version.
		List<DownloadListItem> items = LongStream.range(0, batchSize + 1)
				.mapToObj(id -> new DownloadListItem().setFileEntityId(String.valueOf(id)).setVersionNumber(null))
				.collect(Collectors.toList());
		// The first row is a Dataset, and every other row, including the second batch's, is a file.
		DownloadListItem dataset = items.get(0);
		setupItemTypes(Map.of(EntityType.dataset, List.of(dataset), EntityType.file, items.subList(1, batchSize + 1)));
		when(mockNodeDao.getCurrentRevisionNumbers(List.of(dataset.getFileEntityId())))
				.thenReturn(List.of(new Reference().setTargetId(dataset.getFileEntityId()).setTargetVersionNumber(4L)));
		// The Dataset holds the second batch's file at that file's current version.
		FileAndSize sameFile = member("syn" + batchSize, FILE_ROW_CURRENT_VERSION, FILE_ROW_SIZE);
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(List.of(sameFile));
		when(mockDownloadListDao.addBatchOfFilesToDownloadList(anyLong(), any())).thenReturn((long) batchSize, 0L);

		Query query = new Query().setSql("select * from syn123");
		boolean userVersion = false;
		long usersDownloadListCapacity = DownloadListManagerImpl.MAX_FILES_PER_USER;
		// call under test
		AddToDownloadListResponse result = manager.addQueryResultsToDownloadList(mockProgressCallback, userOne, query,
				userVersion, usersDownloadListCapacity);
		assertEquals(new AddToDownloadListResponse().setNumberOfFilesAdded((long) batchSize), result);

		List<DownloadListItem> batchOneFiles = new ArrayList<>(items.subList(1, batchSize));
		batchOneFiles.add(sameFile.item());
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), batchOneFiles);
		verify(mockDownloadListDao).addBatchOfFilesToDownloadList(userOne.getId(), Collections.emptyList());
	}

	@Test
	public void testCheckQueryCapacityWithTotalUnderCapacity() {
		// call under test
		DownloadListManagerImpl.checkQueryCapacity(1L, 2L);
	}

	@Test
	public void testCheckQueryCapacityWithTotalEqualToCapacity() {
		// call under test
		DownloadListManagerImpl.checkQueryCapacity(2L, 2L);
	}

	@Test
	public void testCheckQueryCapacityWithTotalOverCapacity() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			DownloadListManagerImpl.checkQueryCapacity(3L, 2L);
		}).getMessage();

		assertEquals("Adding the files from the given query to your download list would exceed the maximum number of '100000' files."
				+ "  You currently have '99998' files on you download list.", message);
	}

	@Test
	public void testToEntityRefsWithEmptyItems() {
		boolean useVersion = false;

		// call under test
		List<EntityRef> result = manager.toEntityRefs(Collections.emptyList(), useVersion);

		assertEquals(Collections.emptyList(), result);
		verifyNoInteractions(mockNodeDao);
	}

	@Test
	public void testToEntityRefsWithUseVersion() {
		List<DownloadListItem> items = List.of(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
				new DownloadListItem().setFileEntityId("222").setVersionNumber(3L));
		boolean useVersion = true;

		// call under test
		List<EntityRef> result = manager.toEntityRefs(items, useVersion);

		assertEquals(List.of(new EntityRef().setEntityId("111").setVersionNumber(1L),
				new EntityRef().setEntityId("222").setVersionNumber(3L)), result);
		verifyNoInteractions(mockNodeDao);
	}

	/**
	 * An entity with no resolvable current version is dropped.
	 */
	@Test
	public void testToEntityRefsWithUseVersionFalse() {
		List<DownloadListItem> items = List.of(new DownloadListItem().setFileEntityId("111"),
				new DownloadListItem().setFileEntityId("222"), new DownloadListItem().setFileEntityId("333"));
		boolean useVersion = false;
		when(mockNodeDao.getCurrentRevisionNumbers(List.of("111", "222", "333"))).thenReturn(List.of(
				new Reference().setTargetId("111").setTargetVersionNumber(4L),
				new Reference().setTargetId("222").setTargetVersionNumber(null),
				new Reference().setTargetId("333").setTargetVersionNumber(7L)));

		// call under test
		List<EntityRef> result = manager.toEntityRefs(items, useVersion);

		assertEquals(List.of(new EntityRef().setEntityId("111").setVersionNumber(4L),
				new EntityRef().setEntityId("333").setVersionNumber(7L)), result);
	}

	/**
	 * A file stored against its current version has the ref of that version, so it matches the same file
	 * stored against that specific version.
	 */
	@Test
	public void testToFileRefWithNullVersion() {
		FileAndSize file = new FileAndSize(new DownloadListItem().setFileEntityId("111").setVersionNumber(null), 3L, 0L);

		// call under test
		assertEquals(idAndVersion(111L, 3L), DownloadListManagerImpl.toFileRef(file));
	}

	/**
	 * The file rows come first, as given, followed by the files of the Dataset rows. A folder row is
	 * dropped.
	 */
	@Test
	public void testGetFilesInBatchWithFileAndDatasetRows() {
		DownloadListItem fileItem = new DownloadListItem().setFileEntityId("111").setVersionNumber(1L);
		DownloadListItem datasetItem = new DownloadListItem().setFileEntityId("222").setVersionNumber(2L);
		DownloadListItem folderItem = new DownloadListItem().setFileEntityId("333").setVersionNumber(1L);
		List<DownloadListItem> batch = List.of(datasetItem, folderItem, fileItem);
		setupItemTypes(Map.of(EntityType.file, List.of(fileItem), EntityType.dataset, List.of(datasetItem),
				EntityType.folder, List.of(folderItem)));

		List<FileAndSize> datasetFiles = List.of(member("syn500", 1L, 5L), member("syn501", 3L, 6L));
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(datasetFiles);
		boolean useVersion = true;
		long maxDatasetFiles = 50L;

		// call under test
		List<FileAndSize> result = manager.getFilesInBatch(batch, useVersion, maxDatasetFiles);

		assertEquals(List.of(fileRow(fileItem), datasetFiles.get(0), datasetFiles.get(1)), result);
		verify(mockDownloadListDao).getFilesWithSize(batch, useVersion);
		verify(mockDownloadListDao).filterItemsByType(batch, List.of(EntityType.dataset));
		verify(mockDownloadListDao).getFilesInDatasets(List.of(new EntityRef().setEntityId("222").setVersionNumber(2L)),
				maxDatasetFiles);
		verifyNoInteractions(mockNodeDao);
	}

	/**
	 * Without Dataset rows, no Dataset files are looked up.
	 */
	@Test
	public void testGetFilesInBatchWithNoDatasetRows() {
		DownloadListItem fileItem = new DownloadListItem().setFileEntityId("111").setVersionNumber(1L);
		List<DownloadListItem> batch = List.of(fileItem);
		setupItemTypes(Map.of(EntityType.file, List.of(fileItem)));
		boolean useVersion = true;
		long maxDatasetFiles = 50L;

		// call under test
		List<FileAndSize> result = manager.getFilesInBatch(batch, useVersion, maxDatasetFiles);

		assertEquals(List.of(fileRow(fileItem)), result);
		verify(mockDownloadListDao, never()).getFilesInDatasets(any(), anyLong());
	}

	/**
	 * Without useVersion the file rows are sized at their current version and each Dataset is expanded
	 * at its current version.
	 */
	@Test
	public void testGetFilesInBatchWithUseVersionFalse() {
		DownloadListItem fileItem = new DownloadListItem().setFileEntityId("111").setVersionNumber(null);
		DownloadListItem datasetItem = new DownloadListItem().setFileEntityId("222").setVersionNumber(null);
		List<DownloadListItem> batch = List.of(fileItem, datasetItem);
		setupItemTypes(Map.of(EntityType.file, List.of(fileItem), EntityType.dataset, List.of(datasetItem)));
		when(mockNodeDao.getCurrentRevisionNumbers(List.of("222")))
				.thenReturn(List.of(new Reference().setTargetId("222").setTargetVersionNumber(9L)));

		List<FileAndSize> datasetFiles = List.of(member("syn500", 4L, 5L));
		when(mockDownloadListDao.getFilesInDatasets(any(), anyLong())).thenReturn(datasetFiles);
		boolean useVersion = false;
		long maxDatasetFiles = 50L;

		// call under test
		List<FileAndSize> result = manager.getFilesInBatch(batch, useVersion, maxDatasetFiles);

		assertEquals(List.of(fileRow(fileItem), datasetFiles.get(0)), result);
		verify(mockDownloadListDao).getFilesWithSize(batch, useVersion);
		verify(mockDownloadListDao).getFilesInDatasets(List.of(new EntityRef().setEntityId("222").setVersionNumber(9L)),
				maxDatasetFiles);
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
	public void testPackageFilesWithFileNameAtLimit() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest()
				.setZipFileName("a".repeat(NameValidation.MAX_NAME_CHARS));
		
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean()))
				.thenThrow(new IllegalArgumentException(NameValidation.NAME_LENGTH_TOO_LONG));

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			managerSpy.packageFiles(mockProgressCallback, userOne, request);
		}).getMessage();
		
		assertEquals(NameValidation.NAME_LENGTH_TOO_LONG, errorMessage);
		
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult)));
		
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		verify(mockDownloadListDao, never()).removeBatchOfFilesFromDownloadList(any(), any());
		verify(managerSpy, never()).buildManifest(any(), any(), any());
	}
	
	@Test
	public void testPackageFilesWithFileNameOverLimit() throws IOException {
		DownloadListManagerImpl managerSpy = Mockito.spy(manager);
		DownloadListPackageRequest request = new DownloadListPackageRequest()
				.setZipFileName("a".repeat(NameValidation.MAX_NAME_CHARS + 1));
		
		when(mockDownloadListDao.getFilesAvailableToDownloadFromDownloadList(any(), any(), any(), any(), any(), any()))
				.thenReturn(downloadListItems);
		when(mockFileHandlePackageManager.buildZip(any(), any(), anyBoolean()))
				.thenThrow(new IllegalArgumentException(NameValidation.NAME_LENGTH_TOO_LONG));

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			managerSpy.packageFiles(mockProgressCallback, userOne, request);
		}).getMessage();
		
		assertEquals(NameValidation.NAME_LENGTH_TOO_LONG, errorMessage);
		
		BulkFileDownloadRequest expectedBulkFileDownloadRequest = new BulkFileDownloadRequest()
				.setZipFileFormat(ZipFileFormat.Flat).setZipFileName(request.getZipFileName()).setRequestedFiles(
						Arrays.asList(DownloadListManagerImpl.createAssociationForItem(downloadListItemResult)));
		
		verify(managerSpy).createAccessCallback(userOne);
		verify(mockDownloadListDao).getFilesAvailableToDownloadFromDownloadList(any(), eq(userOne.getId()),
				eq(AvailableFilter.eligibleForPackaging),
				eq(Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC))),
				eq(DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE), eq(0L));
		verify(mockFileHandlePackageManager).buildZip(userOne, expectedBulkFileDownloadRequest, fileSizesChecked);
		verify(mockDownloadListDao, never()).removeBatchOfFilesFromDownloadList(any(), any());
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

		verify(mockCSVWriter, times(3)).writeNext(any(String[].class));
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
		verify(mockCSVWriter, times(2)).writeNext(any(String[].class));
		verify(mockCSVWriter).writeNext(new String[] { "1-0", "1-1", "1-2", "1-3", "1-4", "1-5", "1-6", "1-7", "1-8",
				"1-9", "1-10", "1-11", "one", null, "two" });
		verify(mockCSVWriter).writeNext(new String[] { "2-0", "2-1", "2-2", "2-3", "2-4", "2-5", "2-6", "2-7", "2-8",
				"2-9", "2-10", "2-11", null, "four", "three" });
	}
	
	/**
	 * The count and size are those of the files streamed from the rewritten query, from offset zero.
	 * The index is not asked for a size.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQuery() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(List.of(
				new Row().setValues(List.of("111", "1")), new Row().setValues(List.of("222", "2"))));
		setupItemTypes(Map.of(EntityType.file, List.of(
			new DownloadListItem().setFileEntityId("111").setVersionNumber(1L),
			new DownloadListItem().setFileEntityId("222").setVersionNumber(2L))));

		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(2L)
			.setFileSize(2 * FILE_ROW_SIZE)
			.setIsFileCountAndSizeEstimate(false);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);

		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\", \"ROW_VERSION\" FROM syn123 ORDER BY \"ROW_ID\", \"ROW_VERSION\"")), any());
		verify(mockTableQueryManager, never()).querySinglePage(any(), any(), any(), any());
	}

	/**
	 * The count and size include the files the Datasets expand to.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQueryAndDatasetRows() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));

		DownloadListItem fileItem = new DownloadListItem().setFileEntityId("111").setVersionNumber(1L);
		DownloadListItem datasetOne = new DownloadListItem().setFileEntityId("222").setVersionNumber(1L);
		DownloadListItem datasetTwo = new DownloadListItem().setFileEntityId("333").setVersionNumber(2L);

		setupRunQueryAsStream(List.of(
				new Row().setValues(List.of("111", "1")), new Row().setValues(List.of("222", "1")),
				new Row().setValues(List.of("333", "2"))));
		setupItemTypes(Map.of(EntityType.file, List.of(fileItem), EntityType.dataset, List.of(datasetOne, datasetTwo)));

		List<EntityRef> expectedRefs = List.of(
				new EntityRef().setEntityId("222").setVersionNumber(1L),
				new EntityRef().setEntityId("333").setVersionNumber(2L));
		when(mockDownloadListDao.getFilesInDatasets(expectedRefs, DownloadListManagerImpl.MAX_FILES_PER_USER + 1))
			.thenReturn(memberRange(500, 505, 1L));

		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(6L)
			.setFileSize(FILE_ROW_SIZE + 5L)
			.setIsFileCountAndSizeEstimate(false);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);
		verify(mockDownloadListDao, times(1)).getFilesInDatasets(any(), anyLong());
	}

	/**
	 * Without versions, a file row refers to the file's current version, 1, so it is not collapsed with the
	 * same file as a Dataset member at version 9, and the rewrite selects only the id column.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQueryAndUseVersionFalse() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query).setUseVersionNumber(false)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(List.of(
				new Row().setValues(List.of("111")), new Row().setValues(List.of("222"))));
		setupItemTypes(Map.of(
			EntityType.file, List.of(new DownloadListItem().setFileEntityId("111").setVersionNumber(null)),
			EntityType.dataset, List.of(new DownloadListItem().setFileEntityId("222").setVersionNumber(null))));

		when(mockNodeDao.getCurrentRevisionNumbers(List.of("222")))
			.thenReturn(List.of(new Reference().setTargetId("222").setTargetVersionNumber(9L)));

		List<EntityRef> expectedRefs = List.of(new EntityRef().setEntityId("222").setVersionNumber(9L));
		when(mockDownloadListDao.getFilesInDatasets(expectedRefs, DownloadListManagerImpl.MAX_FILES_PER_USER + 1))
			.thenReturn(List.of(member("syn111", 9L, FILE_ROW_SIZE), member("syn600", 9L, 1L), member("syn601", 9L, 1L),
					member("syn602", 9L, 1L)));

		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(5L)
			.setFileSize(2 * FILE_ROW_SIZE + 3L)
			.setIsFileCountAndSizeEstimate(false);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);
		verify(mockNodeDao, times(1)).getCurrentRevisionNumbers(List.of("222"));
		verify(mockTableQueryManager).runQueryAsStream(eq(mockProgressCallback), eq(userOne), eq(new Query().setSql("SELECT \"ROW_ID\" FROM syn123 ORDER BY \"ROW_ID\"")), any());
	}

	/**
	 * Without versions, a file row and a Dataset member at the file's current version refer to the same
	 * version, so the file is counted and sized once, as the add stores it once.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQueryAndUseVersionFalseAndFileAlsoInDatasetAtCurrentVersion() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query).setUseVersionNumber(false)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(List.of(
				new Row().setValues(List.of("111")), new Row().setValues(List.of("222"))));
		setupItemTypes(Map.of(
			EntityType.file, List.of(new DownloadListItem().setFileEntityId("111").setVersionNumber(null)),
			EntityType.dataset, List.of(new DownloadListItem().setFileEntityId("222").setVersionNumber(null))));

		when(mockNodeDao.getCurrentRevisionNumbers(List.of("222")))
			.thenReturn(List.of(new Reference().setTargetId("222").setTargetVersionNumber(9L)));

		List<EntityRef> expectedRefs = List.of(new EntityRef().setEntityId("222").setVersionNumber(9L));
		when(mockDownloadListDao.getFilesInDatasets(expectedRefs, DownloadListManagerImpl.MAX_FILES_PER_USER + 1))
			.thenReturn(List.of(member("syn111", FILE_ROW_CURRENT_VERSION, FILE_ROW_SIZE), member("syn600", 9L, 1L)));

		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(2L)
			.setFileSize(FILE_ROW_SIZE + 1L)
			.setIsFileCountAndSizeEstimate(false);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);
	}

	/**
	 * A Dataset in the second batch of the stream is expanded, limited to one more than the maximum less
	 * the files already found.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQueryAndDatasetRowInLaterBatch() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));

		int batchSize = (int) DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE;
		setupRunQueryAsStream(rowRange(0, batchSize + 1));
		List<DownloadListItem> batchOneFiles = itemRange(0, batchSize);
		DownloadListItem batchTwoDataset = itemRange(batchSize, batchSize + 1).get(0);
		setupItemTypes(Map.of(EntityType.file, batchOneFiles), Map.of(EntityType.dataset, List.of(batchTwoDataset)));

		List<EntityRef> expectedRefs = List.of(
				new EntityRef().setEntityId(batchTwoDataset.getFileEntityId()).setVersionNumber(1L));
		long expectedLimit = DownloadListManagerImpl.MAX_FILES_PER_USER + 1 - batchSize;
		when(mockDownloadListDao.getFilesInDatasets(expectedRefs, expectedLimit)).thenReturn(memberRange(50000, 50004, 1L));

		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(batchSize + 4L)
			.setFileSize(batchSize * FILE_ROW_SIZE + 4L)
			.setIsFileCountAndSizeEstimate(false);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);
		verify(mockTableQueryManager, times(1)).runQueryAsStream(any(), any(), any(), any());
		verify(mockDownloadListDao).getFilesInDatasets(expectedRefs, expectedLimit);
	}

	/**
	 * More files than any download list can hold makes the response an estimate.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQueryAndFilesOverMax() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		setupRunQueryAsStream(List.of(
				new Row().setValues(List.of("111", "1")), new Row().setValues(List.of("222", "1"))));
		setupItemTypes(Map.of(
			EntityType.file, List.of(new DownloadListItem().setFileEntityId("111").setVersionNumber(1L)),
			EntityType.dataset, List.of(new DownloadListItem().setFileEntityId("222").setVersionNumber(1L))));

		List<EntityRef> expectedRefs = List.of(new EntityRef().setEntityId("222").setVersionNumber(1L));
		List<FileAndSize> datasetFiles = memberRange(1000, 1000 + DownloadListManagerImpl.MAX_FILES_PER_USER + 1, 1L);
		when(mockDownloadListDao.getFilesInDatasets(expectedRefs, DownloadListManagerImpl.MAX_FILES_PER_USER + 1))
			.thenReturn(datasetFiles);

		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(datasetFiles.size() + 1L)
			.setFileSize(FILE_ROW_SIZE + datasetFiles.size())
			.setIsFileCountAndSizeEstimate(true);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);
	}

	/**
	 * Once more files than any download list can hold have been found, the later batches are not looked
	 * up.
	 */
	@Test
	public void testGetAddToDownloadListStatsWithQueryAndFilesOverMaxInEarlierBatch() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));

		int batchSize = (int) DownloadListManagerImpl.MAX_QUERY_PAGE_SIZE;
		setupRunQueryAsStream(rowRange(0, batchSize + 1));
		// The first row is a Dataset and every other row, including the second batch's, is a file.
		DownloadListItem dataset = itemRange(0, 1).get(0);
		setupItemTypes(Map.of(EntityType.dataset, List.of(dataset), EntityType.file, itemRange(1, batchSize + 1)));

		List<FileAndSize> datasetFiles = memberRange(1_000_000, 1_000_000 + DownloadListManagerImpl.MAX_FILES_PER_USER + 1, 1L);
		when(mockDownloadListDao.getFilesInDatasets(List.of(new EntityRef().setEntityId(dataset.getFileEntityId()).setVersionNumber(1L)),
				DownloadListManagerImpl.MAX_FILES_PER_USER + 1)).thenReturn(datasetFiles);

		long batchOneFileRows = batchSize - 1L;
		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(batchOneFileRows + datasetFiles.size())
			.setFileSize(batchOneFileRows * FILE_ROW_SIZE + datasetFiles.size())
			.setIsFileCountAndSizeEstimate(true);

		// Call under test
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);

		assertEquals(expectedResponse, result);
		verify(mockDownloadListDao, times(1)).getFilesWithSize(any(), anyBoolean());
		verify(mockDownloadListDao, times(1)).filterItemsByType(any(), any());
		verify(mockDownloadListDao, times(1)).getFilesInDatasets(any(), anyLong());
	}

	@Test
	public void testGetAddToDownloadListStatsWithQueryWithRecoverableEx() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any()))
			.thenThrow(new LockUnavilableException(LockType.Read, "key", "holder"))
			.thenThrow(new TableUnavailableException(new TableStatus()));

		// Call under test
		assertThrows(RecoverableMessageException.class, () -> {
			manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		});

		// Call under test
		assertThrows(RecoverableMessageException.class, () -> {
			manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		});
		verifyNoInteractions(mockDownloadListDao);
	}

	@Test
	public void testGetAddToDownloadListStatsWithQueryWithUnrecoverableEx() throws Exception {

		Query query = new Query().setSql("select * from syn123");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		when(mockTableManagerSupport.getTableType(any())).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(createColumn(123L, "foo", ColumnType.STRING)));
		when(mockTableQueryManager.runQueryAsStream(any(), any(), any(), any()))
			.thenThrow(new DatastoreException("nope"))
			.thenThrow(new TableFailedException(new TableStatus()));

		// Call under test
		assertThrows(RuntimeException.class, () -> {
			manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		});

		// Call under test
		assertThrows(RuntimeException.class, () -> {
			manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		});
		verifyNoInteractions(mockDownloadListDao);
	}

	@Test
	public void testGetAddToDownloadListStatsWithQueryParseExceptionEx() throws Exception {

		Query query = new Query().setSql("this is not sql");

		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setQuery(query)
		);

		// Call under test
		assertThrows(IllegalArgumentException.class, () -> {
			manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		});
		verifyNoInteractions(mockDownloadListDao, mockTableQueryManager);
	}

	@Test
	public void testGetAddToDownloadListStatsWithContainer() throws Exception {

		String parentId = "syn123";
		
		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setParentId(parentId)
		);
		
		when(mockEntityAuthorizationManager.hasAccess(userOne, parentId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.folder);
		
		// Call under test
		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(10L)
			.setFileSize(1300L)
			.setIsFileCountAndSizeEstimate(false);
		
		when(mockDownloadListDao.getAddChildrenToDownloadListStats(123L)).thenReturn(expectedResponse);
		
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		
		assertEquals(expectedResponse, result);
	}
	
	@Test
	public void testGetAddToDownloadListStatsWithContainerAndRecursive() throws Exception {

		String parentId = "syn123";
		
		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setParentId(parentId).setRecursive(true)
		);
		
		when(mockEntityAuthorizationManager.hasAccess(userOne, parentId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.folder);
		
		// Call under test
		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(10L)
			.setFileSize(1300L)
			.setIsFileCountAndSizeEstimate(false);
		
		when(mockDownloadListDao.getAddDescendantsToDownloadListStats(123L, 500)).thenReturn(expectedResponse);
		
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		
		assertEquals(expectedResponse, result);
	}
	
	@Test
	public void testGetAddToDownloadListStatsWithDataset() throws Exception {

		String parentId = "syn123";
		
		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setParentId(parentId)
		);
		
		when(mockEntityAuthorizationManager.hasAccess(userOne, parentId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.dataset);
		
		List<EntityRef> items = List.of(
			new EntityRef().setEntityId("123"),
			new EntityRef().setEntityId("234")
		);
		
		when(mockNodeDao.getNodeItems(123L)).thenReturn(items);
		
		// Call under test
		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(10L)
			.setFileSize(1300L)
			.setIsFileCountAndSizeEstimate(false);
		
		when(mockDownloadListDao.getAddFileEntityRefToDownloadListStats(items)).thenReturn(expectedResponse);
		
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		
		assertEquals(expectedResponse, result);
	}
	
	@Test
	public void testGetAddToDownloadListStatsWithDatasetCollection() throws Exception {

		String parentId = "syn123";
		
		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setParentId(parentId)
		);
		
		when(mockEntityAuthorizationManager.hasAccess(userOne, parentId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.datasetcollection);
		
		List<EntityRef> items = List.of(
			new EntityRef().setEntityId("123"),
			new EntityRef().setEntityId("234")
		);
		
		when(mockNodeDao.getNodeItems(123L)).thenReturn(items);
		
		// Call under test
		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(10L)
			.setFileSize(1300L)
			.setIsFileCountAndSizeEstimate(false);
		
		when(mockDownloadListDao.getAddDatasetEntityRefFilesToDownloadListStats(items)).thenReturn(expectedResponse);
		
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		
		assertEquals(expectedResponse, result);
	}
	
	@Test
	public void testGetAddToDownloadListStatsWithDatasetCollectionWithEstimate() throws Exception {

		String parentId = "syn123";
		
		AddToDownloadListStatsRequest request = new AddToDownloadListStatsRequest().setRequest(
			addRequest.setParentId(parentId)
		);
		
		when(mockEntityAuthorizationManager.hasAccess(userOne, parentId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockNodeDao.getNodeTypeById(parentId)).thenReturn(EntityType.datasetcollection);
		
		List<EntityRef> items = IntStream.range(0, 1000).boxed().map(i -> 
			new EntityRef().setEntityId("syn" + i)
		).collect(Collectors.toList());
		
		when(mockNodeDao.getNodeItems(123L)).thenReturn(items);
		
		// Call under test
		AddToDownloadListStatsResponse expectedResponse = new AddToDownloadListStatsResponse()
			.setFileCount(10L)
			.setFileSize(1300L)
			.setIsFileCountAndSizeEstimate(true);
		
		when(mockDownloadListDao.getAddDatasetEntityRefFilesToDownloadListStats(items.subList(0, 500))).thenReturn(expectedResponse);
		
		AddToDownloadListStatsResponse result = manager.getAddToDownloadListStats(mockProgressCallback, userOne, request);
		
		assertEquals(expectedResponse, result);
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
