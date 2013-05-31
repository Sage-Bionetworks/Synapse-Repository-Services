package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageDimensionValue;
import org.sagebionetworks.repo.model.storage.StorageUsageSummary;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StorageLocationDAOImplTest {

	@Autowired
	private StorageLocationDAO storageLocationDAO;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private String userId;

	private List<String> toDelete;

	@Before
	public void before(){
		assertNotNull(storageLocationDAO);
		assertNotNull(fileHandleDao);
		assertNotNull(userGroupDAO);
		toDelete = new ArrayList<String>();
		userId = userGroupDAO.findGroup(
				AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);
	}

	@After
	public void after(){
		for(String id: toDelete){
			fileHandleDao.delete(id);
		}
	}

	@Test
	public void testSizeAndCount() throws DatastoreException, NotFoundException{

		// Get baselines
		final int totalSize = storageLocationDAO.getTotalSize().intValue();
		assertTrue(totalSize >=0 );
		final int totalSizeForUser = storageLocationDAO.getTotalSizeForUser(userId).intValue();
		assertTrue(totalSizeForUser >= 0);
		final int totalCount = storageLocationDAO.getTotalCount().intValue();
		assertTrue(totalCount >= 0);
		final int totalCountForUser = storageLocationDAO.getTotalCountForUser(userId).intValue();
		assertTrue(totalCountForUser >= 0);

		// Create the files -- only S3 files count here
		final int size = 50;
		S3FileHandle s3 = TestUtils.createS3FileHandle(userId, size);
		s3 = fileHandleDao.createFile(s3);
		assertNotNull(s3);
		final String s3Id = s3.getId();
		assertNotNull(s3Id);
		toDelete.add(s3Id);

		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(userId);
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		final String previewId = preview.getId();
		assertNotNull(previewId);
		toDelete.add(previewId);

		ExternalFileHandle external = TestUtils.createExternalFileHandle(userId);
		external = fileHandleDao.createFile(external);
		assertNotNull(external);
		final String extId = external.getId();
		assertNotNull(extId);
		toDelete.add(extId);

		assertEquals(totalSize + size, storageLocationDAO.getTotalSize().intValue());
		assertEquals(totalSizeForUser + size, storageLocationDAO.getTotalSizeForUser(userId).intValue());
		assertEquals(totalCount + 1, storageLocationDAO.getTotalCount().intValue());
		assertEquals(totalCountForUser + 1, storageLocationDAO.getTotalCountForUser(userId).intValue());
	}

	@Test
	public void testAggregatedResults() throws DatastoreException, NotFoundException{

		// Create the files -- only S3 files count here
		final int size1 = 10;
		final String contentType1 = "content type 1";
		S3FileHandle s3File1 = TestUtils.createS3FileHandle(userId, size1, contentType1);
		s3File1 = fileHandleDao.createFile(s3File1);
		assertNotNull(s3File1);
		final String s3Id1 = s3File1.getId();
		assertNotNull(s3Id1);
		toDelete.add(s3Id1);

		final int size2 = 30;
		final String contentType2 = "content type 2";
		S3FileHandle s3File2 = TestUtils.createS3FileHandle(userId, size2, contentType2);
		s3File2 = fileHandleDao.createFile(s3File2);
		assertNotNull(s3File2);
		final String s3Id2 = s3File2.getId();
		assertNotNull(s3Id2);
		toDelete.add(s3Id2);

		final int size3 = 50;
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(userId, size3, contentType1);
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		final String previewId = preview.getId();
		assertNotNull(previewId);
		toDelete.add(previewId);

		ExternalFileHandle external = TestUtils.createExternalFileHandle(userId, contentType2);
		external = fileHandleDao.createFile(external);
		assertNotNull(external);
		final String extId = external.getId();
		assertNotNull(extId);
		toDelete.add(extId);

		// Get aggregates on CONTENT_TYPE, STORAGE_PROVIDER
		List<StorageUsageDimension> dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.CONTENT_TYPE);
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		StorageUsageSummaryList results = storageLocationDAO.getAggregatedUsage(dimList);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		List<StorageUsageSummary> aggregates = results.getSummaryList();
		for (StorageUsageSummary aggregate : aggregates) {
			List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
			assertEquals(2, list.size());
			assertEquals(StorageUsageDimension.CONTENT_TYPE, list.get(0).getDimension());
			assertNotNull(list.get(0).getValue());
			assertEquals(StorageUsageDimension.STORAGE_PROVIDER, list.get(1).getDimension());
			assertNotNull(list.get(1).getValue());
			assertEquals(1, aggregate.getAggregatedCount().intValue());
			assertTrue(aggregate.getAggregatedSize().intValue() >= 0);
		}

		// Reverse the order of aggregating dimensions plus dupes
		dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		dimList.add(StorageUsageDimension.CONTENT_TYPE);
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		results = storageLocationDAO.getAggregatedUsage(dimList);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		aggregates = results.getSummaryList();
		for (StorageUsageSummary aggregate : aggregates) {
			List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
			assertEquals(2, list.size());
			assertEquals(StorageUsageDimension.STORAGE_PROVIDER, list.get(0).getDimension());
			assertNotNull(list.get(0).getValue());
			assertEquals(StorageUsageDimension.CONTENT_TYPE, list.get(1).getDimension());
			assertNotNull(list.get(1).getValue());
			assertEquals(1, aggregate.getAggregatedCount().intValue());
			assertTrue(aggregate.getAggregatedSize().intValue() >= 0);
		}

		// One dimension only to verify the aggregated numbers
		dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.USER_ID);
		results = storageLocationDAO.getAggregatedUsage(dimList);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		aggregates = results.getSummaryList();
		for (StorageUsageSummary aggregate : aggregates) {
			List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
			assertEquals(1, list.size());
			assertEquals(StorageUsageDimension.USER_ID, list.get(0).getDimension());
			assertNotNull(list.get(0).getValue());
			assertEquals(4, aggregate.getAggregatedCount().intValue());
			assertEquals(size1 + size2 + size3, aggregate.getAggregatedSize().intValue());
		}

		// Get aggregated results for user
		dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		dimList.add(StorageUsageDimension.CONTENT_TYPE);
		results = storageLocationDAO.getAggregatedUsageForUser(userId, dimList);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		aggregates = results.getSummaryList();
		for (StorageUsageSummary aggregate : aggregates) {
			List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
			assertEquals(2, list.size());
			assertEquals(StorageUsageDimension.STORAGE_PROVIDER, list.get(0).getDimension());
			assertNotNull(list.get(0).getValue());
			assertEquals(StorageUsageDimension.CONTENT_TYPE, list.get(1).getDimension());
			assertNotNull(list.get(1).getValue());
			assertEquals(1, aggregate.getAggregatedCount().intValue());
			assertTrue(aggregate.getAggregatedSize().intValue() >= 0);
		}
	}
}
