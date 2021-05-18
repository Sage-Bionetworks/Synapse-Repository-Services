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
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.springframework.dao.EmptyResultDataAccessException;
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
		V2DBOWikiMarkdown wikiMarkdown = new V2DBOWikiMarkdown();
		wikiMarkdown.setModifiedBy(USER_ID);
		wikiMarkdown.setFileHandleId(FILE_HANDLE_ID);
		wikiMarkdown.setAttachmentIdList(new byte[] {});
		when(jdbcTemplate.query(eq("SELECT * FROM V2_WIKI_MARKDOWN WHERE WIKI_ID = ? AND MARKDOWN_VERSION = ?"), 
				(TableMapping<V2DBOWikiMarkdown>)any(TableMapping.class),eq(WIKI_ID), eq(MARKDOWN_VERSION))).
				thenReturn(Collections.singletonList(wikiMarkdown));
		
		// method under test
		wikiPageDao.create(toCreate, fileNameToFileHandleMap, OWNER_ID.toString(), ObjectType.ENTITY, newFileHandleIds);
		
		verify(idGenerator).generateNewId(IdType.WIKI_ID);
		
		verify(transactionalMessenger).sendMessageAfterCommit(messageToSendCaptor.capture());
		
		assertEquals(ChangeType.CREATE, messageToSendCaptor.getValue().getChangeType());
	}
	
	@Test
	public void testDelete() {
		WikiPageKey pageKey = new WikiPageKey();
		pageKey.setOwnerObjectId(OWNER_ID.toString());
		pageKey.setOwnerObjectType(ObjectType.ENTITY);
		pageKey.setWikiPageId(WIKI_ID.toString());
		
		// method under test
		wikiPageDao.delete(pageKey);
		
		verify(transactionalMessenger).sendMessageAfterCommit(messageToSendCaptor.capture());
		
		assertEquals(ChangeType.DELETE, messageToSendCaptor.getValue().getChangeType());
		
	}

}
