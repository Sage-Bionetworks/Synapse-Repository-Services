package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.V2WikiPageMigrationDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class V2DBOWikiPageMigrationDAOImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private V2WikiPageMigrationDao v2WikiPageMigrationDao;
	
	@Autowired
	private V2WikiPageDao v2WikiPageDao;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	private static final String SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION = 
		"SELECT "+V2_COL_WIKI_MARKDOWN_TITLE+" FROM "+V2_TABLE_WIKI_MARKDOWN+
		" WHERE "+V2_COL_WIKI_MARKDOWN_ID+" = ? AND "+V2_COL_WIKI_MARKDOWN_VERSION_NUM+" = ?";
	
	public static final String SQL_SELECT_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP = 
		"SELECT "+V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP+" FROM "+V2_TABLE_WIKI_ATTACHMENT_RESERVATION+
		" WHERE "+V2_COL_WIKI_ATTACHMENT_RESERVATION_ID+" = ? AND "+V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID+" = ?"; 
	
	/**
	 * Mapping to the timestamp of a file attachment
	 */
	private static final RowMapper<Timestamp> WIKI_ATTACHMENT_TIMESTAMP_MAPPER = new RowMapper<Timestamp>() {
		@Override
		public Timestamp mapRow(ResultSet rs, int rowNum) throws SQLException {
			Timestamp ts = rs.getTimestamp(V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP);
			return ts;
		}
	};
	
	private List<WikiPageKey> toDeleteFromV2;
	private String creatorUserGroupId;
	
	private S3FileHandle attachOne;
	private S3FileHandle markdown;
	
	@Before
	public void before(){
		toDeleteFromV2 = new LinkedList<WikiPageKey>();
		
		UserGroup user = new UserGroup();
		user.setName(UUID.randomUUID().toString());
		user.setIsIndividual(true);
		user.setId(userGroupDAO.create(user));
		creatorUserGroupId = user.getId();

		// We use a long file name to test the uniqueness constraint
		String prefix = "prefix";
		
		// Create a few files
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(prefix+".txt1");
		meta = fileMetadataDao.createFile(meta);
		attachOne = meta;

		meta = new S3FileHandle();
		meta.setBucketName("bucketName2");
		meta.setKey("key2");
		meta.setContentType("content type2");
		meta.setContentSize(123l);
		meta.setContentMd5("md52");
		meta.setCreatedBy(creatorUserGroupId);
		meta.setFileName(prefix+".txt2");
		meta = fileMetadataDao.createFile(meta);
		markdown = meta;
	}
	
	@After
	public void after() throws Exception {
		for (WikiPageKey id : toDeleteFromV2) {
			v2WikiPageDao.delete(id);
		}
		// Delete the file handles
		fileMetadataDao.delete(attachOne.getId());
		fileMetadataDao.delete(markdown.getId());
		
		userGroupDAO.delete(creatorUserGroupId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParentCycle() throws NotFoundException {
		// NOTE: Tried this without a check for parent/child id cycle
		// and an exception was thrown because no root wiki exists
		
		String ownerId = "syn1";
		ObjectType ownerType = ObjectType.ENTITY;
		// Create a V2 WikiPage with id, etag, dates etc all set from conversion
		V2WikiPage page = new V2WikiPage();
		page.setId("1");
		page.setParentWikiId("1");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdown.getId());
		page.setTitle("title1");
		page.setEtag("etag");	
		page.setCreatedOn(new Date(1));
		page.setModifiedOn(new Date(1));
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		fileNameToFileHandleMap.put(attachOne.getFileName(), fileMetadataDao.get(attachOne.getId()));
		List<String> newFileHandleIds = new ArrayList<String>();
		newFileHandleIds.add(attachOne.getId());
		
		// Create
		V2WikiPage result = v2WikiPageMigrationDao.create(page, fileNameToFileHandleMap, 
				ownerId, ownerType, newFileHandleIds);
	}
	
	@Test
	public void testCreateAndUpdateOnDuplicate() throws DatastoreException, NotFoundException, InterruptedException {
		String ownerId = "syn1";
		ObjectType ownerType = ObjectType.ENTITY;
		
		// Create a V2 WikiPage with id, etag, dates etc all set from conversion
		V2WikiPage page = new V2WikiPage();
		page.setId("1");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdown.getId());
		page.setTitle("title1");
		page.setEtag("etag");	
		page.setCreatedOn(new Date(1));
		page.setModifiedOn(new Date(1));
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		page.getAttachmentFileHandleIds().add(attachOne.getId());
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		fileNameToFileHandleMap.put(attachOne.getFileName(), fileMetadataDao.get(attachOne.getId()));
		List<String> newFileHandleIds = new ArrayList<String>();
		newFileHandleIds.add(attachOne.getId());
		
		// Create
		V2WikiPage result = v2WikiPageMigrationDao.create(page, fileNameToFileHandleMap, 
				ownerId, ownerType, newFileHandleIds);
		assertNotNull(result);
		assertEquals(1, v2WikiPageDao.getCount());
		
		// ID, etag, etc shouldn't have reset by creation method
		assertEquals("etag", result.getEtag());
		assertEquals("1", result.getId());
		assertEquals(1, result.getCreatedOn().getTime());
		WikiPageKey key = v2WikiPageDao.lookupWikiKey(result.getId());
		toDeleteFromV2.add(key);
		
		// Store the timestamp on the file attachments
		List<Timestamp> ts = simpleJdbcTemplate.query(SQL_SELECT_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP, 
				WIKI_ATTACHMENT_TIMESTAMP_MAPPER, new Long(1), new Long(attachOne.getId()));
		long firstTimestamp = ts.get(0).getTime();
		
		// Sleep and make sure timestamp of the attachment will change on update
		Thread.sleep(1000);
		
		// Edit the wiki to check for the update
		result.setTitle("titleEdited");
		
		// The ID is the same, so it should update the V2 DB and not throw an error
		V2WikiPage resultAfterUpdate = v2WikiPageMigrationDao.create(result, fileNameToFileHandleMap, 
				ownerId, ownerType, newFileHandleIds);
		assertNotNull(resultAfterUpdate);
		// No new rows should have been made
		assertEquals(1, v2WikiPageDao.getCount());
		// The title should be updated
		assertEquals("titleEdited", resultAfterUpdate.getTitle());
		// There should still just be one markdown version (0), but with the updated title
		assertEquals(1, v2WikiPageDao.getWikiHistory(key, new Long(10), new Long(0)).size());
		List<String> title = simpleJdbcTemplate.query(SQL_SELECT_WIKI_MARKDOWN_USING_ID_AND_VERSION, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String title = rs.getString(V2_COL_WIKI_MARKDOWN_TITLE);
				return title;
			}
		}, new Long(1), new Long(0));
		assertEquals(1, title.size());
		assertEquals("titleEdited", title.get(0));
		
		// Check the timestamp of the same file attachment
		List<Timestamp> tsAfterUpdate = simpleJdbcTemplate.query(SQL_SELECT_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP, 
				WIKI_ATTACHMENT_TIMESTAMP_MAPPER, new Long(1), new Long(attachOne.getId()));
		assertEquals(1, ts.size());
		// The timestamp of the same attachment should have updated
		assertTrue(firstTimestamp != tsAfterUpdate.get(0).getTime());
	}
	
	@Test
	public void testParentCheckAndGetEtag() throws NotFoundException {
		// Test parent wiki check
		String ownerId = "syn1";
		ObjectType ownerType = ObjectType.ENTITY;
		// Create a parent wiki page
		V2WikiPage page = new V2WikiPage();
		page.setId("1");
		page.setCreatedBy(creatorUserGroupId);
		page.setModifiedBy(creatorUserGroupId);
		page.setMarkdownFileHandleId(markdown.getId());
		page.setTitle("title1");
		page.setEtag("etag");	
		page.setCreatedOn(new Date(1));
		page.setModifiedOn(new Date(1));
		page.setAttachmentFileHandleIds(new ArrayList<String>());
		
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		List<String> newFileHandleIds = new ArrayList<String>();
		
		V2WikiPage result = v2WikiPageMigrationDao.create(page, fileNameToFileHandleMap, 
				ownerId, ownerType, newFileHandleIds);
		assertNotNull(result);
		assertEquals(1, v2WikiPageDao.getCount());
		WikiPageKey key = v2WikiPageDao.lookupWikiKey(result.getId());
		toDeleteFromV2.add(key);
		
		V2WikiPage child = new V2WikiPage();
		child.setId("2");
		child.setParentWikiId("1");
		// Before creating the child, we will make this call
		boolean doesParentExist = v2WikiPageMigrationDao.doesWikiExist(child.getParentWikiId());
		assertEquals(true, doesParentExist);
		
		String etagResult = v2WikiPageMigrationDao.getWikiEtag(result.getId());
		assertEquals("etag", etagResult);
		
		// Try to get the etag of a wiki that doesn't exist
		String invalidEtagResult = v2WikiPageMigrationDao.getWikiEtag("3");
		assertEquals(null, invalidEtagResult);
	}
}
