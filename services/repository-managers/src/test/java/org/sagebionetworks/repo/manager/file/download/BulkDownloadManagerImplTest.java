package org.sagebionetworks.repo.manager.file.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.BulkDownloadDAO;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class BulkDownloadManagerImplTest {

	@Mock
	EntityManager mockEntityManager;

	@Mock
	NodeDAO mockNodeDao;

	@Mock
	BulkDownloadDAO mockBulkDownloadDao;

	@Mock
	TableQueryManager mockTableQueryManager;

	@InjectMocks
	BulkDownloadManagerImpl manager;

	@Captor
	ArgumentCaptor<EntityChildrenRequest> childRequestCaptor;
	@Captor
	ArgumentCaptor<List<FileHandleAssociation>> associationCaptor;
	@Captor
	ArgumentCaptor<List<String>> idsCaptor;
	@Captor
	ArgumentCaptor<QueryBundleRequest> queryBundleCaptor;

	UserInfo userInfo;
	String folderId;
	List<EntityHeader> headers;
	String nextPageToken;
	EntityChildrenResponse pageOne;
	EntityChildrenResponse pageTwo;

	String tableId;
	RowSet rowset;
	QueryResultBundle queryResult;
	Query query;

	@Before
	public void before() throws Exception {
		userInfo = new UserInfo(false, 123L);
		folderId = "syn123";
		headers = new LinkedList<>();
		for (int i = 0; i < 5; i++) {
			EntityHeader header = new EntityHeader();
			header.setId("" + i);
			header.setName("name" + i);
			headers.add(header);
			header.setVersionNumber(i * 3L);
		}
		nextPageToken = "hasNextPageToken";
		pageOne = new EntityChildrenResponse();
		pageOne.setNextPageToken(nextPageToken);
		pageOne.setPage(headers.subList(0, 2));

		pageTwo = new EntityChildrenResponse();
		pageTwo.setNextPageToken(null);
		pageTwo.setPage(headers.subList(2, 4));

		pageTwo = new EntityChildrenResponse();
		pageTwo.setNextPageToken(null);
		pageTwo.setPage(headers.subList(2, 4));

		when(mockEntityManager.getChildren(any(UserInfo.class), any(EntityChildrenRequest.class))).thenReturn(pageOne,
				pageTwo);
		DownloadList addedFiles = new DownloadList();
		addedFiles.setFilesToDownload(new LinkedList<>());
		when(mockBulkDownloadDao.addFilesToDownloadList(any(String.class), anyListOf(FileHandleAssociation.class)))
				.thenReturn(addedFiles);
		when(mockBulkDownloadDao.clearDownloadList(any(String.class))).thenReturn(addedFiles);

		when(mockBulkDownloadDao.getUsersDownloadList(any(String.class))).thenReturn(addedFiles);
		List<FileHandleAssociation> associations = createResultsOfSize(4);
		when(mockNodeDao.getFileHandleAssociationsForCurrentVersion(anyListOf(String.class)))
				.thenReturn(associations.subList(0, 2), associations.subList(2, 4));

		tableId = "syn123";
		rowset = new RowSet();
		rowset.setRows(createRows(2));
		rowset.setTableId(tableId);
		QueryResult qr = new QueryResult();
		qr.setQueryResults(rowset);
		queryResult = new QueryResultBundle();
		queryResult.setQueryResult(qr);
		when(mockTableQueryManager.queryBundle(any(ProgressCallback.class), any(UserInfo.class),
				any(QueryBundleRequest.class))).thenReturn(queryResult);

		query = new Query();
		query.setSql("select * from "+tableId);
		
		when(mockEntityManager.getEntityType(userInfo, tableId)).thenReturn(EntityType.entityview);

	}

	@Test
	public void testAddFilesFromFolder() {
		// call under test
		DownloadList list = manager.addFilesFromFolder(userInfo, folderId);
		assertNotNull(list);
		// two pages
		verify(mockEntityManager, times(2)).getChildren(any(UserInfo.class), childRequestCaptor.capture());
		verify(mockNodeDao, times(2)).getFileHandleAssociationsForCurrentVersion(idsCaptor.capture());
		verify(mockBulkDownloadDao, times(2)).addFilesToDownloadList(any(String.class), associationCaptor.capture());

		List<EntityChildrenRequest> childRequests = childRequestCaptor.getAllValues();
		assertNotNull(childRequests);
		assertEquals(2, childRequests.size());
		// first request
		EntityChildrenRequest request = childRequests.get(0);
		assertEquals(folderId, request.getParentId());
		assertEquals(Lists.newArrayList(EntityType.file), request.getIncludeTypes());
		// first request should not have a token
		assertEquals(null, request.getNextPageToken());
		// second request
		request = childRequests.get(1);
		// the second request should include a token
		assertEquals(nextPageToken, request.getNextPageToken());

		List<List<FileHandleAssociation>> capturedAssociations = associationCaptor.getAllValues();
		assertNotNull(capturedAssociations);
		// to calls should be captured.
		assertEquals(2, capturedAssociations.size());
		// first call
		List<FileHandleAssociation> added = capturedAssociations.get(0);
		assertNotNull(added);
		assertEquals(2, added.size());
		FileHandleAssociation association = added.get(0);
		assertEquals("0", association.getAssociateObjectId());
		assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
		assertEquals("000", association.getFileHandleId());

		// second call
		added = capturedAssociations.get(1);
		assertNotNull(added);
		assertEquals(2, added.size());
		association = added.get(1);
		assertEquals("3", association.getAssociateObjectId());
		assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
		assertEquals("333", association.getFileHandleId());

		List<List<String>> capturedId = idsCaptor.getAllValues();
		assertNotNull(capturedId);
		assertEquals(2, capturedId.size());
		// first call
		assertEquals(Lists.newArrayList("0", "1"), capturedId.get(0));
		// second call
		assertEquals(Lists.newArrayList("2", "3"), capturedId.get(1));

	}

	@Test
	public void testAddFilesFromFolderNoChildren() {
		// setup no children.
		EntityChildrenResponse noResutls = new EntityChildrenResponse();
		noResutls.setNextPageToken(null);
		noResutls.setPage(new LinkedList<>());
		when(mockEntityManager.getChildren(any(UserInfo.class), any(EntityChildrenRequest.class)))
				.thenReturn(noResutls);
		when(mockNodeDao.getFileHandleAssociationsForCurrentVersion(anyListOf(String.class)))
				.thenReturn(new LinkedList<>());
		// call under test
		DownloadList list = manager.addFilesFromFolder(userInfo, folderId);
		assertNotNull(list);
		verify(mockEntityManager).getChildren(any(UserInfo.class), any(EntityChildrenRequest.class));
		verify(mockNodeDao).getFileHandleAssociationsForCurrentVersion(anyListOf(String.class));
		verify(mockBulkDownloadDao).addFilesToDownloadList(any(String.class),
				anyListOf(FileHandleAssociation.class));
	}

	@Test
	public void testAddFilesFromFolderOverLimit() {
		// setup over limit
		DownloadList usersList = new DownloadList();
		usersList.setFilesToDownload(createResultsOfSize(BulkDownloadManagerImpl.MAX_FILES_PER_DOWNLOAD_LIST + 1));
		when(mockBulkDownloadDao.addFilesToDownloadList(any(String.class), anyListOf(FileHandleAssociation.class)))
				.thenReturn(usersList);
		try {
			// call under test
			manager.addFilesFromFolder(userInfo, folderId);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(BulkDownloadManagerImpl.EXCEEDED_MAX_NUMBER_ROWS, e.getMessage());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesFromFolderNullUser() {
		userInfo = null;
		// call under test
		manager.addFilesFromFolder(userInfo, folderId);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesFromFolderNullFolder() {
		folderId = null;
		// call under test
		manager.addFilesFromFolder(userInfo, folderId);
	}

	@Test
	public void testAttemptToAddFilesToUsersDownloadList() {
		List<FileHandleAssociation> toAdd = createResultsOfSize(2);
		// call under test
		manager.attemptToAddFilesToUsersDownloadList(userInfo, toAdd);
		verify(mockBulkDownloadDao).addFilesToDownloadList("" + userInfo.getId(), toAdd);
	}

	@Test
	public void testAttemptToAddFilesToUsersDownloadListEmpty() {
		List<FileHandleAssociation> toAdd = new LinkedList<>();
		// call under test
		manager.attemptToAddFilesToUsersDownloadList(userInfo, toAdd);
		verify(mockBulkDownloadDao).addFilesToDownloadList(any(String.class),
				anyListOf(FileHandleAssociation.class));
	}

	@Test
	public void testAddFilesFromQuery() throws Exception {
		// call under test
		DownloadList result = manager.addFilesFromQuery(userInfo, query);
		assertNotNull(result);
		verify(mockTableQueryManager).queryBundle(any(ProgressCallback.class), any(UserInfo.class),
				queryBundleCaptor.capture());
		verify(mockNodeDao).getFileHandleAssociationsForCurrentVersion(idsCaptor.capture());
		verify(mockBulkDownloadDao).addFilesToDownloadList(any(String.class), associationCaptor.capture());

		// validate query request
		QueryBundleRequest queryRequest = queryBundleCaptor.getValue();
		assertNotNull(queryRequest);
		assertEquals(new Long(BulkDownloadManagerImpl.QUERY_ONLY_PART_MASK), queryRequest.getPartMask());
		assertNotNull(queryRequest.getQuery());
		assertEquals(new Long(BulkDownloadManagerImpl.MAX_FILES_PER_DOWNLOAD_LIST + 1),
				queryRequest.getQuery().getLimit());
		assertEquals(query, queryRequest.getQuery());
		// validate get file handles
		List<String> entityIds = idsCaptor.getValue();
		assertEquals(Lists.newArrayList("0", "1"), entityIds);
		// validate add
		List<FileHandleAssociation> associations = associationCaptor.getValue();
		assertNotNull(associations);
		assertEquals(2, associations.size());
		FileHandleAssociation association = associations.get(0);
		assertEquals("0", association.getAssociateObjectId());
		assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
		assertEquals("000", association.getFileHandleId());
	}
	
	@Test
	public void testAddFilesFromQueryNotAview() throws Exception {
		// case where the table is not a view
		when(mockEntityManager.getEntityType(userInfo, tableId)).thenReturn(EntityType.table);
		try {
			// call under test
			manager.addFilesFromQuery(userInfo, query);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(BulkDownloadManagerImpl.FILES_CAN_ONLY_BE_ADDED_FROM_A_FILE_VIEW_QUERY, e.getMessage());
		}
		verify(mockTableQueryManager).queryBundle(any(ProgressCallback.class), any(UserInfo.class), any(QueryBundleRequest.class));
		verify(mockNodeDao, never()).getFileHandleAssociationsForCurrentVersion(anyListOf(String.class));
		verify(mockBulkDownloadDao, never()).addFilesToDownloadList(any(String.class), anyListOf(FileHandleAssociation.class));
	}

	@Test
	public void testAddFilesFromQueryTooManyRows() throws Exception {
		// setup query result with more than the max number of rows.
		rowset.setRows(createRows(BulkDownloadManagerImpl.MAX_FILES_PER_DOWNLOAD_LIST + 1));
		try {
			// call under test
			manager.addFilesFromQuery(userInfo, query);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(BulkDownloadManagerImpl.EXCEEDED_MAX_NUMBER_ROWS, e.getMessage());
		}
	}

	@Test(expected = RecoverableMessageException.class)
	public void testAddFilesFromQueryLockUnavilableException() throws Exception {
		LockUnavilableException exception = new LockUnavilableException();
		when(mockTableQueryManager.queryBundle(any(ProgressCallback.class), any(UserInfo.class),
				any(QueryBundleRequest.class))).thenThrow(exception);
		// call under test
		manager.addFilesFromQuery(userInfo, query);
	}

	@Test(expected = RecoverableMessageException.class)
	public void testAddFilesFromQueryTableUnavailableException() throws Exception {
		TableUnavailableException exception = new TableUnavailableException(null);
		when(mockTableQueryManager.queryBundle(any(ProgressCallback.class), any(UserInfo.class),
				any(QueryBundleRequest.class))).thenThrow(exception);
		// call under test
		manager.addFilesFromQuery(userInfo, query);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesFromQueryParseException() throws Exception {
		ParseException exception = new ParseException();
		when(mockTableQueryManager.queryBundle(any(ProgressCallback.class), any(UserInfo.class),
				any(QueryBundleRequest.class))).thenThrow(exception);
		// call under test
		manager.addFilesFromQuery(userInfo, query);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesFromQueryNullUser() throws Exception {
		userInfo = null;
		// call under test
		manager.addFilesFromQuery(userInfo, query);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesFromQueryNullQuery() throws Exception {
		query = null;
		// call under test
		manager.addFilesFromQuery(userInfo, query);
	}
	
	@Test
	public void testAddFileHandleAssociations() {
		List<FileHandleAssociation> toAdd = createResultsOfSize(10);
		// call under test
		manager.addFileHandleAssociations(userInfo, toAdd);
		verify(mockBulkDownloadDao).addFilesToDownloadList(userInfo.getId().toString(), toAdd);
	}
	
	@Test
	public void testAddFileHandleAssociationsEmpty() {
		List<FileHandleAssociation> toAdd = new LinkedList<>();
		// call under test
		manager.addFileHandleAssociations(userInfo, toAdd);
		verify(mockBulkDownloadDao).addFilesToDownloadList(userInfo.getId().toString(), toAdd);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAddFileHandleAssociationsNullUser() {
		List<FileHandleAssociation> toAdd = createResultsOfSize(10);
		userInfo = null;
		// call under test
		manager.addFileHandleAssociations(userInfo, toAdd);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testAddFileHandleAssociationsNullList() {
		List<FileHandleAssociation> toAdd = null;
		// call under test
		manager.addFileHandleAssociations(userInfo, toAdd);
	}
	
	@Test
	public void testAddFileHandleAssociationsOverLimit() {
		List<FileHandleAssociation> toAdd = createResultsOfSize(BulkDownloadManagerImpl.MAX_FILES_PER_DOWNLOAD_LIST+1);
		DownloadList results = new DownloadList();
		results.setFilesToDownload(toAdd);
		when(mockBulkDownloadDao.addFilesToDownloadList(userInfo.getId().toString(), toAdd)).thenReturn(results);
		try {
			// call under test
			manager.addFileHandleAssociations(userInfo, toAdd);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(BulkDownloadManagerImpl.EXCEEDED_MAX_NUMBER_ROWS, e.getMessage());
		}
	}
	
	@Test
	public void testRemoveFileHandleAssociations() {
		List<FileHandleAssociation> toRemove = createResultsOfSize(2);
		// call under test
		manager.removeFileHandleAssociations(userInfo, toRemove);
		verify(mockBulkDownloadDao).removeFilesFromDownloadList(userInfo.getId().toString(), toRemove);
	}
	
	@Test
	public void testRemoveFileHandleAssociationsEmpty() {
		List<FileHandleAssociation> toRemove = new LinkedList<>();
		// call under test
		manager.removeFileHandleAssociations(userInfo, toRemove);
		verify(mockBulkDownloadDao).removeFilesFromDownloadList(userInfo.getId().toString(), toRemove);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRemoveFileHandleAssociationsNullUser() {
		List<FileHandleAssociation> toRemove = createResultsOfSize(2);
		userInfo = null;
		// call under test
		manager.removeFileHandleAssociations(userInfo, toRemove);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRemoveFileHandleAssociationsNullList() {
		List<FileHandleAssociation> toRemove = null;
		// call under test
		manager.removeFileHandleAssociations(userInfo, toRemove);
	}
	
	@Test
	public void testGetDownloadList() {
		// call under test
		DownloadList list = manager.getDownloadList(userInfo);
		assertNotNull(list);
		verify(mockBulkDownloadDao).getUsersDownloadList(userInfo.getId().toString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetDownloadListNullUser() {
		userInfo = null;
		// call under test
		manager.getDownloadList(userInfo);
	}
	
	@Test
	public void testClearDownloadList() {
		// call under test
		DownloadList list = manager.clearDownloadList(userInfo);
		assertNotNull(list);
		verify(mockBulkDownloadDao).clearDownloadList(userInfo.getId().toString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testClearDownloadListNullUser() {
		userInfo = null;
		// call under test
		manager.clearDownloadList(userInfo);
	}
	
	@Test
	public void truncateAllDownloadDataForAllUsersAdmin() {
		boolean isAdmin = true;
		// call under test
		manager.truncateAllDownloadDataForAllUsers(new UserInfo(isAdmin));
		verify(mockBulkDownloadDao).truncateAllDownloadDataForAllUsers();
	}

	@Test
	public void truncateAllDownloadDataForAllUsersNonAdmin() {
		boolean isAdmin = false;
		try {
			// call under test
			manager.truncateAllDownloadDataForAllUsers(new UserInfo(isAdmin));
			fail();
		} catch (UnauthorizedException e) {
			// expected
		}
		verify(mockBulkDownloadDao, never()).truncateAllDownloadDataForAllUsers();
	}

	/**
	 * Test helper.
	 * 
	 * @param size
	 * @return
	 */
	static List<FileHandleAssociation> createResultsOfSize(int size) {
		List<FileHandleAssociation> result = new LinkedList<>();
		for (int i = 0; i < size; i++) {
			FileHandleAssociation fha = new FileHandleAssociation();
			fha.setAssociateObjectId("" + i);
			fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			String indexString = "" + i;
			fha.setFileHandleId(indexString + indexString + indexString);
			result.add(fha);
		}
		return result;
	}

	/**
	 * Test helper.
	 * 
	 * @return
	 */
	static List<Row> createRows(int size) {
		List<Row> results = new LinkedList<>();
		for (int i = 0; i < size; i++) {
			Row row = new Row();
			row.setRowId(new Long(i));
			results.add(row);
		}
		return results;
	}
}
