package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.V2WikiPageMirrorDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class V2DBOWikiPageMirrorDAOTest {
	@Autowired
	private V2WikiPageMirrorDao v2WikiPageMirrorDao;
	@Autowired
	V2WikiPageDao wikiPageDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	private List<WikiPageKey> toDelete;
	String creatorUserGroupId;
	S3FileHandle attachOne;
	S3FileHandle markdown;
	
	@Before
	public void before(){
		toDelete = new LinkedList<WikiPageKey>();
		UserGroup userGroup = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false);
		assertNotNull(userGroup);
		creatorUserGroupId = userGroup.getId();
		assertNotNull(creatorUserGroupId);
		
		// Create a few files
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("prefix.txt1");
		meta = fileMetadataDao.createFile(meta);
		attachOne = meta;
		
		//Create different markdown content
		meta = new S3FileHandle();
		meta.setBucketName("markdownBucketName");
		meta.setKey("key3");
		meta.setContentType("content type3");
		meta.setContentSize((long) 1231);
		meta.setContentMd5("md53");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName("markdown1");
		markdown = fileMetadataDao.createFile(meta);
	}
	
	@After
	public void after(){
		// Delete the file handles
		if(attachOne != null){
			fileMetadataDao.delete(attachOne.getId());
		}
		if(markdown != null){
			fileMetadataDao.delete(markdown.getId());
		}
	}
	
	@Test
	public void testCRUD() throws NotFoundException {
		String ownerId = "syn182";
		ObjectType ownerType = ObjectType.ENTITY;
		
		V2WikiPage page = new V2WikiPage();
		page.setId("1");
		page.setEtag("etag");
		page.setCreatedOn(new Date(1));
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setModifiedOn(page.getCreatedOn());
		page.setParentWikiId(null);
		page.setTitle("title1");
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.setMarkdownFileHandleId(markdown.getId());
		V2WikiPage clone = v2WikiPageMirrorDao.create(page, new HashMap<String, FileHandle>(), ownerId, ownerType, new ArrayList<String>());
		assertNotNull(clone);
		assertEquals(1, wikiPageDao.getCount());
		WikiPageKey key = new WikiPageKey(ownerId, ownerType, clone.getId());
		toDelete.add(key);
		// Nothing should be different. Should have just copied over
		assertEquals(page, clone);
		// Make sure we can lock
		String etag = wikiPageDao.lockForUpdate(clone.getId());
		assertNotNull(etag);
		assertEquals(page.getEtag(), etag);
		
		clone.setEtag("etag2");
		clone.setModifiedOn(new Date(2));
		List<String> fileIds = clone.getAttachmentFileHandleIds();
		fileIds.add(attachOne.getId());
		Map<String, FileHandle> fileNameMap = new HashMap<String, FileHandle>();
		fileNameMap.put(attachOne.getFileName(), attachOne);
		List<String> newIds = new ArrayList<String>();
		newIds.add(attachOne.getId());
		V2WikiPage updatedClone = v2WikiPageMirrorDao.update(clone, fileNameMap, ownerId, ownerType, newIds);
		assertNotNull(updatedClone);
		assertEquals(clone, updatedClone);
		List<Long> reservation = v2WikiPageMirrorDao.getFileHandleReservationForWiki(key);
		assertEquals(1, reservation.size());
		fileMetadataDao.get(reservation.get(0).toString());
		
		v2WikiPageMirrorDao.delete(key);
		assertEquals(0, wikiPageDao.getCount());
		assertEquals(0, v2WikiPageMirrorDao.getFileHandleReservationForWiki(key).size());
	}
}
