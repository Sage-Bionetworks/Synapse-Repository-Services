package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.download.IdAndVersion;
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

	List<IdAndVersion> idsWithVersions;
	List<IdAndVersion> idsWithoutVersions;

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
		idsWithVersions = new ArrayList<IdAndVersion>(numberOfIds);
		idsWithoutVersions = new ArrayList<IdAndVersion>(numberOfIds);
		for (int i = 0; i < numberOfIds; i++) {

			IdAndVersion idWithVersion = new IdAndVersion();
			idWithVersion.setEntityId("syn" + i);
			idWithVersion.setVersionNumber(new Long(i));
			idsWithVersions.add(idWithVersion);

			IdAndVersion idWithoutVersion = new IdAndVersion();
			idWithoutVersion.setEntityId("syn" + (i + numberOfIds + 1));
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
		List<IdAndVersion> batch = Lists.newArrayList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		assertThrows(IllegalArgumentException.class, () -> {
			Long nullUserId = null;
			// call under test
			downloadListDao.addBatchOfFilesToDownloadList(nullUserId, batch);
		});
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullEntityId() {
		IdAndVersion id = idsWithVersions.get(0);
		id.setEntityId(null);
		List<IdAndVersion> batch = Lists.newArrayList(id);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		}).getMessage();
		assertEquals("Null entityId at index: 0", message);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithNullBatch() {
		List<IdAndVersion> batch = null;
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(0, addedCount);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithEmptyBatch() {
		List<IdAndVersion> batch = Collections.emptyList();
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(0, addedCount);
	}

	@Test
	public void testAddBatchOfFiles() {
		List<IdAndVersion> batch = Lists.newArrayList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		assertEquals(2, addedCount);
		validateDBODownloadList(userTwoIdLong, downloadListDao.getDBODownloadList(userTwoIdLong));

		List<DBODownloadListItem> items = downloadListDao.getDBODownloadListItems(userOneIdLong);
		compareIdAndVersionToListItem(userOneIdLong, batch, items);
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithUpdate() throws InterruptedException {
		List<IdAndVersion> batch = Lists.newArrayList(idsWithVersions.get(0), idsWithoutVersions.get(0));
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
		Thread.sleep(1000L);
		// call under test
		addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, batch);
		// only one file should be added since the first two in the batch are already on
		// the list.
		assertEquals(1, addedCount);
		DBODownloadList currentDBOList = downloadListDao.getDBODownloadList(userOneIdLong);
		assertEquals(userOneIdLong, currentDBOList.getPrincipalId());
		// the etag must change
		assertNotEquals(startingDBOList.getEtag(), currentDBOList.getEtag());
		// the updated on must change.
		assertNotEquals(startingDBOList.getUpdatedOn(), currentDBOList.getUpdatedOn());
		compareIdAndVersionToListItem(userOneIdLong, batch, downloadListDao.getDBODownloadListItems(userOneIdLong));
	}

	@Test
	public void testAddBatchOfFilesToDownloadListWithMultipleUsers() {
		List<IdAndVersion> userOneBatch = Lists.newArrayList(idsWithVersions.get(0), idsWithoutVersions.get(0));
		// call under test
		long addedCount = downloadListDao.addBatchOfFilesToDownloadList(userOneIdLong, userOneBatch);
		assertEquals(userOneBatch.size(), addedCount);

		List<IdAndVersion> userTwoBatch = Lists.newArrayList(idsWithVersions.get(0), idsWithoutVersions.get(2));
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
	 * Helper to compare a list of IdAndVersion to a list of DBODownloadListItem.
	 * 
	 * @param ids
	 * @param items
	 */
	public static void compareIdAndVersionToListItem(Long principalId, List<IdAndVersion> ids,
			List<DBODownloadListItem> items) {
		assertNotNull(principalId);
		assertNotNull(ids);
		assertNotNull(items);
		assertEquals(ids.size(), items.size());
		for (int i = 0; i < ids.size(); i++) {
			IdAndVersion id = ids.get(i);
			assertNotNull(id);
			DBODownloadListItem item = items.get(i);
			assertNotNull(item);
			assertEquals(KeyFactory.stringToKey(id.getEntityId()), item.getEntityId());
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
