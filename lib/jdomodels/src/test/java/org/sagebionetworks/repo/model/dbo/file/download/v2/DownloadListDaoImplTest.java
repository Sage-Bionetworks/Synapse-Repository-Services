package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	Long userOneIdLong;
	String userOneId;

	Long userTwoIdLong;
	String userTwoId;

	List<DownloadListItem> idsWithVersions;
	List<DownloadListItem> idsWithoutVersions;

	@BeforeEach
	public void before() {
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
	}

	@AfterEach
	public void after() {
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
		String message = assertThrows(IllegalArgumentException.class, ()->{
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
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
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
		String message = assertThrows(IllegalArgumentException.class, ()->{
			Long nullUserId = null;
			// call under test
			downloadListDao.clearDownloadList(nullUserId);
		}).getMessage();
		assertEquals("User Id is required.", message);
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
}
