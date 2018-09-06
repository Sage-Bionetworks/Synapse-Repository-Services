package org.sagebionetworks.repo.manager.file.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

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
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.BulkDownloadDAO;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class BulkDownloadManagerImplTest {
	
	@Mock
	EntityManager mockEntityManager;
	
	@Mock
	NodeDAO mockNodeDao;
	
	@Mock
	BulkDownloadDAO mockBulkDownloadDao;
	
	@InjectMocks
	BulkDownloadManagerImpl manager;
	
	@Captor
	ArgumentCaptor<EntityChildrenRequest> childRequestCapture;
	@Captor
	ArgumentCaptor<List<FileHandleAssociation>> associationCapture;
	@Captor
	ArgumentCaptor<String> idCapture;
	@Captor
	ArgumentCaptor<Long> versionCapture;
	
	UserInfo userInfo;
	String folderId;
	List<EntityHeader> headers;
	String nextPageToken;
	EntityChildrenResponse pageOne;
	EntityChildrenResponse pageTwo;
	
	
	@Before
	public void before() {
		userInfo = new UserInfo(false, 123L);
		folderId = "syn123";
		headers = new LinkedList<>();
		for(int i=0; i<5; i++) {
			EntityHeader header = new EntityHeader();
			header.setId(""+i);
			header.setName("name"+i);
			headers.add(header);
			header.setVersionNumber(i*3L);
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
		
		when(mockEntityManager.getChildren(any(UserInfo.class), any(EntityChildrenRequest.class))).thenReturn(pageOne, pageTwo);
		DownloadList addedFiles = new DownloadList();
		addedFiles.setFilesToDownload(new LinkedList<>());
		when(mockBulkDownloadDao.addFilesToDownloadList(any(String.class), anyListOf(FileHandleAssociation.class))).thenReturn(addedFiles);
		
		when(mockBulkDownloadDao.getUsersDownloadList(any(String.class))).thenReturn(addedFiles);
		when(mockNodeDao.getFileHandleIdForVersion(any(String.class), any(Long.class))).thenReturn("111","222","333","444");
		
	}
	
	@Test
	public void testAddFilesFromFolder() {
		// call under test
		DownloadList list = manager.addFilesFromFolder(userInfo, folderId);
		assertNotNull(list);
		// two pages
		verify(mockEntityManager, times(2)).getChildren(any(UserInfo.class), childRequestCapture.capture());
		verify(mockNodeDao, times(4)).getFileHandleIdForVersion(idCapture.capture(), versionCapture.capture());
		verify(mockBulkDownloadDao, times(2)).addFilesToDownloadList(any(String.class), associationCapture.capture());
		
		List<EntityChildrenRequest> childRequests = childRequestCapture.getAllValues();
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
		assertEquals(nextPageToken,request.getNextPageToken());
		
		List<List<FileHandleAssociation>> capturedAssociations = associationCapture.getAllValues();
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
		assertEquals("111", association.getFileHandleId());
		
		// second call
		added = capturedAssociations.get(1);
		assertNotNull(added);
		assertEquals(2, added.size());
		association = added.get(1);
		assertEquals("3", association.getAssociateObjectId());
		assertEquals(FileHandleAssociateType.FileEntity, association.getAssociateObjectType());
		assertEquals("444", association.getFileHandleId());
	
		List<String> capturedId = idCapture.getAllValues();
		assertNotNull(capturedId);
		assertEquals(4, capturedId.size());
		assertEquals("0", capturedId.get(0));
		assertEquals("3", capturedId.get(3));
		
		List<Long> capturedVersions = versionCapture.getAllValues();
		assertNotNull(capturedVersions);
		assertEquals(4, capturedVersions.size());
		assertEquals(new Long(0), capturedVersions.get(0));
		assertEquals(new Long(3*3), capturedVersions.get(3));
	}
	
	@Test
	public void testAddFilesFromFolderNoChildren() {
		// setup no children.
		EntityChildrenResponse noResutls = new EntityChildrenResponse();
		noResutls.setNextPageToken(null);
		noResutls.setPage(new LinkedList<>());
		when(mockEntityManager.getChildren(any(UserInfo.class), any(EntityChildrenRequest.class))).thenReturn(noResutls);
		// call under test
		DownloadList list = manager.addFilesFromFolder(userInfo, folderId);
		assertNotNull(list);
		verify(mockEntityManager).getChildren(any(UserInfo.class), any(EntityChildrenRequest.class));
		verify(mockNodeDao, never()).getFileHandleIdForVersion(any(String.class), any(Long.class));
		verify(mockBulkDownloadDao, never()).addFilesToDownloadList(any(String.class), anyListOf(FileHandleAssociation.class));
	}
	
	@Test
	public void testAddFilesFromFolderOverLimit() {
		// setup over limit
		DownloadList usersList = new DownloadList();
		usersList.setFilesToDownload(createResultsOfSize(BulkDownloadManagerImpl.MAX_FILES_PER_DOWNLOAD_LIST+1));
		when(mockBulkDownloadDao.addFilesToDownloadList(any(String.class), anyListOf(FileHandleAssociation.class))).thenReturn(usersList);
		try {
			// call under test
			manager.addFilesFromFolder(userInfo, folderId);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("Exceeded the maximum number of 100 files.", e.getMessage());
		}
	}
	
	List<FileHandleAssociation> createResultsOfSize(int size){
		List<FileHandleAssociation> result = new LinkedList<>();
		for(int i=0; i<size; i++) {
			FileHandleAssociation fha = new FileHandleAssociation();
			fha.setAssociateObjectId(""+i);
			fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			String indexString = ""+i;
			fha.setFileHandleId(indexString+indexString+indexString);
			result.add(fha);
		}
		return result;
	}

}
