package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class WikiAttachmentFileScannerIntegrationTest {

	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private V2WikiPageDao wikiDao;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.WikiAttachment;
	
	private UserInfo user;
	
	
	@BeforeEach
	public void before() {
		wikiDao.truncateAll();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		wikiDao.truncateAll();
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		
		List<V2WikiPage> wikiPages = new ArrayList<>();
		
		wikiPages.add(createWikiMarkdown("123", 1));
		wikiPages.add(createWikiMarkdown("456", 0));
		wikiPages.add(createWikiMarkdown("789", 2));
		
		List<ScannedFileHandleAssociation> expected = new ArrayList<>();

		for (V2WikiPage wiki : wikiPages) {
			for (String fileHandleId : wiki.getAttachmentFileHandleIds()) {
				expected.add(new ScannedFileHandleAssociation(Long.valueOf(wiki.getId()), Long.valueOf(fileHandleId)));
			}
		}
		
		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, range).spliterator(), false
		).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}
	
	private V2WikiPage createWikiMarkdown(String ownerId, int attachmentNum) {
		V2WikiPage wikiPage = new V2WikiPage();
		
		wikiPage.setCreatedBy(user.getId().toString());
		wikiPage.setModifiedBy(user.getId().toString());
		wikiPage.setMarkdownFileHandleId(utils.generateFileHandle(user));
		
		List<String> attachments = IntStream.range(0, attachmentNum).boxed().map(i-> utils.generateFileHandle(user)).collect(Collectors.toList());
		
		wikiPage.setAttachmentFileHandleIds(attachments);
		
		wikiPage = wikiDao.create(wikiPage, Collections.emptyMap(), ownerId, ObjectType.ENTITY, attachments);
		
		// Shortcut: the create method above take in input both a map between a file name and its id and the list of attachement ids (why???). Then serialize this to a list in form of a string. When reading a
		// wiki page it reads from that list instead of the list of attachment, since we pass an empty map the list is going to be empty. What we are interested in, is just the list of attachments.
		wikiPage.setAttachmentFileHandleIds(attachments);
		
		return wikiPage;
	}

}
