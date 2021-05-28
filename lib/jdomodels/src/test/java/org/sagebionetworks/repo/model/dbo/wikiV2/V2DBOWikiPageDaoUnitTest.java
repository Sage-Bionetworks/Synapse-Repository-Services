package org.sagebionetworks.repo.model.dbo.wikiV2;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class V2DBOWikiPageDaoUnitTest {
	
	@Mock
	private IdGenerator idGenerator;
	
	@Mock
	private TransactionalMessenger transactionalMessenger;
	
	@Mock
	private DBOBasicDao basicDao;
	
	@Mock
	private JdbcTemplate jdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate namedTemplate;
	
	@Mock
	private SynapseS3Client s3Client;

	@Mock
	private FileHandleDao fileMetadataDao;	
	
	@InjectMocks
	private V2DBOWikiPageDaoImpl wikiPageDao;
	
	@Captor
	private ArgumentCaptor<MessageToSend> messageToSendCaptor;
	
	@Captor
	private ArgumentCaptor<V2DBOWikiOwner> wikiDboCaptor;
	
	private static final Long OWNER_ID = 101L;
	private static final Long WIKI_ID = 202L;
	private static final String MARKDOWN_FILE_HANDLE_ID = "303";
	private static final Long MARKDOWN_VERSION = 1L;
	private static final Long USER_ID = 99L;
	private static final Long FILE_HANDLE_ID = 123L;

	@Test
	public void testCreate() {
		V2WikiPage toCreate = new V2WikiPage();
		toCreate.setMarkdownFileHandleId(MARKDOWN_FILE_HANDLE_ID);
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		List<String> newFileHandleIds = new ArrayList<String>();
		
		when(idGenerator.generateNewId(IdType.WIKI_ID)).thenReturn(WIKI_ID);
		V2DBOWikiPage dbo = new V2DBOWikiPage();
		dbo.setId(WIKI_ID);
		dbo.setCreatedBy(USER_ID);
		dbo.setModifiedBy(USER_ID);
		dbo.setMarkdownVersion(MARKDOWN_VERSION);
		when(basicDao.createNew(any(V2DBOWikiPage.class))).thenReturn(dbo);
		
		when(jdbcTemplate.queryForObject("SELECT ROOT_WIKI_ID FROM V2_WIKI_OWNERS WHERE OWNER_ID = ? AND OWNER_OBJECT_TYPE = ?", 
				Long.class, OWNER_ID, "ENTITY")).thenReturn(WIKI_ID);
		
		when(jdbcTemplate.query(eq("SELECT * FROM V2_WIKI_PAGE WHERE ID = ? AND ROOT_ID = ?"), 
				(TableMapping<V2DBOWikiPage>)any(TableMapping.class), eq(WIKI_ID), eq(WIKI_ID))).
				thenReturn(Collections.singletonList(dbo));
		V2DBOWikiMarkdown dboMarkdown = new V2DBOWikiMarkdown();
		dboMarkdown.setModifiedBy(USER_ID);
		dboMarkdown.setFileHandleId(FILE_HANDLE_ID);
		dboMarkdown.setAttachmentIdList(new byte[] {});
		when(jdbcTemplate.query(eq("SELECT * FROM V2_WIKI_MARKDOWN WHERE WIKI_ID = ? AND MARKDOWN_VERSION = ?"), 
				(TableMapping<V2DBOWikiMarkdown>)any(TableMapping.class),eq(WIKI_ID), eq(MARKDOWN_VERSION))).
				thenReturn(Collections.singletonList(dboMarkdown));
		
		// method under test
		wikiPageDao.create(toCreate, fileNameToFileHandleMap, OWNER_ID.toString(), ObjectType.ENTITY, newFileHandleIds);
		
		// TODO
		verify(basicDao.createNew(toCreate)); // V2DBOWikiPage see line 299 in V2DBOWikiPageDaoImpl
		verify(basicDao.createNew(toCreate)); // V2DBOWikiOwner see line 480 in V2DBOWikiPageDaoImpl
		verify(basicDao.createBatch(batch)); // List<V2DBOWikiAttachmentReservation> see line 276 in V2DBOWikiPageDaoImpl
		verify(basicDao.createNew(toCreate)); // V2DBOWikiMarkdown see line 286 in V2DBOWikiPageDaoImpl
		
		verify(transactionalMessenger).sendMessageAfterCommit(messageToSendCaptor.capture());
		assertEquals(ChangeType.CREATE, messageToSendCaptor.getValue().getChangeType());
	}
	
	@Test
	public void testUpdate() {
		V2WikiPage toUpdate = new V2WikiPage();
		toUpdate.setId(WIKI_ID.toString());
		toUpdate.setMarkdownFileHandleId(MARKDOWN_FILE_HANDLE_ID);
		Map<String, FileHandle> fileNameToFileHandleMap = new HashMap<String, FileHandle>();
		List<String> newFileHandleIds = new ArrayList<String>();
		
		when(jdbcTemplate.queryForObject("SELECT ID FROM V2_WIKI_PAGE WHERE ID = ?", 
				Long.class, WIKI_ID.toString())).thenReturn(WIKI_ID);
		
		when(jdbcTemplate.queryForObject("SELECT ROOT_WIKI_ID FROM V2_WIKI_OWNERS WHERE OWNER_ID = ? AND OWNER_OBJECT_TYPE = ?", 
				Long.class, OWNER_ID, "ENTITY")).thenReturn(WIKI_ID);
		
		V2DBOWikiPage dbo = new V2DBOWikiPage();
		dbo.setId(WIKI_ID);
		dbo.setCreatedBy(USER_ID);
		dbo.setModifiedBy(USER_ID);
		dbo.setMarkdownVersion(MARKDOWN_VERSION);
		when(jdbcTemplate.query(eq("SELECT * FROM V2_WIKI_PAGE WHERE ID = ? AND ROOT_ID = ?"), 
				(TableMapping<V2DBOWikiPage>)any(TableMapping.class), eq(WIKI_ID), eq(WIKI_ID))).
				thenReturn(Collections.singletonList(dbo));
		
		V2DBOWikiMarkdown dboMarkdown = new V2DBOWikiMarkdown();
		dboMarkdown.setWikiId(WIKI_ID);
		dboMarkdown.setMarkdownVersion(MARKDOWN_VERSION);
		dboMarkdown.setFileHandleId(FILE_HANDLE_ID);
		dboMarkdown.setAttachmentIdList(new byte[] {});
		dboMarkdown.setModifiedBy(USER_ID);
		when(jdbcTemplate.query(eq("SELECT * FROM V2_WIKI_MARKDOWN WHERE WIKI_ID = ? AND MARKDOWN_VERSION = ?"),
				(TableMapping<V2DBOWikiMarkdown>)any(TableMapping.class), eq(WIKI_ID), eq(MARKDOWN_VERSION))).
				thenReturn(Collections.singletonList(dboMarkdown));
		
		// method under test
		wikiPageDao.updateWikiPage(toUpdate, fileNameToFileHandleMap, OWNER_ID.toString(), ObjectType.ENTITY, newFileHandleIds);
		
		verify(basicDao.update(toUpdate)); //  V2DBOWikiPage see line 389 in V2DBOWikiPageDaoImpl
		verify(basicDao.createNew(toCreate)); // V2DBOWikiMarkdown see line 351 in V2DBOWikiPageDaoImpl
		verify(basicDao.createBatch(batch)); // List<V2DBOWikiAttachmentReservation> see line 359 in V2DBOWikiPageDaoImpl

		verify(transactionalMessenger).sendMessageAfterCommit((V2DBOWikiPage)any(), eq(ChangeType.UPDATE));
	}
	
	@Test
	public void testUpdateOrderHint() {
		V2WikiOrderHint orderHint = new V2WikiOrderHint();
		WikiPageKey key = new WikiPageKey();
		
		// method under test
		wikiPageDao.updateOrderHint(orderHint, key);
		
	
		verify(basicDao).update(wikiDboCaptor.capture());
		wikiDboCaptor.getValue().getRootWikiId()
		wikiDboCaptor.getValue()

		verify(transactionalMessenger).sendMessageAfterCommit(messageToSendCaptor.capture());
		assertEquals(ChangeType.UPDATE, messageToSendCaptor.getValue().getChangeType());
	
	}
	
	@Test
	public void testDelete() {
		WikiPageKey pageKey = new WikiPageKey();
		pageKey.setOwnerObjectId(OWNER_ID.toString());
		pageKey.setOwnerObjectType(ObjectType.ENTITY);
		pageKey.setWikiPageId(WIKI_ID.toString());
		
		// method under test
		wikiPageDao.delete(pageKey);
		
		verify(jdbcTemplate).update("DELETE FROM V2_WIKI_PAGE WHERE ID = ? AND ROOT_ID = ?", WIKI_ID, WIKI_ID);
		
		verify(transactionalMessenger).sendMessageAfterCommit(messageToSendCaptor.capture());
		assertEquals(ChangeType.DELETE, messageToSendCaptor.getValue().getChangeType());
		
	}

}
