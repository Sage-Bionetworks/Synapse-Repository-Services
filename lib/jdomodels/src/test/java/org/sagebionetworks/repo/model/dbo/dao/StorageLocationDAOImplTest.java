package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StorageLocationDAOImplTest {

	@Autowired
	private DBOBasicDao dboBasicDao;

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
	public void before() throws NumberFormatException, DatastoreException, NotFoundException, InvalidModelException {
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

		locations = new StorageLocations(KeyFactory.stringToKey(nodeId), userIdLong,
			attachmentList, locationList, strAnnotations);
	}

	@After
	public void after() throws NotFoundException, DatastoreException {
		boolean success = nodeDao.delete(nodeId.toString());
		Assert.assertTrue(success);
		locations = null;
	}

	@Test
	public void test() throws NotFoundException, DatastoreException {
		this.dao.replaceLocationData(locations);
		// We have inserted 4 rows in this unit test
		Assert.assertTrue(this.dboBasicDao.getCount(DBOStorageLocation.class) >= 4);
	}
}
