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
import org.sagebionetworks.repo.model.StorageUsageQueryDao;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageDimensionValue;
import org.sagebionetworks.repo.model.storage.StorageUsageSummary;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StorageUsageQueryDaoImplTest {

	@Autowired
	private StorageUsageQueryDao storageUsageQueryDao;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private String userId;

	private List<String> toDelete;

	@Before
	public void before(){

		assertNotNull(storageUsageQueryDao);
		assertNotNull(fileHandleDao);
		assertNotNull(userGroupDAO);

		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);

		toDelete = new ArrayList<String>();
	}

	@After
	public void after(){
		for(String id: toDelete){
			fileHandleDao.delete(id);
		}
	}

	@Test
	public void testGetSizeAndGetCount() throws Exception {

		// Get baselines
		final int totalSize = storageUsageQueryDao.getTotalSize().intValue();
		assertTrue(totalSize >=0 );
		final int totalSizeForUser = storageUsageQueryDao.getTotalSizeForUser(userId).intValue();
		assertTrue(totalSizeForUser >= 0);
		final int totalCount = storageUsageQueryDao.getTotalCount().intValue();
		assertTrue(totalCount >= 0);
		final int totalCountForUser = storageUsageQueryDao.getTotalCountForUser(userId).intValue();
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

		assertEquals(totalSize + size, storageUsageQueryDao.getTotalSize().intValue());
		assertEquals(totalSizeForUser + size, storageUsageQueryDao.getTotalSizeForUser(userId).intValue());
		assertEquals(totalCount + 1, storageUsageQueryDao.getTotalCount().intValue());
		assertEquals(totalCountForUser + 1, storageUsageQueryDao.getTotalCountForUser(userId).intValue());
	}

	@Test
	public void testAggregatedResults() throws Exception {

		// Create the files
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

		ExternalFileHandle external = TestUtils.createExternalFileHandle(userId, "content type");
		external = fileHandleDao.createFile(external);
		assertNotNull(external);
		final String extId = external.getId();
		assertNotNull(extId);
		toDelete.add(extId);

		// Get aggregates on CONTENT_TYPE, STORAGE_PROVIDER
		List<StorageUsageDimension> dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.CONTENT_TYPE);
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		StorageUsageSummaryList results = storageUsageQueryDao.getAggregatedUsage(dimList);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		List<StorageUsageSummary> aggregates = results.getSummaryList();
		int contentType1Count = 0;
		int contentType1Size = 0;
		int contentType2Count = 0;
		int contentType2Size = 0;
		for (StorageUsageSummary aggregate : aggregates) {
			List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
			assertEquals(2, list.size());
			StorageUsageDimensionValue dim = list.get(0);
			assertEquals(StorageUsageDimension.CONTENT_TYPE, dim.getDimension());
			String contentType = dim.getValue();
			if (contentType1.equals(contentType)) {
				contentType1Count = contentType1Count + aggregate.getAggregatedCount().intValue();
				contentType1Size = contentType1Size + aggregate.getAggregatedSize().intValue();
			} else if (contentType2.equals(contentType)) {
				contentType2Count = contentType2Count + aggregate.getAggregatedCount().intValue();
				contentType2Size = contentType2Size + aggregate.getAggregatedSize().intValue();
			}
			dim = list.get(1);
			assertEquals(StorageUsageDimension.STORAGE_PROVIDER, dim.getDimension());
			String provider = dim.getValue();
			if (contentType1.equals(contentType) && "S3".equals(provider)) {
				assertEquals(1, aggregate.getAggregatedCount().intValue());
				assertEquals(10, aggregate.getAggregatedSize().intValue());
			}
		}
		assertEquals(2, contentType1Count);
		assertEquals(60, contentType1Size);
		assertEquals(1, contentType2Count);
		assertEquals(30, contentType2Size);

		// Reverse the order of aggregating dimensions plus dupes
		dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		dimList.add(StorageUsageDimension.CONTENT_TYPE);
		dimList.add(StorageUsageDimension.STORAGE_PROVIDER);
		results = storageUsageQueryDao.getAggregatedUsage(dimList);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		contentType1Count = 0;
		contentType1Size = 0;
		contentType2Count = 0;
		contentType2Size = 0;
		aggregates = results.getSummaryList();
		for (StorageUsageSummary aggregate : aggregates) {
			List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
			assertEquals(2, list.size());
			assertEquals(StorageUsageDimension.STORAGE_PROVIDER, list.get(0).getDimension());
			assertNotNull(list.get(0).getValue());
			assertEquals(StorageUsageDimension.CONTENT_TYPE, list.get(1).getDimension());
			assertNotNull(list.get(1).getValue());
			String contentType = list.get(1).getValue();
			if (contentType1.equals(contentType)) {
				contentType1Count = contentType1Count + aggregate.getAggregatedCount().intValue();
				contentType1Size = contentType1Size + aggregate.getAggregatedSize().intValue();
			} else if (contentType2.equals(contentType)) {
				contentType2Count = contentType2Count + aggregate.getAggregatedCount().intValue();
				contentType2Size = contentType2Size + aggregate.getAggregatedSize().intValue();
			}
			assertEquals(1, aggregate.getAggregatedCount().intValue());
			assertTrue(aggregate.getAggregatedSize().intValue() >= 0);
		}
		assertEquals(2, contentType1Count);
		assertEquals(60, contentType1Size);
		assertEquals(1, contentType2Count);
		assertEquals(30, contentType2Size);

		// One dimension only to verify the aggregated numbers
		dimList = new ArrayList<StorageUsageDimension>();
		dimList.add(StorageUsageDimension.USER_ID);
		results = storageUsageQueryDao.getAggregatedUsage(dimList);
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
		results = storageUsageQueryDao.getAggregatedUsageForUser(userId, dimList);
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

		// Get aggregated usage user -- paginated
		results = storageUsageQueryDao.getAggregatedUsageByUserInRange(0, 1);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		aggregates = results.getSummaryList();
		assertEquals(1, aggregates.size());
		StorageUsageSummary aggregate = aggregates.get(0);
		List<StorageUsageDimensionValue> list = aggregate.getDimensionList();
		assertEquals(1, list.size());
		assertEquals(StorageUsageDimension.USER_ID, list.get(0).getDimension());
		assertEquals(4, aggregate.getAggregatedCount().intValue());
		assertEquals(size1 + size2 + size3, aggregate.getAggregatedSize().intValue());
		results = storageUsageQueryDao.getAggregatedUsageByUserInRange(1, 100);
		assertEquals(4, results.getTotalCount().intValue());
		assertEquals(size1 + size2 + size3, results.getTotalSize().intValue());
		aggregates = results.getSummaryList();
		assertEquals(0, aggregates.size()); // Out-of-range; we only have one user
	}
	
	@Test
	public void testGetItemizedStorageForUser() {

		// Set up the files
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

		// Test
		List<StorageUsage> results = storageUsageQueryDao.getUsageInRangeForUser(userId, 0, 100);
		assertNotNull(results);
		assertEquals(4, results.size());
		StorageUsage su = results.get(0);
		assertNotNull(su.getId());
		assertNotNull(su.getName());
		assertNotNull(su.getStorageProvider());
		assertNotNull(su.getLocation());
		assertNotNull(su.getUserId());
		assertNotNull(su.getCreatedOn());
		assertNotNull(su.getContentMd5());
		assertNotNull(su.getContentSize());
		assertNotNull(su.getContentType());
	}
}
