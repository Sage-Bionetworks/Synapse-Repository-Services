package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
public class WikiMarkdownFileScannerIntegrationTest {
	
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
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.WikiMarkdown;
	
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
		
		V2WikiPage w1 = createWikiMarkdown("123");
		V2WikiPage w2 = createWikiMarkdown("456");
		V2WikiPage w3 = createWikiMarkdown("789");
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(Long.valueOf(w1.getId()), Long.valueOf(w1.getMarkdownFileHandleId())),
				new ScannedFileHandleAssociation(Long.valueOf(w2.getId()), Long.valueOf(w2.getMarkdownFileHandleId())),
				new ScannedFileHandleAssociation(Long.valueOf(w3.getId()), Long.valueOf(w3.getMarkdownFileHandleId()))
		);
		
		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(
				manager.scanRange(associationType, range).spliterator(), false
		).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}
	
	private V2WikiPage createWikiMarkdown(String ownerId) {
		V2WikiPage wikiPage = new V2WikiPage();
		
		wikiPage.setCreatedBy(user.getId().toString());
		wikiPage.setModifiedBy(user.getId().toString());
		wikiPage.setMarkdownFileHandleId(utils.generateFileHandle(user));
		
		wikiPage = wikiDao.create(wikiPage, Collections.emptyMap(), ownerId, ObjectType.ENTITY, Collections.emptyList());
		
		return wikiPage;
	}

}
