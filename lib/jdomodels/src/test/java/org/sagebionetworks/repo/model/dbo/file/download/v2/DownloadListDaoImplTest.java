package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType.BOOLEAN;
import static org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType.DOUBLE;
import static org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType.LONG;
import static org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType.STRING;
import static org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType.TIMESTAMP_MS;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.ACTUAL_VERSION;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.CONTENT_SIZE;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.CREATED_BY;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.CREATED_ON;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.ENTITY_NAME;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.IS_ELIGIBLE_FOR_PACKAGING;
import static org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAOImpl.PROJECT_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.AvailableFilter;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortDirection;
import org.sagebionetworks.repo.model.download.SortField;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.DatasetItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DownloadListDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDao;
	@Autowired
	private DownloadListDAO downloadListDao;
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private FileHandleObjectHelper fileHandleObjectHelper;
	@Autowired
	private FileHandleDao fileHandleDao;

	private Long userOneIdLong;
	private String userOneId;

	private Long userTwoIdLong;
	private String userTwoId;

	private List<DownloadListItem> idsWithVersions;
	private List<DownloadListItem> idsWithoutVersions;
	private long limit;
	private AvailableFilter filter;

	@BeforeEach
	public void before() {
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();
		downloadListDao.truncateAllData();
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userOneIdLong = userGroupDao.create(ug);
		userOneId = "" + userOneIdLong;
		// second user
		ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userTwoIdLong = userGroupDao.create(ug);
		userTwoId = "" + userTwoIdLong;

		int numberOfIds = 4;
		idsWithVersions = new ArrayList<DownloadListItem>(numberOfIds);
		idsWithoutVersions = new ArrayList<DownloadListItem>(numberOfIds);
		for (int i = 0; i < numberOfIds; i++) {

			DownloadListItem idWithVersion = new DownloadListItem();
			idWithVersion.setFileEntityId("syn" + i);
			idWithVersion.setVersionNumber(new Long(i));
			idsWithVersions.add(idWithVersion);

			DownloadListItem idWithoutVersion = new DownloadListItem();
			idWithoutVersion.setFileEntityId("syn" + (i + numberOfIds + 1));
			idWithoutVersion.setVersionNumber(null);
			idsWithoutVersions.add(idWithoutVersion);
		}
		limit = 100L;
		filter = null;
	}

	@AfterEach
	public void after() {
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();

		if (userOneId != null) {
			userGroupDao.delete(userOneId);

		}
		if (userTwoId != null) {
			userGroupDao.delete(userTwoId);
		}
		downloadListDao.truncateAllData();
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullUserId() {
		List<DownloadListItem> batch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			Long nullUserId = null;
			// call under test
			downloadListDao.addBatchOfFilesToDownloadList(nullUserId, batch);
		}).getMessage();
		assertEquals("User Id is required.", message);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullEntityId() {
		DownloadListItem id = idsWithVersions.get(0);
		id.setFileEntityId(null);
		List<DownloadListItem> batch = Arrays.asList(id);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		}).getMessage();
		assertEquals("Null fileEntityId at index: 0", message);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullBatch() {
		List<DownloadListItem> batch = null;
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(0, addedCount);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithEmptyBatch() {
		List<DownloadListItem> batch = Collections.emptyList();
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(0, addedCount);
	}

	@Test
	public void testAddBatchOfFiles() {
		List<DownloadListItem> batch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(2, addedCount);
		validateDBODownloadList(userOneIdLong, downloadListDao.getDBODownloadList(userOneIdLong));

		List<DBODownloadListItem> items = downloadListDao.getDBODownloadListItems(userOneIdLong);
		compareIdAndVersionToListItem(userOneIdLong, batch, items);
	}

	@Test
	public void testAddBatchOfFilesWithNullItem() {
		List<DownloadListItem> batch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(0), null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		}).getMessage();
		assertEquals("Null Item found at index: 2", message);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithUpdate() throws InterruptedException {
		List<DownloadListItem> batch = Lists.newArrayList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(2, addedCount);
		DBODownloadList startingDBOList = downloadListDao.getDBODownloadList(userOneIdLong);
		assertNotNull(startingDBOList);
		assertEquals(userOneIdLong, startingDBOList.getPrincipalId());
		assertNotNull(startingDBOList.getEtag());
		assertNotNull(startingDBOList.getUpdatedOn());
		// add one item to the batch
		batch.add(idsWithoutVersions.get(1));
		// sleep to unsure the updated on changes
		Thread.sleep(2);
		// call under test
		addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		// only one file should be added since the first two in the batch are already on
		// the list.
		assertEquals(1, addedCount);
		DBODownloadList currentDBOList = downloadListDao.getDBODownloadList(userOneIdLong);
		validateListChanged(startingDBOList, currentDBOList);
		compareIdAndVersionToListItem(userOneIdLong, batch, downloadListDao.getDBODownloadListItems(userOneIdLong));
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithMultipleUsers() {
		List<DownloadListItem> userOneBatch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneBatch);
		assertEquals(userOneBatch.size(), addedCount);

		List<DownloadListItem> userTwoBatch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(1));
		// call under test
		addedCount = downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, userTwoBatch);
		assertEquals(userOneBatch.size(), addedCount);

		compareIdAndVersionToListItem(userOneIdLong, userOneBatch,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
		validateDBODownloadList(userOneIdLong, downloadListDao.getDBODownloadList(userOneIdLong));
		compareIdAndVersionToListItem(userTwoIdLong, userTwoBatch,
				downloadListDao.getDBODownloadListItems(userTwoIdLong));
		validateDBODownloadList(userTwoIdLong, downloadListDao.getDBODownloadList(userTwoIdLong));
	}

	@Test
	public void testRemoveBatchOfFilesFromDownloadListWithNullUser() {
		List<DownloadListItem> batchToRemove = Arrays.asList(idsWithVersions.get(1), idsWithoutVersions.get(0));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			Long nullUserId = null;
			// call under test
			downloadListDao.removeBatchOfFilesFromDownloadList(nullUserId, batchToRemove);
		}).getMessage();
		assertEquals("User Id is required.", message);
	}

	@Test
	public void testRemoveBatchOfFilesFromDownloadListWithNullBatch() {
		List<DownloadListItem> batchToRemove = null;
		// call under test
		long removeCount = downloadListDao.removeBatchOfFilesFromDownloadList(userOneIdLong, batchToRemove);
		assertEquals(0L, removeCount);
	}

	@Test
	public void testRemoveBatchOfFilesFromDownloadListWithEmptyBatch() {
		List<DownloadListItem> batchToRemove = Collections.emptyList();
		// call under test
		long removeCount = downloadListDao.removeBatchOfFilesFromDownloadList(userOneIdLong, batchToRemove);
		assertEquals(0L, removeCount);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithNullEntityId() {
		DownloadListItem id = idsWithVersions.get(0);
		id.setFileEntityId(null);
		List<DownloadListItem> batch = Arrays.asList(id);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			downloadListDao.removeBatchOfFilesFromDownloadList(userOneIdLong, batch);
		}).getMessage();
		assertEquals("Null fileEntityId at index: 0", message);
	}

	@Test
	public void testRemoveBatchOfFilesFromDownloadList() throws InterruptedException {
		List<DownloadListItem> startBatch = Arrays.asList(idsWithVersions.get(1), idsWithoutVersions.get(1),
				idsWithVersions.get(0), idsWithoutVersions.get(0));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, startBatch);
		DBODownloadList listStart = downloadListDao.getDBODownloadList(userOneIdLong);
		validateDBODownloadList(userOneIdLong, listStart);
		List<DownloadListItem> batchToRemove = Arrays.asList(idsWithVersions.get(1), idsWithoutVersions.get(0));
		// sleep to unsure the updated on changes
		Thread.sleep(2);
		// call under test
		long removedCount = downloadListDao.removeBatchOfFilesFromDownloadList(userOneIdLong, batchToRemove);
		assertEquals(2L, removedCount);
		// the download list etag must be updated
		validateListChanged(listStart, downloadListDao.getDBODownloadList(userOneIdLong));

		List<DownloadListItem> expectedIds = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(1));
		compareIdAndVersionToListItem(userOneIdLong, expectedIds,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}

	@Test
	public void testRemoveBatchOfFilesFromDownloadListWithNullItem() throws InterruptedException {
		List<DownloadListItem> startBatch = Arrays.asList(idsWithVersions.get(1), idsWithoutVersions.get(1),
				idsWithVersions.get(0), idsWithoutVersions.get(0));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, startBatch);
		DBODownloadList listStart = downloadListDao.getDBODownloadList(userOneIdLong);
		validateDBODownloadList(userOneIdLong, listStart);
		List<DownloadListItem> batchToRemove = Arrays.asList(idsWithVersions.get(1), idsWithoutVersions.get(0), null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			downloadListDao.removeBatchOfFilesFromDownloadList(userOneIdLong, batchToRemove);
		}).getMessage();
		assertEquals("Null Item at index: 2", message);
	}

	@Test
	public void testRemoveBatchOfFilesToDownloadListWithMultipleUsers() {
		List<DownloadListItem> userOneBatch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneBatch);
		assertEquals(userOneBatch.size(), addedCount);
		List<DownloadListItem> userTwoBatch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(1));
		addedCount = downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, userTwoBatch);
		assertEquals(userOneBatch.size(), addedCount);

		// remove the common element.
		List<DownloadListItem> batchToRemove = Arrays.asList(idsWithVersions.get(0));
		// call under test
		long removeCount = downloadListDao.removeBatchOfFilesFromDownloadList(userOneIdLong, batchToRemove);
		assertEquals(1L, removeCount);
		List<DownloadListItem> expectedUserOneList = Arrays.asList(idsWithoutVersions.get(0));
		compareIdAndVersionToListItem(userOneIdLong, expectedUserOneList,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
		// the second user's list must be unchanged.
		compareIdAndVersionToListItem(userTwoIdLong, userTwoBatch,
				downloadListDao.getDBODownloadListItems(userTwoIdLong));
	}

	@Test
	public void testClearDownloadList() throws InterruptedException {
		List<DownloadListItem> startBatch = Arrays.asList(idsWithVersions.get(1), idsWithoutVersions.get(1));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, startBatch);
		DBODownloadList listStart = downloadListDao.getDBODownloadList(userOneIdLong);
		// sleep to unsure the updated on changes
		Thread.sleep(2);
		// call under test
		downloadListDao.clearDownloadList(userOneIdLong);
		compareIdAndVersionToListItem(userOneIdLong, Collections.emptyList(),
				downloadListDao.getDBODownloadListItems(userOneIdLong));
		validateListChanged(listStart, downloadListDao.getDBODownloadList(userOneIdLong));
	}

	@Test
	public void testClearDownloadListWithMultipleUsers() {
		List<DownloadListItem> userOneBatch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneBatch);
		List<DownloadListItem> userTwoBatch = Arrays.asList(idsWithVersions.get(0), idsWithoutVersions.get(1));
		downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, userTwoBatch);

		// call under test
		downloadListDao.clearDownloadList(userOneIdLong);
		compareIdAndVersionToListItem(userOneIdLong, Collections.emptyList(),
				downloadListDao.getDBODownloadListItems(userOneIdLong));
		// second user should remain unchanged
		compareIdAndVersionToListItem(userTwoIdLong, userTwoBatch,
				downloadListDao.getDBODownloadListItems(userTwoIdLong));
	}

	@Test
	public void testClearDownloadListWithNullUserId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			Long nullUserId = null;
			// call under test
			downloadListDao.clearDownloadList(nullUserId);
		}).getMessage();
		assertEquals("User Id is required.", message);
	}

	@Test
	public void testIsMatch() {
		DownloadListItem item = new DownloadListItem().setFileEntityId("syn123").setVersionNumber(3L);
		DownloadListItemResult result = (DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn123")
				.setVersionNumber(3L);
		// call under test
		assertTrue(DownloadListDAOImpl.isMatch(item, result));
	}

	@Test
	public void testIsMatchWithDifferentIds() {
		DownloadListItem item = new DownloadListItem().setFileEntityId("syn123").setVersionNumber(3L);
		DownloadListItemResult result = (DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn456")
				.setVersionNumber(3L);
		// call under test
		assertFalse(DownloadListDAOImpl.isMatch(item, result));
	}

	@Test
	public void testIsMatchWithDifferentVersions() {
		DownloadListItem item = new DownloadListItem().setFileEntityId("syn123").setVersionNumber(3L);
		DownloadListItemResult result = (DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn123")
				.setVersionNumber(4L);
		// call under test
		assertFalse(DownloadListDAOImpl.isMatch(item, result));
	}

	@Test
	public void testIsMatchWithVersionNull() {
		DownloadListItem item = new DownloadListItem().setFileEntityId("syn123").setVersionNumber(null);
		DownloadListItemResult result = (DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn123")
				.setVersionNumber(null);
		// call under test
		assertTrue(DownloadListDAOImpl.isMatch(item, result));
	}

	@Test
	public void testIsMatchWithVersionNullItem() {
		DownloadListItem item = new DownloadListItem().setFileEntityId("syn123").setVersionNumber(null);
		DownloadListItemResult result = (DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn123")
				.setVersionNumber(1L);
		// call under test
		assertFalse(DownloadListDAOImpl.isMatch(item, result));
	}

	@Test
	public void testIsMatchWithVersionNullResult() {
		DownloadListItem item = new DownloadListItem().setFileEntityId("syn123").setVersionNumber(1L);
		DownloadListItemResult result = (DownloadListItemResult) new DownloadListItemResult().setFileEntityId("syn123")
				.setVersionNumber(null);
		// call under test
		assertFalse(DownloadListDAOImpl.isMatch(item, result));
	}
	
	@Test
	public void testGetAvailableFilesFromDownloadListWithEmptyDownloadLists() {
		int batchSize = 100;
		EntityAccessCallback mockCallback = Mockito.mock(EntityAccessCallback.class);
		when(mockCallback.filter(any())).thenReturn(Collections.emptyList());
		// Call under test
		List<Long> results = downloadListDao.getAvailableFilesFromDownloadList(mockCallback,
				userOneIdLong, batchSize);
		assertEquals(Collections.emptyList(), results);
		verifyNoMoreInteractions(mockCallback);
	}

	@Test
	public void testGetAvailableFilesFromDownloadListWithNoDownloadAccess() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 5;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);

		// Add the latest version of each file to the user's download list
		List<DownloadListItem> toAdd = files.stream()
				.map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		// Add the first version of each file to the user's download list
		toAdd = files.stream().map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		int batchSize = 100;
		// deny access to all files
		// Call under test
		List<Long> results = downloadListDao.getAvailableFilesFromDownloadList(l -> Collections.emptyList(),
				userOneIdLong, batchSize);
		assertEquals(Collections.emptyList(), results);
	}

	@Test
	public void testGetAvailableFilesFromDownloadListWithFullAccess() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 5;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);

		// Add the latest version of each file to the user's download list
		List<DownloadListItem> toAdd = files.stream()
				.map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		// Add the first version of each file to the user's download list
		toAdd = files.stream().map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		int batchSize = 100;
		// grant access to all files
		// Call under test
		List<Long> results = downloadListDao.getAvailableFilesFromDownloadList(l -> l, userOneIdLong, batchSize);
		List<Long> expected = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetAvailableFilesFromDownloadListWithPartialAccess() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 5;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		// Add the latest version of each file to the user's download list
		List<DownloadListItem> toAdd = files.stream()
				.map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		// Add the first version of each file to the user's download list
		toAdd = files.stream().map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		// grant access to a sub-set of the files
		List<Long> subSet = Arrays.asList(fileIds.get(0), fileIds.get(2), fileIds.get(4));

		int batchSize = 100;
		// Call under test
		List<Long> results = downloadListDao.getAvailableFilesFromDownloadList(l -> subSet, userOneIdLong, batchSize);
		assertEquals(subSet, results);
	}

	@Test
	public void testGetAvailableFilesFromDownloadListWithBatchingCanAccessAllFiles() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 5;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		// Add the latest version of each file to the user's download list
		List<DownloadListItem> toAdd = files.stream()
				.map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		// Add the first version of each file to the user's download list
		toAdd = files.stream().map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);


		EntityAccessCallback mockCallback = Mockito.mock(EntityAccessCallback.class);
		when(mockCallback.filter(any())).thenReturn(fileIds.subList(0, 2), fileIds.subList(2, 4),
				fileIds.subList(4, 5));

		int batchSize = 2;
		// Call under test
		List<Long> results = downloadListDao.getAvailableFilesFromDownloadList(mockCallback, userOneIdLong, batchSize);
		assertEquals(fileIds, results);
		verify(mockCallback, times(3)).filter(any());
		verify(mockCallback).filter(fileIds.subList(0, 2));
		verify(mockCallback).filter(fileIds.subList(2, 4));
		verify(mockCallback).filter(fileIds.subList(4, 5));
	}

	@Test
	public void testGetAvailableFilesFromDownloadListWithBatchingCanAccessSubsetOfFiles() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 5;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		// Add the latest version of each file to the user's download list
		List<DownloadListItem> toAdd = files.stream()
				.map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		// Add the first version of each file to the user's download list
		toAdd = files.stream().map(n -> new DownloadListItem().setFileEntityId(n.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		EntityAccessCallback mockCallback = Mockito.mock(EntityAccessCallback.class);
		when(mockCallback.filter(any())).thenReturn(Arrays.asList(fileIds.get(1)), Arrays.asList(fileIds.get(2)),
				Arrays.asList(fileIds.get(4)));

		int batchSize = 2;
		// Call under test
		List<Long> results = downloadListDao.getAvailableFilesFromDownloadList(mockCallback, userOneIdLong, batchSize);
		List<Long> expected = Arrays.asList(fileIds.get(1), fileIds.get(2), fileIds.get(4));
		assertEquals(expected, results);
		verify(mockCallback, times(3)).filter(any());
		verify(mockCallback).filter(fileIds.subList(0, 2));
		verify(mockCallback).filter(fileIds.subList(2, 4));
		verify(mockCallback).filter(fileIds.subList(4, 5));
	}

	@Test
	public void testGetDownloadListItemsWithLatestVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 1;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(1, files.size());
		Node file = nodeDao.getNode(files.get(0).getId());
		Node folder = nodeDao.getNode(file.getParentId());
		Node project = nodeDao.getNode(folder.getParentId());

		FileHandle fileHandle = fileHandleDao.get(file.getFileHandleId());

		// add the latest version of the file to the download list.
		DownloadListItem item = new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(null);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, Arrays.asList(item));

		List<DBODownloadListItem> allItems = downloadListDao.getDBODownloadListItems(userOneIdLong);

		// call under test
		List<DownloadListItemResult> results = downloadListDao.getDownloadListItems(userOneIdLong, item);

		DownloadListItemResult expectedResult = new DownloadListItemResult();
		expectedResult.setFileName(file.getName());
		expectedResult.setAddedOn(allItems.get(0).getAddedOn());
		expectedResult.setFileEntityId(file.getId());
		expectedResult.setVersionNumber(null);
		expectedResult.setProjectId(project.getId());
		expectedResult.setProjectName(project.getName());
		expectedResult.setCreatedBy(file.getCreatedByPrincipalId().toString());
		expectedResult.setCreatedOn(file.getCreatedOn());
		expectedResult.setFileSizeBytes(fileHandle.getContentSize());
		expectedResult.setIsEligibleForPackaging(true);
		expectedResult.setFileHandleId(fileHandle.getId());

		List<DownloadListItemResult> expected = Arrays.asList(expectedResult);

		assertEquals(expected, results);
	}

	@Test
	public void testGetDownloadListItemsWithPreviousVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 1;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(1, files.size());
		Node file = nodeDao.getNode(files.get(0).getId());
		Node folder = nodeDao.getNode(file.getParentId());
		Node project = nodeDao.getNode(folder.getParentId());

		Node fileV1 = nodeDao.getNodeForVersion(file.getId(), 1L);
		FileHandle fileHandle = fileHandleDao.get(fileV1.getFileHandleId());

		// add the first version of the file to the download list.
		DownloadListItem item = new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(1L);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, Arrays.asList(item));

		List<DBODownloadListItem> allItems = downloadListDao.getDBODownloadListItems(userOneIdLong);

		// call under test
		List<DownloadListItemResult> results = downloadListDao.getDownloadListItems(userOneIdLong, item);

		DownloadListItemResult expectedResult = new DownloadListItemResult();
		expectedResult.setFileName(file.getName());
		expectedResult.setAddedOn(allItems.get(0).getAddedOn());
		expectedResult.setFileEntityId(file.getId());
		expectedResult.setVersionNumber(1L);
		expectedResult.setProjectId(project.getId());
		expectedResult.setProjectName(project.getName());
		expectedResult.setCreatedBy(file.getCreatedByPrincipalId().toString());
		expectedResult.setCreatedOn(file.getCreatedOn());
		expectedResult.setFileSizeBytes(fileHandle.getContentSize());
		expectedResult.setIsEligibleForPackaging(true);
		expectedResult.setFileHandleId(fileHandle.getId());

		List<DownloadListItemResult> expected = Arrays.asList(expectedResult);

		assertEquals(expected, results);
	}

	@Test
	public void testGetDownloadListItemsWithNestedProjects() {
		Node p1 = nodeDaoHelper.create(n -> {
			n.setName("p1");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});
		Node p2 = nodeDaoHelper.create(n -> {
			n.setName("p2");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(p1.getId());
			n.setNodeType(EntityType.project);
		});

		FileHandle fh1 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(123L);
			h.setFileName("file.txt");
			h.setBucketName(null);
		});
		Node file = nodeDaoHelper.create(n -> {
			n.setName("f1");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(p2.getId());
			n.setNodeType(EntityType.file);
			n.setFileHandleId(fh1.getId());
		});

		DownloadListItem item = new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(null);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, Arrays.asList(item));

		List<DBODownloadListItem> allItems = downloadListDao.getDBODownloadListItems(userOneIdLong);

		// call under test
		List<DownloadListItemResult> results = downloadListDao.getDownloadListItems(userOneIdLong, item);

		DownloadListItemResult expectedResult = new DownloadListItemResult();
		expectedResult.setFileName(file.getName());
		expectedResult.setAddedOn(allItems.get(0).getAddedOn());
		expectedResult.setFileEntityId(file.getId());
		expectedResult.setVersionNumber(null);
		expectedResult.setProjectId(p2.getId());
		expectedResult.setProjectName(p2.getName());
		expectedResult.setCreatedBy(file.getCreatedByPrincipalId().toString());
		expectedResult.setCreatedOn(file.getCreatedOn());
		expectedResult.setFileSizeBytes(fh1.getContentSize());
		expectedResult.setIsEligibleForPackaging(true);
		expectedResult.setFileHandleId(fh1.getId());

		List<DownloadListItemResult> expected = Arrays.asList(expectedResult);

		assertEquals(expected, results);
	}

	@Test
	public void testGetDownloadListItemsWithMultipleUsers() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());

		List<DownloadListItem> userOneItems = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);

		List<DownloadListItem> userTwoItems = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(2).getId()).setVersionNumber(2L));

		downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, userTwoItems);

		// call under test
		List<DownloadListItemResult> oneResults = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.toArray(new DownloadListItem[userOneItems.size()]));
		validateMatches(userOneItems, oneResults);

		// call under test
		List<DownloadListItemResult> twoResults = downloadListDao.getDownloadListItems(userTwoIdLong,
				userTwoItems.toArray(new DownloadListItem[userTwoItems.size()]));
		validateMatches(userTwoItems, twoResults);
	}
	
	@Test
	public void testGetColumnName() {
		assertEquals(ENTITY_NAME, DownloadListDAOImpl.getColumnName(SortField.fileName));
		assertEquals(PROJECT_NAME, DownloadListDAOImpl.getColumnName(SortField.projectName));
		assertEquals(COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID, DownloadListDAOImpl.getColumnName(SortField.synId));
		assertEquals(ACTUAL_VERSION, DownloadListDAOImpl.getColumnName(SortField.versionNumber));
		assertEquals(COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON, DownloadListDAOImpl.getColumnName(SortField.addedOn));
		assertEquals(CREATED_BY, DownloadListDAOImpl.getColumnName(SortField.createdBy));
		assertEquals(CREATED_ON, DownloadListDAOImpl.getColumnName(SortField.createdOn));
		assertEquals(CONTENT_SIZE, DownloadListDAOImpl.getColumnName(SortField.fileSize));
		assertEquals(IS_ELIGIBLE_FOR_PACKAGING, DownloadListDAOImpl.getColumnName(SortField.isEligibleForPackaging));
	}
	
	@Test
	public void testGetColumnNameEachType() {
		for(SortField field: SortField.values()) {
			assertNotNull(DownloadListDAOImpl.getColumnName(field));
		}
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffix() {
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileName).setDirection(SortDirection.ASC),
				new Sort().setField(SortField.projectName).setDirection(SortDirection.DESC));
		Long limit = 1L;
		Long offset = 0L;
		// call under test
		assertEquals(" ORDER BY ENTITY_NAME ASC, PROJECT_NAME DESC LIMIT :limit OFFSET :offset",
				DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset));
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffixWitNullLimit() {
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileName).setDirection(SortDirection.ASC),
				new Sort().setField(SortField.projectName).setDirection(SortDirection.DESC));
		Long limit = null;
		Long offset = 0L;
		// call under test
		assertEquals(" ORDER BY ENTITY_NAME ASC, PROJECT_NAME DESC OFFSET :offset",
				DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset));
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffixWitNullOffset() {
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileName).setDirection(SortDirection.ASC),
				new Sort().setField(SortField.projectName).setDirection(SortDirection.DESC));
		Long limit = 1L;
		Long offset = null;
		// call under test
		assertEquals(" ORDER BY ENTITY_NAME ASC, PROJECT_NAME DESC LIMIT :limit",
				DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset));
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffixWithNullDirection() {
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileName).setDirection(null));
		Long limit = 1L;
		Long offset = 0L;
		// call under test
		assertEquals(" ORDER BY ENTITY_NAME LIMIT :limit OFFSET :offset",
				DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset));
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffixWithNullSortField() {
		List<Sort> sort = Arrays.asList(new Sort().setField(null).setDirection(SortDirection.DESC));
		Long limit = 1L;
		Long offset = 0L;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset);
		}).getMessage();
		assertEquals("sort.field is required.", message);
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffixWithNullSort() {
		List<Sort> sort = null;
		Long limit = 1L;
		Long offset = 0L;
		// call under test
		assertEquals(" LIMIT :limit OFFSET :offset",
				DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset));
	}
	
	@Test
	public void testBuildAvailableDownloadQuerySuffixWithAllNull() {
		List<Sort> sort = null;
		Long limit = null;
		Long offset = null;
		// call under test
		assertEquals("", DownloadListDAOImpl.buildAvailableDownloadQuerySuffix(sort, limit, offset));
	}
	
	@Test
	public void testBuildAvailableFilterWithNull() {
		AvailableFilter filter = null;
		// call under test
		assertEquals("", DownloadListDAOImpl.buildAvailableFilter(filter));
	}
	
	@Test
	public void testBuildAvailableFilterWithEligible() {
		AvailableFilter filter = AvailableFilter.eligibleForPackaging;
		// call under test
		assertEquals(" WHERE F.METADATA_TYPE = 'S3' AND F.CONTENT_SIZE <= :maxEligibleSize", DownloadListDAOImpl.buildAvailableFilter(filter));
	}
	
	@Test
	public void testBuildAvailableFilterWithIneligible() {
		AvailableFilter filter = AvailableFilter.ineligibleForPackaging;
		// call under test
		assertEquals(" WHERE F.METADATA_TYPE <> 'S3' OR F.CONTENT_SIZE > :maxEligibleSize", DownloadListDAOImpl.buildAvailableFilter(filter));
	}
	
	
	@Test
	public void testBuildAvailableFilterWithEachTypel() {
		for(AvailableFilter filter: AvailableFilter.values()) {
			// call under test
			String result = DownloadListDAOImpl.buildAvailableFilter(filter);
			assertNotNull(result);
		}
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownload() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());

		List<DownloadListItem> userOneItems = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(3).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(4).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(5).getId()).setVersionNumber(null));

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.toArray(new DownloadListItem[userOneItems.size()]));

		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.ASC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadWithSortIsEligibleForPackaging() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 0));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});

		// S3 under max (eligible)
		String fileName = "s3UnderSize";
		FileHandle fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
		}, S3FileHandle.class);
		Node fileS3UnderSize = createFile(project.getId(), fileName, fileHandle);

		// S3 over max (ineligible)
		fileName = "s3OverSize";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING+1);
		}, S3FileHandle.class);
		Node fileS3OverSize = createFile(project.getId(), fileName, fileHandle);

		// External (ineligible)
		fileName = "external";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize( 101L);
		}, ExternalFileHandle.class);
		Node fileExternal = createFile(project.getId(), fileName, fileHandle);

		List<DownloadListItem> toAdd = Arrays.asList(
				new DownloadListItem().setFileEntityId(fileS3UnderSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileS3OverSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileExternal.getId()).setVersionNumber(1L));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong, toAdd.get(0), toAdd.get(2), toAdd.get(1));
		assertTrue(expected.get(0).getIsEligibleForPackaging());
		assertFalse(expected.get(1).getIsEligibleForPackaging());
		assertFalse(expected.get(2).getIsEligibleForPackaging());

		filter = null;
		List<Sort> sort = Arrays.asList(
				new Sort().setField(SortField.isEligibleForPackaging).setDirection(SortDirection.DESC),
				new Sort().setField(SortField.synId).setDirection(SortDirection.DESC)
		);
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadWithFilterEligile() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 0));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});

		// S3 under max (eligible)
		String fileName = "s3UnderSize";
		FileHandle fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
		}, S3FileHandle.class);
		Node fileS3UnderSize = createFile(project.getId(), fileName, fileHandle);

		// S3 over max (ineligible)
		fileName = "s3OverSize";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING+1);
		}, S3FileHandle.class);
		Node fileS3OverSize = createFile(project.getId(), fileName, fileHandle);

		// External (ineligible)
		fileName = "external";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize( 101L);
		}, ExternalFileHandle.class);
		Node fileExternal = createFile(project.getId(), fileName, fileHandle);

		List<DownloadListItem> toAdd = Arrays.asList(
				new DownloadListItem().setFileEntityId(fileS3UnderSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileS3OverSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileExternal.getId()).setVersionNumber(1L));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong, toAdd.get(0));

		filter = AvailableFilter.eligibleForPackaging;
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.DESC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadWithFilterIneligile() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 0));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});

		// S3 under max (eligible)
		String fileName = "s3UnderSize";
		FileHandle fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
		}, S3FileHandle.class);
		Node fileS3UnderSize = createFile(project.getId(), fileName, fileHandle);

		// S3 over max (ineligible)
		fileName = "s3OverSize";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING+1);
		}, S3FileHandle.class);
		Node fileS3OverSize = createFile(project.getId(), fileName, fileHandle);

		// External (ineligible)
		fileName = "external";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize( 101L);
		}, ExternalFileHandle.class);
		Node fileExternal = createFile(project.getId(), fileName, fileHandle);

		List<DownloadListItem> toAdd = Arrays.asList(
				new DownloadListItem().setFileEntityId(fileS3UnderSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileS3OverSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileExternal.getId()).setVersionNumber(1L));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong, toAdd.get(2), toAdd.get(1));

		filter = AvailableFilter.ineligibleForPackaging;
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.DESC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}

	@Test
	public void testGetFilesAvailableToDownloadFromDownloadWithNullFilter() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 0));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});

		// S3 under max (eligible)
		String fileName = "s3UnderSize";
		FileHandle fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
		}, S3FileHandle.class);
		Node fileS3UnderSize = createFile(project.getId(), fileName, fileHandle);

		// S3 over max (ineligible)
		fileName = "s3OverSize";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING+1);
		}, S3FileHandle.class);
		Node fileS3OverSize = createFile(project.getId(), fileName, fileHandle);

		// External (ineligible)
		fileName = "external";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize( 101L);
		}, ExternalFileHandle.class);
		Node fileExternal = createFile(project.getId(), fileName, fileHandle);

		List<DownloadListItem> toAdd = Arrays.asList(
				new DownloadListItem().setFileEntityId(fileS3UnderSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileS3OverSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileExternal.getId()).setVersionNumber(1L));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong, toAdd.get(2), toAdd.get(1), toAdd.get(0));

		filter = null;
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.DESC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadListWithSubAccess() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		List<DownloadListItem> userOneItems = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(3).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(4).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(5).getId()).setVersionNumber(null));

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.get(1), userOneItems.get(3), userOneItems.get(4));
		
		// grant access to a sub-set of the files
		List<Long> subSet = Arrays.asList(fileIds.get(1), fileIds.get(4), fileIds.get(5));

		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.ASC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> subSet,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadListWithLimitOffset() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());

		List<DownloadListItem> userOneItems = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(3).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(4).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(5).getId()).setVersionNumber(null));

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong, userOneItems.get(3),
				userOneItems.get(4));

		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.ASC));
		Long limit = 2L;
		Long offset = 3L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadListWithOrderByProjectName() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());

		// Add the latest version of each file to user's download list
		List<DownloadListItem> userOneItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.get(3),
				userOneItems.get(4),
				userOneItems.get(5),
				userOneItems.get(0),
				userOneItems.get(1),
				userOneItems.get(2)
		);

		List<Sort> sort = Arrays.asList(
				new Sort().setField(SortField.projectName).setDirection(SortDirection.DESC),
				new Sort().setField(SortField.synId).setDirection(SortDirection.ASC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadListWithOrderByEntityNameAndVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());

		// add latest version
		List<DownloadListItem> currentVersionItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		// add first version
		List<DownloadListItem> firstVersionItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		List<DownloadListItem> userOneItems = new ArrayList<>(files.size()*2);
		userOneItems.addAll(currentVersionItems);
		userOneItems.addAll(firstVersionItems);

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);
		
		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.get(5),
				userOneItems.get(2),
				userOneItems.get(4),
				userOneItems.get(1),
				userOneItems.get(3),
				userOneItems.get(0)
		);

		List<Sort> sort = Arrays.asList(
				new Sort().setField(SortField.fileName).setDirection(SortDirection.DESC),
				new Sort().setField(SortField.versionNumber).setDirection(SortDirection.ASC));
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadListWithMultipleVersionOfSameFile() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 1;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(1, files.size());

		List<DownloadListItem> userOneItems = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(null)
		);

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);

		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.get(2),
				userOneItems.get(0),
				userOneItems.get(1)
		);

		List<Sort> sort = null;
		Long limit = 100L;
		Long offset = 0L;
		// call under test
		List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
				userOneIdLong, filter, sort, limit, offset);
		assertEquals(expected.size(), result.size());
		assertTrue(expected.containsAll(result));
	}
	
	@Test
	public void testGetFilesAvailableToDownloadFromDownloadListWithSortEachField() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());

		List<DownloadListItem> userOneItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);
		
		List<DownloadListItemResult> expected = downloadListDao.getDownloadListItems(userOneIdLong,
				userOneItems.toArray(new DownloadListItem[userOneItems.size()]));
		
		for(SortField field: SortField.values()) {
			List<Sort> sort = Arrays.asList(
					new Sort().setField(field).setDirection(SortDirection.DESC));
			Long limit = 100L;
			Long offset = 0L;
			// call under test
			List<DownloadListItemResult> result = downloadListDao.getFilesAvailableToDownloadFromDownloadList(l -> l,
					userOneIdLong, filter, sort, limit, offset);
			assertEquals(expected.size(), result.size());
			assertTrue(expected.containsAll(result));
		}
	}
	
	@Test
	public void testGetTotalNumberOfFilesOnDownloadList() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());

		List<DownloadListItem> userOneItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);
		
		userOneItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());

		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneItems);
		
		List<DownloadListItem> userTwoItems = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());

		downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, userTwoItems);
		
		// call under test
		long count = downloadListDao.getTotalNumberOfFilesOnDownloadList(userOneIdLong);
		assertEquals(12L, count);
		
		// call under test
		count = downloadListDao.getTotalNumberOfFilesOnDownloadList(userTwoIdLong);
		assertEquals(6L, count);
	}
	
	@Test
	public void testFilterUnsupportedTypes() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 2;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		
		Node folder = nodeDao.getNode(files.get(0).getParentId());
		assertNotNull(folder);
		assertEquals(EntityType.folder, folder.getNodeType());
		Node project = nodeDao.getNode(folder.getParentId());
		assertNotNull(project);
		assertEquals(EntityType.project, project.getNodeType());
		
		List<DownloadListItem> filesV1 = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(1L))
				.collect(Collectors.toList());
		List<DownloadListItem> filesV2 = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		List<DownloadListItem> filesVNull = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		
		List<DownloadListItem> otherTypes = Arrays.asList(
				new DownloadListItem().setFileEntityId(folder.getId()),
				new DownloadListItem().setFileEntityId(project.getId())
		);
		List<DownloadListItem> toFilter = new ArrayList<>();
		toFilter.addAll(filesV1);
		toFilter.addAll(otherTypes);
		toFilter.addAll(filesV2);
		toFilter.addAll(filesVNull);
		
		// call under test
		List<DownloadListItem> results = downloadListDao.filterUnsupportedTypes(toFilter);
		
		List<DownloadListItem> expected = new ArrayList<>();
		expected.addAll(filesV1);
		expected.addAll(filesV2);
		expected.addAll(filesVNull);
		assertEquals(expected, results);
	}
	
	@Test
	public void testFilterUnsupportedTypesWithNullBatch() {
		List<DownloadListItem> toFilter = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			downloadListDao.filterUnsupportedTypes(toFilter);
		}).getMessage();
		assertEquals("batch is required.", message);
	}
	
	@Test
	public void testFilterUnsupportedTypesWithEmptyBatch() {
		List<DownloadListItem> toFilter = Collections.emptyList();
		// call under test
		List<DownloadListItem> results = downloadListDao.filterUnsupportedTypes(toFilter);
		assertEquals(Collections.emptyList(), results);
	}
	
	@Test
	public void testGetListStatistics() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
				
		FilesStatisticsResponse expected = new FilesStatisticsResponse()
				.setNumberOfFilesAvailableForDownload(12L)
				.setNumberOfFilesAvailableForDownloadAndEligibleForPackaging(12L)
				.setNumberOfFilesRequiringAction(0L)
				.setSumOfFileSizesAvailableForDownload(getSumFileSize(fileIds)*2L)
				.setTotalNumberOfFiles(12L);
		
		// call under test
		FilesStatisticsResponse stats = downloadListDao.getListStatistics(l->l, userOneIdLong);
		assertEquals(expected, stats);
	}
	
	@Test
	public void testGetListStatisticsWithRestrictions() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		// grant access to a sub-set of the files
		List<Long> subSet = Arrays.asList(fileIds.get(1), fileIds.get(5));
		
		FilesStatisticsResponse expected = new FilesStatisticsResponse()
				.setNumberOfFilesAvailableForDownload(4L)
				.setNumberOfFilesAvailableForDownloadAndEligibleForPackaging(4L)
				.setNumberOfFilesRequiringAction(8L)
				.setSumOfFileSizesAvailableForDownload(getSumFileSize(subSet)*2L)
				.setTotalNumberOfFiles(12L);
		
		// call under test
		FilesStatisticsResponse stats = downloadListDao.getListStatistics(l->subSet, userOneIdLong);
		assertEquals(expected, stats);
	}
	
	@Test
	public void testGetListStatisticsWithMultipleUsers() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		
		List<DownloadListItem> toAdd = Arrays.asList(items.get(0), items.get(1), items.get(2));
		List<Long> accessibleToOne = Arrays.asList(fileIds.get(1), fileIds.get(2));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		
		toAdd = Arrays.asList(items.get(1), items.get(2), items.get(3), items.get(4), items.get(5));
		List<Long> accessibleToTwo = Arrays.asList(fileIds.get(1), fileIds.get(2), fileIds.get(5));
		downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, toAdd);
		
		FilesStatisticsResponse expectedOne = new FilesStatisticsResponse()
				.setNumberOfFilesAvailableForDownload(2L)
				.setNumberOfFilesAvailableForDownloadAndEligibleForPackaging(2L)
				.setNumberOfFilesRequiringAction(1L)
				.setSumOfFileSizesAvailableForDownload(getSumFileSize(accessibleToOne))
				.setTotalNumberOfFiles(3L);
		
		// call under test
		FilesStatisticsResponse stats = downloadListDao.getListStatistics(l->accessibleToOne, userOneIdLong);
		assertEquals(expectedOne, stats);
		
		FilesStatisticsResponse expectedTwo = new FilesStatisticsResponse()
				.setNumberOfFilesAvailableForDownload(3L)
				.setNumberOfFilesAvailableForDownloadAndEligibleForPackaging(3L)
				.setNumberOfFilesRequiringAction(2L)
				.setSumOfFileSizesAvailableForDownload(getSumFileSize(accessibleToTwo))
				.setTotalNumberOfFiles(5L);
		
		// call under test
		stats = downloadListDao.getListStatistics(l->accessibleToTwo, userTwoIdLong);
		assertEquals(expectedTwo, stats);
	}
	
	@Test
	public void testGetListStatisticsWithFilesIneligibleForPackaging() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 0));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});

		// S3 under max (eligible)
		String fileName = "s3UnderSize";
		FileHandle fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
		}, S3FileHandle.class);
		Node fileS3UnderSize = createFile(project.getId(), fileName, fileHandle);

		// S3 over max (ineligible)
		fileName = "s3OverSize";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize(FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING+1);
		}, S3FileHandle.class);
		Node fileS3OverSize = createFile(project.getId(), fileName, fileHandle);

		// External (ineligible)
		fileName = "external";
		fileHandle = fileHandleObjectHelper.createFileHandle(f->{
			f.setContentSize( 101L);
		}, ExternalFileHandle.class);
		Node fileExternal = createFile(project.getId(), fileName, fileHandle);
		
		List<Long> fileIds = Arrays.asList(fileS3UnderSize, fileS3OverSize, fileExternal).stream()
				.map(n -> KeyFactory.stringToKey(n.getId())).collect((Collectors.toList()));

		List<DownloadListItem> toAdd = Arrays.asList(
				new DownloadListItem().setFileEntityId(fileS3UnderSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileS3OverSize.getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(fileExternal.getId()).setVersionNumber(1L));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		
		FilesStatisticsResponse expected = new FilesStatisticsResponse()
				.setNumberOfFilesAvailableForDownload(3L)
				.setNumberOfFilesAvailableForDownloadAndEligibleForPackaging(1L)
				.setNumberOfFilesRequiringAction(0L)
				.setSumOfFileSizesAvailableForDownload(getSumFileSize(fileIds))
				.setTotalNumberOfFiles(3L);
		
		// call under test
		FilesStatisticsResponse stats = downloadListDao.getListStatistics(l->l, userOneIdLong);
		assertEquals(expected, stats);
	}
	
	/**
	 * Helper to calculate the sum of the sizes of the given node Ids
	 * 
	 * @param fileIds
	 * @return
	 */
	public long getSumFileSize(List<Long> fileIds) {
		long sum = 0L;
		for(Long id: fileIds) {
			Node node = nodeDao.getNode(id.toString());
			FileHandle fh = fileHandleDao.get(node.getFileHandleId());
			sum += fh.getContentSize();
		}
		return sum;
	}


	/**
	 * Create a simple hierarchy of files.  Each file created will have two versions.
	 * 
	 * @param numberOfProject   The number of root projects
	 * @param foldersPerProject The number of folders in each project
	 * @param filesPerFolder    The number of files per folder.
	 * @return Only the files are returned.
	 */
	public List<Node> createFileHierarchy(int numberOfProject, int foldersPerProject, int filesPerFolder) {
		List<Node> results = new ArrayList<Node>(numberOfProject * foldersPerProject * filesPerFolder);
		for (int p = 0; p < numberOfProject; p++) {
			final int projectNumber = p;
			Node project = nodeDaoHelper.create(n -> {
				n.setName(String.join("-", "project", "" + projectNumber));
				n.setCreatedByPrincipalId(userOneIdLong);
				n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
				n.setNodeType(EntityType.project);
			});
			for (int d = 0; d < foldersPerProject; d++) {
				final int dirNumber = d;
				Node dir = nodeDaoHelper.create(n -> {
					n.setName(String.join("-", "dir", "" + projectNumber, "" + dirNumber));
					n.setCreatedByPrincipalId(userOneIdLong);
					n.setParentId(project.getId());
					n.setNodeType(EntityType.folder);
				});
				for (int f = 0; f < filesPerFolder; f++) {
					final int fileNumber = f;
					final String fileName = String.join("-", "file", "" + projectNumber, "" + dirNumber,
							"" + fileNumber);
					Long contentSize = 1L + (2L * fileNumber);
					FileHandle fh1 = fileHandleObjectHelper.create(h -> {
						h.setContentSize(contentSize);
						h.setFileName(fileName);
					});
					
					Node file = createFile(dir.getId(), fileName, fh1);
					// Create a second version for this file.
					final String fileName2 = String.join("-", fileName, "v2");
					FileHandle fh2 = fileHandleObjectHelper.create(h -> {
						h.setContentSize(1L + (2L * fileNumber + 1));
						h.setFileName(fileName2);
					});
					file.setFileHandleId(fh2.getId());
					file.setVersionComment("v2");
					file.setVersionLabel("v2");
					nodeDao.createNewVersion(file);
					file = nodeDao.getNode(file.getId());
					results.add(file);
				}
			}
		}
		return results;
	}

	/**
	 * Create a single file in the given folder.

	 */
	Node createFile(String parentId, String fileName, FileHandle fileHandle) {
		Node file = nodeDaoHelper.create(n -> {
			n.setName(fileName);
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(parentId);
			n.setNodeType(EntityType.file);
			n.setFileHandleId(fileHandle.getId());
			n.setVersionNumber(1L);
			n.setVersionComment("v1");
			n.setVersionLabel("v1");
		});
		return file;
	}


	/**
	 * Validate that the passed items match the order, ids, and version of the
	 * passed results.
	 * 
	 * @param items
	 * @param results
	 */
	public static void validateMatches(List<DownloadListItem> items, List<DownloadListItemResult> results) {
		assertNotNull(items);
		assertNotNull(results);
		assertEquals(items.size(), results.size());
		for (int i = 0; i < items.size(); i++) {
			assertTrue(DownloadListDAOImpl.isMatch(items.get(0), results.get(0)));
		}
	}

	/**
	 * Helper to validate a download list.
	 * 
	 * @param principalId
	 * @param list
	 */
	public static void validateDBODownloadList(Long principalId, DBODownloadList list) {
		assertNotNull(list);
		assertEquals(principalId, list.getPrincipalId());
		assertNotNull(list.getEtag());
		assertNotNull(list.getUpdatedOn());
	}

	/**
	 * Helper to validate that the current download list has a new etag and
	 * updatedOn.
	 * 
	 * @param start
	 * @param current
	 */
	public static void validateListChanged(DBODownloadList start, DBODownloadList current) {
		assertNotNull(start);
		assertNotNull(current);
		assertEquals(start.getPrincipalId(), current.getPrincipalId());
		assertNotEquals(start.getEtag(), current.getEtag());
		assertTrue(current.getUpdatedOn().after(start.getUpdatedOn()));
	}

	/**
	 * Helper to compare a list of IdAndVersion to a list of DBODownloadListItem.
	 * 
	 * @param ids
	 * @param items
	 */
	public static void compareIdAndVersionToListItem(Long principalId, List<DownloadListItem> ids,
			List<DBODownloadListItem> items) {
		assertNotNull(principalId);
		assertNotNull(ids);
		assertNotNull(items);
		assertEquals(ids.size(), items.size());
		for (int i = 0; i < ids.size(); i++) {
			DownloadListItem id = ids.get(i);
			assertNotNull(id);
			DBODownloadListItem item = items.get(i);
			assertNotNull(item);
			assertEquals(KeyFactory.stringToKey(id.getFileEntityId()), item.getEntityId());
			if (id.getVersionNumber() == null) {
				assertEquals(DownloadListDAOImpl.NULL_VERSION_NUMBER, item.getVersionNumber());
			} else {
				assertEquals(id.getVersionNumber(), item.getVersionNumber());
			}
			assertEquals(principalId, item.getPrincipalId());
			assertNotNull(item.getAddedOn());
		}
	}
	
	@Test
	public void testGetActionsRequiredFromDownloadList() {
		int numberOfProject = 3;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(9, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());
		
		MeetAccessRequirement restrictionOne = new MeetAccessRequirement().setAccessRequirementId(999L);
		RequestDownload downloadOne = new RequestDownload().setBenefactorId(888L);
		MeetAccessRequirement restrictionTwo = new MeetAccessRequirement().setAccessRequirementId(777L);
		
		List<FileActionRequired> actionsRequired = Arrays.asList(
				new FileActionRequired().withFileId(fileIds.get(0)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(2)).withAction(downloadOne),
				new FileActionRequired().withFileId(fileIds.get(3)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(5)).withAction(downloadOne),
				new FileActionRequired().withFileId(fileIds.get(6)).withAction(downloadOne),
				new FileActionRequired().withFileId(fileIds.get(8)).withAction(restrictionTwo)
		);

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		long limit = 100L;
		long offset = 0L;
				
		List<ActionRequiredCount> expected = Arrays.asList(
				new ActionRequiredCount().setAction(downloadOne).setCount(6L),
				new ActionRequiredCount().setAction(restrictionOne).setCount(4L),
				new ActionRequiredCount().setAction(restrictionTwo).setCount(2L)
		);
		
		// call under test
		List<ActionRequiredCount> result = downloadListDao.getActionsRequiredFromDownloadList(l -> actionsRequired,
				userOneIdLong, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetActionsRequiredFromDownloadListWithPagination() {
		int numberOfProject = 3;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(9, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());
		
		MeetAccessRequirement restrictionOne = new MeetAccessRequirement().setAccessRequirementId(999L);
		RequestDownload downloadOne = new RequestDownload().setBenefactorId(888L);
		MeetAccessRequirement restrictionTwo = new MeetAccessRequirement().setAccessRequirementId(777L);
		
		List<FileActionRequired> actionsRequired = Arrays.asList(
				new FileActionRequired().withFileId(fileIds.get(0)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(2)).withAction(downloadOne),
				new FileActionRequired().withFileId(fileIds.get(3)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(5)).withAction(downloadOne),
				new FileActionRequired().withFileId(fileIds.get(6)).withAction(downloadOne),
				new FileActionRequired().withFileId(fileIds.get(8)).withAction(restrictionTwo)
		);

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		long limit = 1L;
		long offset = 2L;
				
		List<ActionRequiredCount> expected = Arrays.asList(
				new ActionRequiredCount().setAction(restrictionTwo).setCount(2L)
		);
		
		// call under test
		List<ActionRequiredCount> result = downloadListDao.getActionsRequiredFromDownloadList(l -> actionsRequired,
				userOneIdLong, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetActionsRequiredFromDownloadListWithNoActions() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 2;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(4, files.size());
		
		List<FileActionRequired> actionsRequired = Collections.emptyList();

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(null))
				.collect(Collectors.toList());
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, items);
		
		long limit = 100L;
		long offset = 0L;
				
		List<ActionRequiredCount> expected = Collections.emptyList();
		
		// call under test
		List<ActionRequiredCount> result = downloadListDao.getActionsRequiredFromDownloadList(l -> actionsRequired,
				userOneIdLong, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetActionsRequiredFromDownloadListWithMultipleUsers() {
		int numberOfProject = 2;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(6, files.size());
		List<Long> fileIds = files.stream().map(n -> KeyFactory.stringToKey(n.getId())).collect(Collectors.toList());

		List<DownloadListItem> items = files.stream()
				.map(f -> new DownloadListItem().setFileEntityId(f.getId()).setVersionNumber(2L))
				.collect(Collectors.toList());
		
		List<DownloadListItem> toAdd = Arrays.asList(items.get(0), items.get(1), items.get(2));
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, toAdd);
		
		toAdd = Arrays.asList(items.get(1), items.get(2), items.get(3), items.get(4), items.get(5));
		downloadListDao.addBatchOfFilesToDownloadList(userTwoIdLong, toAdd);
		
		MeetAccessRequirement restrictionOne = new MeetAccessRequirement().setAccessRequirementId(999L);
		MeetAccessRequirement restrictionTwo = new MeetAccessRequirement().setAccessRequirementId(888L);
		
		// user one
		List<FileActionRequired> actionsRequired = Arrays.asList(
				new FileActionRequired().withFileId(fileIds.get(0)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(1)).withAction(restrictionOne)
		);
		long limit = 100L;
		long offset = 0L;	
		List<ActionRequiredCount> expected = Arrays.asList(
				new ActionRequiredCount().setAction(restrictionOne).setCount(2L)
		);
		
		// call under test
		List<ActionRequiredCount> result = downloadListDao.getActionsRequiredFromDownloadList(l -> actionsRequired,
				userOneIdLong, limit, offset);
		assertEquals(expected, result);
		
		// user two
		List<FileActionRequired> actionsRequiredTwo = Arrays.asList(
				new FileActionRequired().withFileId(fileIds.get(2)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(4)).withAction(restrictionOne),
				new FileActionRequired().withFileId(fileIds.get(5)).withAction(restrictionTwo)
		);	
		expected = Arrays.asList(
				new ActionRequiredCount().setAction(restrictionOne).setCount(2L),
				new ActionRequiredCount().setAction(restrictionTwo).setCount(1L)
		);
		
		// call under test
		result = downloadListDao.getActionsRequiredFromDownloadList(l -> actionsRequiredTwo,
				userTwoIdLong, limit, offset);
		assertEquals(expected, result);
	}
	
	@Test
	public void testAddChildrenToDownloadListWithUseVersionTrue() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		long parentId = KeyFactory.stringToKey(files.get(0).getParentId());
		boolean useVersion = true;
		// call under test
		Long count = downloadListDao.addChildrenToDownloadList(userOneIdLong, parentId, useVersion, limit);
		assertEquals(3L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(2).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddChildrenToDownloadListWithLimit() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		limit = 2;
		long parentId = KeyFactory.stringToKey(files.get(0).getParentId());
		boolean useVersion = true;
		// call under test
		Long count = downloadListDao.addChildrenToDownloadList(userOneIdLong, parentId, useVersion, limit);
		assertEquals(2L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddChildrenToDownloadListWithFilesAlreadyOnListDifferentVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		
		List<DownloadListItem> alreadyOnList = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(1L)
		);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, alreadyOnList);
		
		long parentId = KeyFactory.stringToKey(files.get(0).getParentId());
		boolean useVersion = true;
		// call under test
		Long count = downloadListDao.addChildrenToDownloadList(userOneIdLong, parentId, useVersion, limit);
		assertEquals(3L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(1l),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(2).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddChildrenToDownloadListWithFilesAlreadyOnListSameVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		
		List<DownloadListItem> alreadyOnList = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L)
		);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, alreadyOnList);
		
		long parentId = KeyFactory.stringToKey(files.get(0).getParentId());
		boolean useVersion = true;
		// call under test
		Long count = downloadListDao.addChildrenToDownloadList(userOneIdLong, parentId, useVersion, limit);
		assertEquals(2L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2l),
				new DownloadListItem().setFileEntityId(files.get(2).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddChildrenToDownloadListWithUseVersionFalse() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		long parentId = KeyFactory.stringToKey(files.get(0).getParentId());
		boolean useVersion = false;
		// call under test
		Long count = downloadListDao.addChildrenToDownloadList(userOneIdLong, parentId, useVersion, limit);
		assertEquals(3L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(2).getId()).setVersionNumber(null)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddChildrenToDownloadListWithFilesAndFolders() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 1;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(1, files.size());
		long parentId = KeyFactory.stringToKey(files.get(0).getParentId());
		// The folder should not appear on the download list.
		Node folder = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "dir", "" + 4, "" + 5));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(KeyFactory.keyToString(parentId));
			n.setNodeType(EntityType.folder);
		});
		boolean useVersion = true;
		// call under test
		Long count = downloadListDao.addChildrenToDownloadList(userOneIdLong, parentId, useVersion, limit);
		assertEquals(1L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddDatasetItemsToDownloadListWithUseVersionTrue() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		boolean useVersion = true;
		// add the first 2 of 3 files to the dataset
		List<DatasetItem> items = Arrays.asList(
				new DatasetItem().setEntityId(files.get(0).getId())
				.setVersionNumber(files.get(0).getVersionNumber()),
				new DatasetItem().setEntityId(files.get(1).getId())
				.setVersionNumber(files.get(1).getVersionNumber()));
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(files.get(0).getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});
		List<DatasetItem> toAdd = nodeDao.getDatasetItems(KeyFactory.stringToKey(dataset.getId()));
		// call under test
		Long count = downloadListDao.addDatasetItemsToDownloadList(userOneIdLong, toAdd, useVersion, limit);
		assertEquals(2L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddDatasetItemsToDownloadListWithUseVersionFalse() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		boolean useVersion = false;
		// add the first 2 of 3 files to the dataset
		List<DatasetItem> items = Arrays.asList(
				new DatasetItem().setEntityId(files.get(0).getId())
				.setVersionNumber(files.get(0).getVersionNumber()),
				new DatasetItem().setEntityId(files.get(1).getId())
				.setVersionNumber(files.get(1).getVersionNumber()));
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(files.get(0).getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});
		List<DatasetItem> toAdd = nodeDao.getDatasetItems(KeyFactory.stringToKey(dataset.getId()));
		// call under test
		Long count = downloadListDao.addDatasetItemsToDownloadList(userOneIdLong, toAdd, useVersion, limit);
		assertEquals(2L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(null),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(null)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddDatasetItemsToDownloadListWithLimit() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 3;
		// 1 as limit, when dataset contains 2 items
		limit = 1;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(3, files.size());
		boolean useVersion = true;
		// add the first 2 of 3 files to the dataset
		List<DatasetItem> items = Arrays.asList(
				new DatasetItem().setEntityId(files.get(0).getId())
				.setVersionNumber(files.get(0).getVersionNumber()),
				new DatasetItem().setEntityId(files.get(1).getId())
				.setVersionNumber(files.get(1).getVersionNumber()));
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(files.get(0).getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});
		List<DatasetItem> toAdd = nodeDao.getDatasetItems(KeyFactory.stringToKey(dataset.getId()));
		// call under test
		Long count = downloadListDao.addDatasetItemsToDownloadList(userOneIdLong, toAdd, useVersion, limit);
		assertEquals(1L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddDatasetItemsToDownloadListWithFilesAlreadyOnListSameVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 2;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(2, files.size());
		// add the 2nd file to the list with version 2
		List<DownloadListItem> alreadyOnList = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L)
		);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, alreadyOnList);
		boolean useVersion = true;
		// add all files (2) to the dataset
		List<DatasetItem> items = Arrays.asList(
				new DatasetItem().setEntityId(files.get(0).getId())
				.setVersionNumber(files.get(0).getVersionNumber()),
				new DatasetItem().setEntityId(files.get(1).getId())
				.setVersionNumber(files.get(1).getVersionNumber()));
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(files.get(0).getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});
		List<DatasetItem> toAdd = nodeDao.getDatasetItems(KeyFactory.stringToKey(dataset.getId()));
		// call under test
		Long count = downloadListDao.addDatasetItemsToDownloadList(userOneIdLong, toAdd, useVersion, limit);
		// only adds 1 now because the other file was already there
		assertEquals(1L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testAddDatasetItemsToDownloadListWithFilesAlreadyOnListDifferentVersion() {
		int numberOfProject = 1;
		int foldersPerProject = 1;
		int filesPerFolder = 2;
		List<Node> files = createFileHierarchy(numberOfProject, foldersPerProject, filesPerFolder);
		assertEquals(2, files.size());
		// add the 2nd file to the list with version 1
		List<DownloadListItem> alreadyOnList = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(1L)
		);
		downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, alreadyOnList);
		boolean useVersion = true;
		// add all files (2) to the dataset
		List<DatasetItem> items = Arrays.asList(
				new DatasetItem().setEntityId(files.get(0).getId())
				.setVersionNumber(files.get(0).getVersionNumber()),
				new DatasetItem().setEntityId(files.get(1).getId())
				.setVersionNumber(files.get(1).getVersionNumber()));
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(files.get(0).getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});
		List<DatasetItem> toAdd = nodeDao.getDatasetItems(KeyFactory.stringToKey(dataset.getId()));
		// call under test
		Long count = downloadListDao.addDatasetItemsToDownloadList(userOneIdLong, toAdd, useVersion, limit);
		assertEquals(2L, count);
		
		List<DownloadListItem> expected = Arrays.asList(
				new DownloadListItem().setFileEntityId(files.get(0).getId()).setVersionNumber(2L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(1L),
				new DownloadListItem().setFileEntityId(files.get(1).getId()).setVersionNumber(2L)
		);
		compareIdAndVersionToListItem(userOneIdLong, expected,
				downloadListDao.getDBODownloadListItems(userOneIdLong));
	}
	
	@Test
	public void testGetItemManifestDetailsWithVersionNumber() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 1));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});
		// v1
		FileHandle fh1 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(101L);
			h.setFileName("one.txt");
		});
		Node file = nodeDaoHelper.create(n -> {
			n.setName("one.txt");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setFileHandleId(fh1.getId());
			n.setVersionNumber(1L);
			n.setVersionComment("v1");
			n.setVersionLabel("v1");
		});
		Annotations annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("one", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("v1")));
		nodeDao.updateUserAnnotations(file.getId(), annotations);
		// v2
		FileHandle fh2 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(202L);
			h.setFileName("two.txt");
		});
		file.setFileHandleId(fh2.getId());
		file.setName("two.txt");
		file.setVersionComment("v2");
		file.setVersionLabel("v2");
		file.setVersionNumber(2L);
		nodeDao.createNewVersion(file);
		annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("one", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("v2")));
		nodeDao.updateUserAnnotations(file.getId(), annotations);
		
		// call under test
		JSONObject object = downloadListDao.getItemManifestDetails(new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(1L));
		
		JSONObject expected = new JSONObject();
		expected.put(ManifestKeys.ID.name(), file.getId());
		expected.put(ManifestKeys.name.name(), file.getName());
		expected.put(ManifestKeys.versionNumber.name(), 1L);
		expected.put(ManifestKeys.contentType.name(), "text/plain; charset=UTF-8");
		expected.put(ManifestKeys.dataFileSizeBytes.name(), "101");
		expected.put(ManifestKeys.createdBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.createdOn.name(), ""+file.getCreatedOn().getTime());
		expected.put(ManifestKeys.modifiedBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.modifiedOn.name(), ""+file.getModifiedOn().getTime());
		expected.put(ManifestKeys.parentId.name(), file.getParentId());
		expected.put(ManifestKeys.synapseURL.name(), "https://www.synapse.org/#!Synapse:"+file.getId()+".1");
		expected.put(ManifestKeys.dataFileMD5Hex.name(), "md5");
		expected.put("one", "v1");
		assertJSONEquals(expected, object);
	}
	
	@Test
	public void testGetItemManifestDetailsWithNullVersionNumber() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 1));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});
		// v1
		FileHandle fh1 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(101L);
			h.setFileName("one.txt");
		});
		Node file = nodeDaoHelper.create(n -> {
			n.setName("one.txt");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setFileHandleId(fh1.getId());
			n.setVersionNumber(1L);
			n.setVersionComment("v1");
			n.setVersionLabel("v1");
		});
		Annotations annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("one", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("v1")));
		nodeDao.updateUserAnnotations(file.getId(), annotations);
		// v2
		FileHandle fh2 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(202L);
			h.setFileName("two.txt");
		});
		file.setFileHandleId(fh2.getId());
		file.setName("two.txt");
		file.setVersionComment("v2");
		file.setVersionLabel("v2");
		file.setVersionNumber(2L);
		nodeDao.createNewVersion(file);
		annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("one", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("v2")));
		nodeDao.updateUserAnnotations(file.getId(), annotations);
		
		// call under test
		JSONObject object = downloadListDao.getItemManifestDetails(new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(null));
		
		JSONObject expected = new JSONObject();
		expected.put(ManifestKeys.ID.name(), file.getId());
		expected.put(ManifestKeys.name.name(), file.getName());
		expected.put(ManifestKeys.versionNumber.name(), 2L);
		expected.put(ManifestKeys.contentType.name(), "text/plain; charset=UTF-8");
		expected.put(ManifestKeys.dataFileSizeBytes.name(), "202");
		expected.put(ManifestKeys.createdBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.createdOn.name(), ""+file.getCreatedOn().getTime());
		expected.put(ManifestKeys.modifiedBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.modifiedOn.name(), ""+file.getModifiedOn().getTime());
		expected.put(ManifestKeys.parentId.name(), file.getParentId());
		expected.put(ManifestKeys.synapseURL.name(), "https://www.synapse.org/#!Synapse:"+file.getId()+".2");
		expected.put(ManifestKeys.dataFileMD5Hex.name(), "md5");
		expected.put("one", "v2");
		assertJSONEquals(expected, object);
	}
	
	
	@Test
	public void testGetItemManifestDetailsWithEachAnnotationType() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 1));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});
		// v1
		FileHandle fh1 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(101L);
			h.setFileName("one.txt");
		});
		Node file = nodeDaoHelper.create(n -> {
			n.setName("one.txt");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setFileHandleId(fh1.getId());
			n.setVersionNumber(1L);
			n.setVersionComment("v1");
			n.setVersionLabel("v1");
		});
		Annotations annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put("string", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("v1")));
		annotations.getAnnotations().put("boolean", new AnnotationsValue().setType(BOOLEAN).setValue(Arrays.asList("false")));
		annotations.getAnnotations().put("double", new AnnotationsValue().setType(DOUBLE).setValue(Arrays.asList("1.3")));
		annotations.getAnnotations().put("long", new AnnotationsValue().setType(LONG).setValue(Arrays.asList("987654")));
		annotations.getAnnotations().put("timestamp", new AnnotationsValue().setType(TIMESTAMP_MS).setValue(Arrays.asList("2222")));
		annotations.getAnnotations().put("strings", new AnnotationsValue().setType(STRING).setValue(Arrays.asList("a","b","c")));
		annotations.getAnnotations().put("booleans", new AnnotationsValue().setType(BOOLEAN).setValue(Arrays.asList("true","false")));
		annotations.getAnnotations().put("doubles", new AnnotationsValue().setType(BOOLEAN).setValue(Arrays.asList("1.1","1.2")));
		annotations.getAnnotations().put("longs", new AnnotationsValue().setType(BOOLEAN).setValue(Arrays.asList("111","222")));
		annotations.getAnnotations().put("timestamps", new AnnotationsValue().setType(BOOLEAN).setValue(Arrays.asList("44","55")));
		nodeDao.updateUserAnnotations(file.getId(), annotations);
		
		// call under test
		JSONObject object = downloadListDao.getItemManifestDetails(new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(1L));
		
		JSONObject expected = new JSONObject();
		expected.put(ManifestKeys.ID.name(), file.getId());
		expected.put(ManifestKeys.name.name(), file.getName());
		expected.put(ManifestKeys.versionNumber.name(), 1L);
		expected.put(ManifestKeys.contentType.name(), "text/plain; charset=UTF-8");
		expected.put(ManifestKeys.dataFileSizeBytes.name(), "101");
		expected.put(ManifestKeys.createdBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.createdOn.name(), ""+file.getCreatedOn().getTime());
		expected.put(ManifestKeys.modifiedBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.modifiedOn.name(), ""+file.getModifiedOn().getTime());
		expected.put(ManifestKeys.parentId.name(), file.getParentId());
		expected.put(ManifestKeys.synapseURL.name(), "https://www.synapse.org/#!Synapse:"+file.getId()+".1");
		expected.put(ManifestKeys.dataFileMD5Hex.name(), "md5");
		expected.put("string", "v1");
		expected.put("boolean", "false");
		expected.put("double", "1.3");
		expected.put("long", "987654");
		expected.put("timestamp", "2222");
		expected.put("strings", "[a, b, c]");
		expected.put("booleans", "[true, false]");
		expected.put("doubles", "[1.1, 1.2]");
		expected.put("longs", "[111, 222]");
		expected.put("timestamps", "[44, 55]");
		assertJSONEquals(expected, object);
	}
	
	@Test
	public void testGetItemManifestDetailsWithIgnoreDefaultNamedAnnotations() {
		Node project = nodeDaoHelper.create(n -> {
			n.setName(String.join("-", "project", "" + 1));
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(NodeConstants.BOOTSTRAP_NODES.ROOT.getId().toString());
			n.setNodeType(EntityType.project);
		});
		// v1
		FileHandle fh1 = fileHandleObjectHelper.create(h -> {
			h.setContentSize(101L);
			h.setFileName("one.txt");
		});
		Node file = nodeDaoHelper.create(n -> {
			n.setName("one.txt");
			n.setCreatedByPrincipalId(userOneIdLong);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setFileHandleId(fh1.getId());
			n.setVersionNumber(1L);
			n.setVersionComment("v1");
			n.setVersionLabel("v1");
		});
		Annotations annotations = new Annotations().setAnnotations(new LinkedHashMap<>());
		annotations.getAnnotations().put(ManifestKeys.name.name(), new AnnotationsValue().setType(STRING).setValue(Arrays.asList("ignore me")));
		annotations.getAnnotations().put("boolean", new AnnotationsValue().setType(BOOLEAN).setValue(Arrays.asList("false")));
		annotations.getAnnotations().put("nullValue", null);
		annotations.getAnnotations().put("nullType", new AnnotationsValue().setType(null).setValue(Arrays.asList("not")));
		annotations.getAnnotations().put("emptyValue", new AnnotationsValue().setType(STRING).setValue(Collections.emptyList()));
		nodeDao.updateUserAnnotations(file.getId(), annotations);
		
		// call under test
		JSONObject object = downloadListDao.getItemManifestDetails(new DownloadListItem().setFileEntityId(file.getId()).setVersionNumber(1L));
		
		JSONObject expected = new JSONObject();
		expected.put(ManifestKeys.ID.name(), file.getId());
		expected.put(ManifestKeys.name.name(), file.getName());
		expected.put(ManifestKeys.versionNumber.name(), 1L);
		expected.put(ManifestKeys.contentType.name(), "text/plain; charset=UTF-8");
		expected.put(ManifestKeys.dataFileSizeBytes.name(), "101");
		expected.put(ManifestKeys.createdBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.createdOn.name(), ""+file.getCreatedOn().getTime());
		expected.put(ManifestKeys.modifiedBy.name(), userOneIdLong.toString());
		expected.put(ManifestKeys.modifiedOn.name(), ""+file.getModifiedOn().getTime());
		expected.put(ManifestKeys.parentId.name(), file.getParentId());
		expected.put(ManifestKeys.synapseURL.name(), "https://www.synapse.org/#!Synapse:"+file.getId()+".1");
		expected.put(ManifestKeys.dataFileMD5Hex.name(), "md5");
		expected.put("boolean", "false");
		expected.put("nullType", "not");
		assertJSONEquals(expected, object);
	}


	
	/**
	 * Helper to compare two JSON objects.
	 * @param one
	 * @param two
	 */
	public static void assertJSONEquals(JSONObject one, JSONObject two) {
		assertNotNull(one);
		assertNotNull(two);
		assertEquals(one.keySet(), two.keySet());
		for(String key: one.keySet()) {
			assertEquals(one.getString(key), two.getString(key), "key: "+key);
		}
	}
}
