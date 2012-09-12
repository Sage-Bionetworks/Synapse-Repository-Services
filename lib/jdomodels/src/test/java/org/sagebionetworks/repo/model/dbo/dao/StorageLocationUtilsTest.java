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
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class StorageLocationUtilsTest {

	private StorageLocations locations;
	private AmazonS3 s3Client;

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
		ld.setPath("/ld1Path");
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

		List<S3ObjectSummary> objList1 = new ArrayList<S3ObjectSummary>();
		S3ObjectSummary objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn("111/ad1Token");
		when(objSummary.getSize()).thenReturn(12345L);
		objList1.add(objSummary);
		ObjectListing objListing1 = mock(ObjectListing.class);
		when(objListing1.getObjectSummaries()).thenReturn(objList1);
		when(objListing1.isTruncated()).thenReturn(true);

		List<S3ObjectSummary> objList2 = new ArrayList<S3ObjectSummary>();
		objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn("abc/xyz");
		when(objSummary.getSize()).thenReturn(54321L);
		objList2.add(objSummary);
		objSummary = mock(S3ObjectSummary.class);
		when(objSummary.getKey()).thenReturn("ld1Path");
		when(objSummary.getSize()).thenReturn(99999L);
		objList2.add(objSummary);
		ObjectListing objListing2 = mock(ObjectListing.class);
		when(objListing2.getObjectSummaries()).thenReturn(objList2);
		when(objListing2.isTruncated()).thenReturn(false);

		s3Client = mock(AmazonS3Client.class);
		String bucket = StackConfiguration.getS3Bucket();
		when(s3Client.listObjects(bucket, "111/")).thenReturn(objListing1);
		when(s3Client.listNextBatchOfObjects(objListing1)).thenReturn(objListing2);
	}

	@After
	public void after() {
		locations = null;
		s3Client = null;
	}

	@Test
	public void testCreateBatch() {

		List<DBOStorageLocation> dboList = StorageLocationUtils.createBatch(locations, s3Client);
		Assert.assertNotNull(dboList);
		Assert.assertEquals(4, dboList.size());

		DBOStorageLocation dbo = dboList.get(0);
		Assert.assertNull(dbo.getId());
		Assert.assertEquals(111L, dbo.getNodeId().longValue());
		Assert.assertEquals(333L, dbo.getUserId().longValue());
		Assert.assertTrue(dbo.getIsAttachment());
		Assert.assertEquals("/111/ad1Token", dbo.getLocation());
		Assert.assertEquals(LocationTypeNames.awss3.name(), dbo.getStorageProvider());
		Assert.assertEquals(Long.valueOf(12345L), dbo.getContentSize());
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
		Assert.assertEquals("/ld1Path", dbo.getLocation());
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
		Assert.assertEquals(Long.valueOf(54321L), dbo.getContentSize());
		Assert.assertEquals("ldContentType", dbo.getContentType());
		Assert.assertEquals("ldMd5", dbo.getContentMd5());
	}
} 
