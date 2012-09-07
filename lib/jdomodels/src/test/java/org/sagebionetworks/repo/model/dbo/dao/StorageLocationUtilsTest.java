package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;

public class StorageLocationUtilsTest {

	private StorageLocations locations;

	@Before
	public void before() {

		Long nodeId = Long.valueOf(111L);
		Long userId = Long.valueOf(333L);

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
		ld.setPath("/abc/xyz");
		ld.setType(LocationTypeNames.awss3);
		locationList.add(ld);

		Map<String, List<String>> strAnnotations = new HashMap<String, List<String>>();
		List<String> md5List = new ArrayList<String>();
		md5List.add("ldMd5");
		strAnnotations.put("md5", md5List);

		List<String> ctList = new ArrayList<String>();
		ctList.add("ldContentType");
		strAnnotations.put("contentType", ctList);

		locations = new StorageLocations(nodeId, userId,
			attachmentList, locationList, strAnnotations);
	}

	@After
	public void after() {
		locations = null;
	}

	@Test
	public void testCreateBatch() {

		List<DBOStorageLocation> dboList = StorageLocationUtils.createBatch(locations);
		Assert.assertNotNull(dboList);
		Assert.assertEquals(4, dboList.size());

		DBOStorageLocation dbo = dboList.get(0);
		Assert.assertNull(dbo.getId());
		Assert.assertEquals(111L, dbo.getNodeId().longValue());
		Assert.assertEquals(333L, dbo.getUserId().longValue());
		Assert.assertTrue(dbo.getIsAttachment());
		Assert.assertEquals("/111/ad1Token", dbo.getLocation());
		Assert.assertEquals(LocationTypeNames.awss3.name(), dbo.getStorageProvider());
		Assert.assertNull(dbo.getContentSize());
		Assert.assertEquals("ad1Code", dbo.getContentType());
		Assert.assertEquals("ad1Md5", dbo.getContentMd5());

		dbo = dboList.get(1);
		Assert.assertNull(dbo.getId());
		Assert.assertEquals(111L, dbo.getNodeId().longValue());
		Assert.assertEquals(333L, dbo.getUserId().longValue());
		Assert.assertTrue(dbo.getIsAttachment());
		Assert.assertEquals("/111/ad2Token", dbo.getLocation());
		Assert.assertEquals(LocationTypeNames.awss3.name(), dbo.getStorageProvider());
		Assert.assertNull(dbo.getContentSize());
		Assert.assertEquals("ad2Code", dbo.getContentType());
		Assert.assertEquals("ad2Md5", dbo.getContentMd5());

		dbo = dboList.get(2);
		Assert.assertNull(dbo.getId());
		Assert.assertEquals(111L, dbo.getNodeId().longValue());
		Assert.assertEquals(333L, dbo.getUserId().longValue());
		Assert.assertFalse(dbo.getIsAttachment());
		Assert.assertEquals("ld1Path", dbo.getLocation());
		Assert.assertEquals(LocationTypeNames.external.name(), dbo.getStorageProvider());
		Assert.assertNull(dbo.getContentSize());
		Assert.assertEquals("ldContentType", dbo.getContentType());
		Assert.assertEquals("ldMd5", dbo.getContentMd5());

		dbo = dboList.get(3);
		Assert.assertNull(dbo.getId());
		Assert.assertEquals(111L, dbo.getNodeId().longValue());
		Assert.assertEquals(333L, dbo.getUserId().longValue());
		Assert.assertFalse(dbo.getIsAttachment());
		Assert.assertEquals("/abc/xyz", dbo.getLocation());
		Assert.assertEquals(LocationTypeNames.awss3.name(), dbo.getStorageProvider());
		Assert.assertNull(dbo.getContentSize());
		Assert.assertEquals("ldContentType", dbo.getContentType());
		Assert.assertEquals("ldMd5", dbo.getContentMd5());
	}
} 
