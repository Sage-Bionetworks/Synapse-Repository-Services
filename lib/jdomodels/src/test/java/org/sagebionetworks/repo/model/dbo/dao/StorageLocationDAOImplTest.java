package org.sagebionetworks.repo.model.dbo.dao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageDimensionValue;
import org.sagebionetworks.repo.model.storage.StorageUsageSummary;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StorageLocationDAOImplTest {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private StorageLocationDAO dao;

	private String userId;
	private String nodeId;

	private List<AttachmentData> attachmentList;
	private List<LocationData> locationList;
	private Map<String, List<String>> strAnnotations;
	private StorageLocations locations;

	private final long a1Size = 23L;
	private final long a2Size = 53L;
	private final long l2Size = 11L;
	private final long sizeTotal = a1Size + a2Size + l2Size;
	private final long countTotal = 4L; // a1, a2, l1, l2; l1 has no size (i.e. null)

	@Before
	public void before() throws Exception {

		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		Assert.assertNotNull(userId);

		attachmentList = new ArrayList<AttachmentData>();
		AttachmentData ad = new AttachmentData();
		ad.setName("ad1");
		ad.setTokenId("ad1Token");
		ad.setContentType("ad1Code");
		ad.setMd5("ad1Md5");
		ad.setUrl("ad1Url");
		ad.setPreviewId("ad1Preview");
		attachmentList.add(ad);
		ad = new AttachmentData();
		ad.setName("ad2");
		ad.setTokenId("ad2Token");
		ad.setContentType("ad2Code");
		ad.setMd5("ad2Md5");
		ad.setUrl("ad2Url");
		ad.setPreviewId("ad2Preview");
		attachmentList.add(ad);

		locationList = new ArrayList<LocationData>();
		LocationData ld = new LocationData();
		ld.setPath("ld1Path");
		ld.setType(LocationTypeNames.external);
		locationList.add(ld);
		ld = new LocationData();
		ld.setPath("abc/xyz");
		ld.setType(LocationTypeNames.awss3);
		locationList.add(ld);

		strAnnotations = new HashMap<String, List<String>>();
		List<String> md5List = new ArrayList<String>();
		md5List.add("ldMd5");
		strAnnotations.put("md5", md5List);	
		List<String> ctList = new ArrayList<String>();
		ctList.add("ldContentType");
		strAnnotations.put("contentType", ctList);
	}

	@After
	public void after() throws NotFoundException, DatastoreException {
		if (nodeId != null) {
			nodeDao.delete(nodeId.toString());
		}
		attachmentList = null;
		locationList = null;
		strAnnotations = null;
		locations = null;
	}

	@Test
	public void testReplaceLocationData() throws Exception {
		long base = basicDao.getCount(DBOStorageLocation.class);
		long count = attachmentList.size() + locationList.size();
		addTestNode();
		dao.replaceLocationData(locations);
		// We have inserted 4 rows in this unit test
		Assert.assertEquals(base + count, basicDao.getCount(DBOStorageLocation.class));
		// Repeat should replace the same 4 rows (i.e. shouldn't add another 4 rows)
		dao.replaceLocationData(locations);
		Assert.assertEquals(base + count, basicDao.getCount(DBOStorageLocation.class));
		removeTestNode();
		Assert.assertEquals(base, basicDao.getCount(DBOStorageLocation.class));
	}

	@Test
	public void testGetTotalSize() throws Exception {
		long base = dao.getTotalSize();
		addTestNode();
		dao.replaceLocationData(locations);
		Long total = dao.getTotalSize();
		// Except for the ones we are mocking here, no other
		// storage item has size in the test database
		Assert.assertEquals(base + sizeTotal, total.longValue());
		removeTestNode();
	}

	@Test
	public void testGetTotalSizeForUser() throws Exception {
		long base = dao.getTotalSizeForUser(userId);
		addTestNode();
		dao.replaceLocationData(locations);
		Long total = dao.getTotalSizeForUser(userId);
		Assert.assertEquals(base + sizeTotal, total.longValue());
		total = dao.getTotalSizeForUser("syn9293829999990"); // fake user
		Assert.assertEquals(0L, total.longValue());
		removeTestNode();
	}

	@Test
	public void testGetTotalCount() throws Exception {
		long base = dao.getTotalCount();
		addTestNode();
		dao.replaceLocationData(locations);
		// There are other storage items besides the ones mocked here
		Assert.assertEquals(base + countTotal, dao.getTotalCount().longValue());
		removeTestNode();
	}

	@Test
	public void testGetTotalCountForUser() throws Exception {
		addTestNode();
		dao.replaceLocationData(locations);
		Assert.assertEquals(countTotal, dao.getTotalCountForUser(userId).longValue());
		Assert.assertEquals(0L, dao.getTotalCountForUser("syn9293829999990").longValue()); // fake user
		removeTestNode();
	}

	@Test
	public void testGetTotalCountForNode() throws Exception {
		addTestNode();
		dao.replaceLocationData(locations);
		Assert.assertEquals(countTotal, dao.getTotalCountForNode(nodeId).longValue());
		Assert.assertEquals(0L, dao.getTotalCountForNode("syn9293829999990").longValue()); // fake node
		removeTestNode();
	}

	@Test
	public void testGetAggregatedUsage() throws Exception {

		addTestNode();
		dao.replaceLocationData(locations);

		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		StorageUsageSummaryList susList = dao.getAggregatedUsage(dList);
		// Except for the ones we are mocking here, no other
		// storage item has size in the test database
		Assert.assertEquals(sizeTotal, susList.getTotalSize().longValue());
		// There are other storage items (owned by other users) besides the ones mocked here
		Assert.assertTrue(susList.getTotalCount().longValue() >= countTotal);
		Assert.assertEquals(0, susList.getSummaryList().size());

		dList.add(StorageUsageDimension.STORAGE_PROVIDER);
		dList.add(StorageUsageDimension.IS_ATTACHMENT);
		dList.add(StorageUsageDimension.STORAGE_PROVIDER);
		dList.add(StorageUsageDimension.IS_ATTACHMENT);

		susList = dao.getAggregatedUsage(dList);
		// Except for the ones we are mocking here, no other
		// storage item has size in the test database
		Assert.assertEquals(sizeTotal, susList.getTotalSize().longValue());
		// There are other storage items (owned by other users) besides the ones mocked here
		Assert.assertTrue(susList.getTotalCount().longValue() >= countTotal);
		List<StorageUsageSummary> summaryList = susList.getSummaryList();
		//
		// Currently aggregated into 3 rows:
		//
		// STORAGE_PROVIDER | IS_ATTACHMENT
		// =================================
		//      awss3            false
		//      awss3            true
		//      external         false
		//
		Assert.assertEquals(3, summaryList.size());
		long sumOfSize = 0;
		long sumOfCount = 0;
		for (StorageUsageSummary summary : summaryList) {
			List<StorageUsageDimensionValue> dvList = summary.getDimensionList();;
			Assert.assertEquals(2, dvList.size());
			// We should maintain the original aggregating order
			Assert.assertEquals(StorageUsageDimension.STORAGE_PROVIDER, dvList.get(0).getDimension());
			Assert.assertEquals(StorageUsageDimension.IS_ATTACHMENT, dvList.get(1).getDimension());
			sumOfSize = sumOfSize + summary.getAggregatedSize();
			sumOfCount = sumOfCount + summary.getAggregatedCount();
		}
		// Except for the ones we are mocking here, no other
		// storage item has size in the test database
		Assert.assertEquals(sizeTotal, sumOfSize);
		Assert.assertEquals(susList.getTotalCount().longValue(), sumOfCount);

		removeTestNode();
	}

	@Test
	public void testGetAggregatedUsageForUser() throws Exception {

		addTestNode();
		dao.replaceLocationData(locations);

		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		StorageUsageSummaryList susList = dao.getAggregatedUsageForUser(userId, dList);
		Assert.assertEquals(sizeTotal, susList.getTotalSize().longValue());
		Assert.assertEquals(countTotal, susList.getTotalCount().longValue());
		Assert.assertEquals(0, susList.getSummaryList().size());

		dList.add(StorageUsageDimension.IS_ATTACHMENT);
		dList.add(StorageUsageDimension.STORAGE_PROVIDER);

		susList = dao.getAggregatedUsageForUser(userId, dList);
		Assert.assertEquals(sizeTotal, susList.getTotalSize().longValue());
		Assert.assertEquals(countTotal, susList.getTotalCount().longValue());
		List<StorageUsageSummary> summaryList = susList.getSummaryList();
		//
		// Currently aggregated into 3 rows:
		//
		// STORAGE_PROVIDER | IS_ATTACHMENT
		// =================================
		//      awss3            false
		//      awss3            true
		//      external         false
		//
		Assert.assertEquals(3, summaryList.size());
		long sumOfSize = 0L;
		long sumOfCount = 0L;
		for (StorageUsageSummary summary : summaryList) {
			List<StorageUsageDimensionValue> dvList = summary.getDimensionList();
			Assert.assertEquals(2, dvList.size());
			Assert.assertEquals(StorageUsageDimension.IS_ATTACHMENT, dvList.get(0).getDimension());
			Assert.assertEquals(StorageUsageDimension.STORAGE_PROVIDER, dvList.get(1).getDimension());
			sumOfSize = sumOfSize + summary.getAggregatedSize();
			sumOfCount = sumOfCount + summary.getAggregatedCount();
		}
		Assert.assertEquals(sizeTotal, sumOfSize);
		Assert.assertEquals(countTotal, sumOfCount);

		removeTestNode();
	}

	@Test
	public void testGetStorageUsageInRangeForUser() throws Exception {

		addTestNode();
		dao.replaceLocationData(locations);

		int beginIncl = 0;
		int endExcl = 1000;
		List<StorageUsage> suList = dao.getUsageInRangeForUser(userId, beginIncl, endExcl);

		Assert.assertEquals(4, suList.size());
		Map<String, StorageUsage> suMap = new HashMap<String, StorageUsage>();
		for (StorageUsage su : suList) {
			suMap.put(su.getLocation(), su);
		}
		Assert.assertEquals(4, suMap.size());

		StorageUsage su = suMap.get("/" + KeyFactory.stringToKey(nodeId) + "/ad1Token");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertTrue(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("/" + KeyFactory.stringToKey(nodeId) + "/ad1Token", su.getLocation());
		Assert.assertEquals("ad1Code", su.getContentType());
		Assert.assertEquals(a1Size, su.getContentSize().longValue());
		Assert.assertEquals("ad1Md5", su.getContentMd5());

		su = suMap.get("/" + KeyFactory.stringToKey(nodeId) + "/ad2Token");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertTrue(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("/" + KeyFactory.stringToKey(nodeId) + "/ad2Token", su.getLocation());
		Assert.assertEquals("ad2Code", su.getContentType());
		Assert.assertEquals(a2Size, su.getContentSize().longValue());
		Assert.assertEquals("ad2Md5", su.getContentMd5());

		su = suMap.get("ld1Path");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertFalse(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.external, su.getStorageProvider());
		Assert.assertEquals("ld1Path", su.getLocation());
		Assert.assertEquals("ldContentType", su.getContentType());
		Assert.assertEquals(0L, su.getContentSize().longValue());
		Assert.assertEquals("ldMd5", su.getContentMd5());

		su = suMap.get("abc/xyz");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertFalse(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("abc/xyz", su.getLocation());
		Assert.assertEquals("ldContentType", su.getContentType());
		Assert.assertEquals(l2Size, su.getContentSize().longValue());
		Assert.assertEquals("ldMd5", su.getContentMd5());

		beginIncl = 1;
		endExcl = 3;
		suList = dao.getUsageInRangeForUser(userId, beginIncl, endExcl);
		Assert.assertEquals(2, suList.size());

		removeTestNode();
	}

	@Test
	public void testGetStorageUsageInRangeForNode() throws Exception {

		addTestNode();
		dao.replaceLocationData(locations);

		int beginIncl = 0;
		int endExcl = 1000;
		List<StorageUsage> suList = dao.getUsageInRangeForNode(nodeId, beginIncl, endExcl);

		Assert.assertEquals(4, suList.size());
		Map<String, StorageUsage> suMap = new HashMap<String, StorageUsage>();
		for (StorageUsage su : suList) {
			suMap.put(su.getLocation(), su);
		}
		Assert.assertEquals(4, suMap.size());

		StorageUsage su = suMap.get("/" + KeyFactory.stringToKey(nodeId) + "/ad1Token");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertTrue(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("/" + KeyFactory.stringToKey(nodeId) + "/ad1Token", su.getLocation());
		Assert.assertEquals("ad1Code", su.getContentType());
		Assert.assertEquals(a1Size, su.getContentSize().longValue());
		Assert.assertEquals("ad1Md5", su.getContentMd5());

		su = suMap.get("/" + KeyFactory.stringToKey(nodeId) + "/ad2Token");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertTrue(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("/" + KeyFactory.stringToKey(nodeId) + "/ad2Token", su.getLocation());
		Assert.assertEquals("ad2Code", su.getContentType());
		Assert.assertEquals(a2Size, su.getContentSize().longValue());
		Assert.assertEquals("ad2Md5", su.getContentMd5());

		su = suMap.get("ld1Path");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertFalse(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.external, su.getStorageProvider());
		Assert.assertEquals("ld1Path", su.getLocation());
		Assert.assertEquals("ldContentType", su.getContentType());
		Assert.assertEquals(0L, su.getContentSize().longValue());
		Assert.assertEquals("ldMd5", su.getContentMd5());

		su = suMap.get("abc/xyz");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertFalse(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("abc/xyz", su.getLocation());
		Assert.assertEquals("ldContentType", su.getContentType());
		Assert.assertEquals(l2Size, su.getContentSize().longValue());
		Assert.assertEquals("ldMd5", su.getContentMd5());

		beginIncl = 1;
		endExcl = 3;
		suList = dao.getUsageInRangeForNode(nodeId, beginIncl, endExcl);
		Assert.assertEquals(2, suList.size());

		removeTestNode();
	}

	@Test
	public void testGetAggregatedUsageByUserInRange() throws Exception {

		addTestNode();
		dao.replaceLocationData(locations);

		int beginIncl = 0;
		int endExcl = 1000;
		StorageUsageSummaryList summaryList = dao.getAggregatedUsageByUserInRange(beginIncl, endExcl);

		Assert.assertEquals(sizeTotal, summaryList.getTotalSize().longValue());
		Assert.assertTrue(summaryList.getTotalCount().longValue() >= countTotal);

		List<StorageUsageSummary> summaries = summaryList.getSummaryList();
		long sumOfSize = 0L;
		long sumOfCount = 0L;
		for (StorageUsageSummary sus : summaries) {
			Assert.assertEquals(1, sus.getDimensionList().size());
			StorageUsageDimensionValue sudv = sus.getDimensionList().get(0);
			Assert.assertEquals(StorageUsageDimension.USER_ID, sudv.getDimension());
			Assert.assertNotNull(sudv.getValue());
			sumOfSize = sumOfSize + sus.getAggregatedSize();
			sumOfCount = sumOfCount + sus.getAggregatedCount();
		}
		Assert.assertEquals(sizeTotal, sumOfSize);
		Assert.assertEquals(summaryList.getTotalCount().longValue(), sumOfCount);

		removeTestNode();
	}

	@Test
	public void testGetAggregatedUsageByNodeInRange() throws Exception {

		addTestNode();
		dao.replaceLocationData(locations);

		int beginIncl = 0;
		int endExcl = 1000;
		StorageUsageSummaryList summaryList = dao.getAggregatedUsageByNodeInRange(beginIncl, endExcl);

		Assert.assertEquals(sizeTotal, summaryList.getTotalSize().longValue());
		Assert.assertTrue(summaryList.getTotalCount().longValue() >= countTotal);

		List<StorageUsageSummary> summaries = summaryList.getSummaryList();
		long sumOfSize = 0L;
		long sumOfCount = 0L;
		for (StorageUsageSummary sus : summaries) {
			Assert.assertEquals(1, sus.getDimensionList().size());
			StorageUsageDimensionValue sudv = sus.getDimensionList().get(0);
			Assert.assertEquals(StorageUsageDimension.NODE_ID, sudv.getDimension());
			Assert.assertNotNull(sudv.getValue());
			sumOfSize = sumOfSize + sus.getAggregatedSize();
			sumOfCount = sumOfCount + sus.getAggregatedCount();
		}
		Assert.assertEquals(sizeTotal, sumOfSize);
		Assert.assertEquals(summaryList.getTotalCount().longValue(), sumOfCount);

		removeTestNode();
	}

	// Inserts a new test node so that storage location data can be associated with this node
	private void addTestNode() throws Exception {

		// Create the node
		Long userIdLong = Long.parseLong(userId);
		Assert.assertNotNull(userIdLong);
		Node node = NodeTestUtils.createNew("A test node for location data", userIdLong);
		Assert.assertNotNull(node);
		String nodeId = nodeDao.createNew(node);
		Assert.assertNotNull(nodeId);
		this.nodeId = nodeId;

		// Clear any location data associated with the node
		dao.deleteLocationDataByOwnerId(KeyFactory.stringToKey(nodeId));

		// Create the location data
		locations = new StorageLocations(KeyFactory.stringToKey(nodeId), userIdLong,
				attachmentList, locationList, strAnnotations);

		// Mock a new S3 client
		List<S3ObjectSummary> objList = new ArrayList<S3ObjectSummary>();
		S3ObjectSummary objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn(KeyFactory.stringToKey(nodeId) + "/ad1Token");
		when(objSummary.getSize()).thenReturn(a1Size);
		objList.add(objSummary);
		objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn(KeyFactory.stringToKey(nodeId) + "/ad2Token");
		when(objSummary.getSize()).thenReturn(a2Size);
		objList.add(objSummary);
		objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn("abc/xyz");
		when(objSummary.getSize()).thenReturn(l2Size);
		objList.add(objSummary);
		ObjectListing objListing = mock(ObjectListing.class);
		when(objListing.getObjectSummaries()).thenReturn(objList);
		when(objListing.isTruncated()).thenReturn(false);

		AmazonS3 s3Client = Mockito.mock(AmazonS3.class);
		String bucket = StackConfiguration.getS3Bucket();
		when(s3Client.listObjects(bucket, KeyFactory.stringToKey(nodeId) + "/")).thenReturn(objListing);
		ReflectionTestUtils.setField(unwrap(), "amazonS3Client", s3Client);
	}

	// Removes the test node. This should also remove any storage location associated with it
	private void removeTestNode() throws Exception {
		// Delete the node
		boolean success = nodeDao.delete(nodeId.toString());
		Assert.assertTrue(success);
		// Clear any location data associated with the node
		dao.deleteLocationDataByOwnerId(KeyFactory.stringToKey(nodeId));
	}

	private StorageLocationDAO unwrap() throws Exception {
		if(AopUtils.isAopProxy(dao) && dao instanceof Advised) {
			Object target = ((Advised)dao).getTargetSource().getTarget();
			return (StorageLocationDAOImpl)target;
		}
		return dao;
	}
}
