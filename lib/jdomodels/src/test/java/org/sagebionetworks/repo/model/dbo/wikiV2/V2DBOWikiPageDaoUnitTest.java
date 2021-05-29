package org.sagebionetworks.repo.model.dbo.wikiV2;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.migration.MigrationType;
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
	private ArgumentCaptor<DatabaseObject> dboCaptor;
	
	@Captor
	private ArgumentCaptor<V2DBOWikiPage> wikiPageDboCaptor;
	
	@Captor
	private ArgumentCaptor<V2DBOWikiOwner> wikiOwnerDboCaptor;
	
	@Captor
	private ArgumentCaptor<V2DBOWikiMarkdown> wikiMarkdownDboCaptor;
	
	@Captor
	private ArgumentCaptor<List<V2DBOWikiAttachmentReservation>> attachmentBatchCaptor;
	
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
		
		verify(basicDao, times(3)).createNew(dboCaptor.capture()); 
		List<DatabaseObject> createdDbos = dboCaptor.getAllValues();
		
		V2DBOWikiPage createdDboWikiPage = (V2DBOWikiPage)createdDbos.get(0);
		assertEquals(dbo.getId(), createdDboWikiPage.getId());
		
		V2DBOWikiOwner createdDboWikiOwner = (V2DBOWikiOwner)createdDbos.get(1); 
		assertEquals(OWNER_ID, createdDboWikiOwner.getOwnerId());
		assertEquals(WIKI_ID, createdDboWikiOwner.getRootWikiId());
		
		V2DBOWikiMarkdown createdDboMarkdown = (V2DBOWikiMarkdown)createdDbos.get(2); 
		assertEquals(WIKI_ID, createdDboMarkdown.getWikiId());
		assertEquals(Long.parseLong(MARKDOWN_FILE_HANDLE_ID), createdDboMarkdown.getFileHandleId());
		assertEquals(0L, createdDboMarkdown.getMarkdownVersion());
		
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
		
		verify(basicDao).update(wikiPageDboCaptor.capture());
		assertEquals(WIKI_ID, wikiPageDboCaptor.getValue().getId());
		assertEquals(WIKI_ID, wikiPageDboCaptor.getValue().getRootId());
		assertNull(wikiPageDboCaptor.getValue().getParentId());
		
		verify(basicDao).createNew(wikiMarkdownDboCaptor.capture());
		assertEquals(WIKI_ID, wikiMarkdownDboCaptor.getValue().getWikiId());
		assertEquals(Long.parseLong(MARKDOWN_FILE_HANDLE_ID), wikiMarkdownDboCaptor.getValue().getFileHandleId());
		assertEquals(MARKDOWN_VERSION+1L, wikiMarkdownDboCaptor.getValue().getMarkdownVersion());
		
		verify(transactionalMessenger).sendMessageAfterCommit((V2DBOWikiPage)any(), eq(ChangeType.UPDATE));
	}
	
	@Test
	public void testUpdateOrderHint() {
		V2WikiOrderHint orderHint = new V2WikiOrderHint();
		String etag = "ETAG";
		orderHint.setEtag(etag);
		orderHint.setOwnerId(OWNER_ID.toString());
		orderHint.setOwnerObjectType(ObjectType.ENTITY);
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(OWNER_ID.toString());
		key.setOwnerObjectType(ObjectType.ENTITY);
		key.setWikiPageId(WIKI_ID.toString());
		
		V2DBOWikiOwner wikiOwner = new V2DBOWikiOwner();
		wikiOwner.setRootWikiId(WIKI_ID);
		wikiOwner.setOwnerId(OWNER_ID);
		
		when(jdbcTemplate.query(eq("SELECT * FROM V2_WIKI_OWNERS WHERE ROOT_WIKI_ID = ?"),
				(TableMapping<V2DBOWikiOwner>)any(TableMapping.class), eq(WIKI_ID))).
				thenReturn(Collections.singletonList(wikiOwner));

		// method under test
		wikiPageDao.updateOrderHint(orderHint, key);
		
	
		verify(basicDao).update(wikiOwnerDboCaptor.capture());
		assertEquals(WIKI_ID, wikiOwnerDboCaptor.getValue().getRootWikiId());
		assertEquals(MigrationType.V2_WIKI_OWNERS, wikiOwnerDboCaptor.getValue().getMigratableTableType());
		assertEquals(etag, wikiOwnerDboCaptor.getValue().getEtag());
		assertEquals(OWNER_ID, wikiOwnerDboCaptor.getValue().getOwnerId());
		assertEquals(ObjectType.ENTITY, wikiOwnerDboCaptor.getValue().getOwnerType());

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
		
		verify(jdbcTemplate).update("DELETE FROM V2_WIKI_PAGE WHERE ID = ? AND ROOT_ID = ?", WIKI_ID, null);
		
		verify(transactionalMessenger).sendMessageAfterCommit(messageToSendCaptor.capture());
		assertEquals(ChangeType.DELETE, messageToSendCaptor.getValue().getChangeType());
		
	}

}
