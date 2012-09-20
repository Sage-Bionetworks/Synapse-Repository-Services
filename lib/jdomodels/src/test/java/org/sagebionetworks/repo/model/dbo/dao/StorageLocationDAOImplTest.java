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
import org.sagebionetworks.repo.model.storage.StorageUsageList;
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

	private StorageLocations locations;

	@Before
	public void before() throws Exception {

		userId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		Assert.assertNotNull(userId);
		Long userIdLong = Long.parseLong(userId);
		Node node = NodeTestUtils.createNew("test node for location data", userIdLong);
		Assert.assertNotNull(node);
		nodeId = nodeDao.createNew(node);
		Assert.assertNotNull(nodeId);

		List<AttachmentData> attachmentList = new ArrayList<AttachmentData>();
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

		List<LocationData> locationList = new ArrayList<LocationData>();
		LocationData ld = new LocationData();
		ld.setPath("ld1Path");
		ld.setType(LocationTypeNames.external);
		locationList.add(ld);
		ld = new LocationData();
		ld.setPath("abc/xyz");
		ld.setType(LocationTypeNames.awss3);
		locationList.add(ld);

		Map<String, List<String>> strAnnotations = new HashMap<String, List<String>>();
		List<String> md5List = new ArrayList<String>();
		md5List.add("ldMd5");
		strAnnotations.put("md5", md5List);
	
		List<String> ctList = new ArrayList<String>();
		ctList.add("ldContentType");
		strAnnotations.put("contentType", ctList);

		locations = new StorageLocations(KeyFactory.stringToKey(nodeId), userIdLong,
			attachmentList, locationList, strAnnotations);

		List<S3ObjectSummary> objList = new ArrayList<S3ObjectSummary>();
		S3ObjectSummary objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn(KeyFactory.stringToKey(nodeId) + "/ad1Token");
		when(objSummary.getSize()).thenReturn(23L);
		objList.add(objSummary);
		objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn(KeyFactory.stringToKey(nodeId) + "/ad2Token");
		when(objSummary.getSize()).thenReturn(53L);
		objList.add(objSummary);
		objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn("abc/xyz");
		when(objSummary.getSize()).thenReturn(11L);
		objList.add(objSummary);
		ObjectListing objListing = mock(ObjectListing.class);
		when(objListing.getObjectSummaries()).thenReturn(objList);
		when(objListing.isTruncated()).thenReturn(false);

		AmazonS3 s3Client = Mockito.mock(AmazonS3.class);
		String bucket = StackConfiguration.getS3Bucket();
		when(s3Client.listObjects(bucket, KeyFactory.stringToKey(nodeId) + "/")).thenReturn(objListing);
		ReflectionTestUtils.setField(unwrap(), "amazonS3Client", s3Client);
	}

	@After
	public void after() throws NotFoundException, DatastoreException {
		boolean success = nodeDao.delete(nodeId.toString());
		Assert.assertTrue(success);
		locations = null;
	}

	@Test
	public void testReplaceLocationData() throws NotFoundException, DatastoreException {
		long c = basicDao.getCount(DBOStorageLocation.class);
		dao.replaceLocationData(locations);
		// We have inserted 4 rows in this unit test
		Assert.assertEquals(4L + c, basicDao.getCount(DBOStorageLocation.class));
		// Repeat should replace the same 4 rows (i.e. shouldn't add another 4 rows)
		dao.replaceLocationData(locations);
		Assert.assertEquals(4L + c, basicDao.getCount(DBOStorageLocation.class));
	}

	@Test
	public void testTotalUsage() throws DatastoreException {
		dao.replaceLocationData(locations);
		Long total = dao.getTotalUsage(userId);
		Assert.assertEquals(87L, total.longValue());
		total = dao.getTotalUsage("syn9293829999990"); // fake user
		Assert.assertEquals(0L, total.longValue());
	}

	@Test
	public void testGetAggregatedUsage() {

		dao.replaceLocationData(locations);

		List<StorageUsageDimension> dList = new ArrayList<StorageUsageDimension>();
		StorageUsageSummaryList susList = dao.getAggregatedUsage(userId, dList);
		Assert.assertEquals(userId, susList.getUserId());
		Assert.assertEquals(87L, susList.getGrandTotal().longValue());
		Assert.assertEquals(0, susList.getSummaryList().size());

		dList.add(StorageUsageDimension.IS_ATTACHMENT);
		dList.add(StorageUsageDimension.STORAGE_PROVIDER);
		susList = dao.getAggregatedUsage(userId, dList);
		Assert.assertEquals(userId, susList.getUserId());
		Assert.assertEquals(87L, susList.getGrandTotal().longValue());
		List<StorageUsageSummary> summaryList = susList.getSummaryList();
		Assert.assertEquals(3, summaryList.size());
		long sum = 0;
		for (StorageUsageSummary summary : summaryList) {
			Assert.assertEquals(userId, summary.getUserId());
			List<StorageUsageDimensionValue> dvList = summary.getDimensionList();
			Assert.assertEquals(2, dvList.size());
			Assert.assertEquals(StorageUsageDimension.IS_ATTACHMENT, dvList.get(0).getDimension());
			Assert.assertEquals(StorageUsageDimension.STORAGE_PROVIDER, dvList.get(1).getDimension());
			sum = sum + summary.getTotalSize();
		}
		Assert.assertEquals(87L, sum);
	}

	@Test
	public void testGetStorageUsageInRange() {

		dao.replaceLocationData(locations);

		int beginIncl = 0;
		int endExcl = 1000;
		StorageUsageList usage = dao.getStorageUsageInRange(userId, beginIncl, endExcl);

		//Assert.assertEquals(87L, usage.getGrandTotal().longValue());
		Assert.assertEquals(userId, usage.getUserId());

		List<StorageUsage> suList = usage.getUsageList();
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
		Assert.assertEquals(23L, su.getContentSize().longValue());
		Assert.assertEquals("ad1Md5", su.getContentMd5());

		su = suMap.get("/" + KeyFactory.stringToKey(nodeId) + "/ad2Token");
		Assert.assertNotNull(su);
		Assert.assertEquals(userId, su.getUserId());
		Assert.assertEquals(nodeId, su.getNodeId());
		Assert.assertTrue(su.getIsAttachment());
		Assert.assertEquals(LocationTypeNames.awss3, su.getStorageProvider());
		Assert.assertEquals("/" + KeyFactory.stringToKey(nodeId) + "/ad2Token", su.getLocation());
		Assert.assertEquals("ad2Code", su.getContentType());
		Assert.assertEquals(53L, su.getContentSize().longValue());
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
		Assert.assertEquals(11L, su.getContentSize().longValue());
		Assert.assertEquals("ldMd5", su.getContentMd5());

		beginIncl = 1;
		endExcl = 3;
		usage = dao.getStorageUsageInRange(userId, beginIncl, endExcl);
		suList = usage.getUsageList();
		Assert.assertEquals(2, suList.size());
	}

	private StorageLocationDAO unwrap() throws Exception {
		if(AopUtils.isAopProxy(dao) && dao instanceof Advised) {
			Object target = ((Advised)dao).getTargetSource().getTarget();
			return (StorageLocationDAOImpl)target;
		}
		return dao;
	}
}
