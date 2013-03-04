package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.backup.WikiPageAttachmentBackup;
import org.sagebionetworks.repo.model.backup.WikiPageBackup;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;

/**
 * Unit test for WikiMigratableManager
 * @author John
 *
 */
public class WikiMigratableManagerTest {
	
	WikiPageDao sourceStubDao;
	WikiPageDao destStubDao;
	WikiPageMigratableManager sourceManager;
	WikiPageMigratableManager destManager;
	
	@Before
	public void before(){
		sourceStubDao = new StubWikiPageDao();
		sourceManager = new WikiPageMigratableManager(sourceStubDao);
		destStubDao = new StubWikiPageDao();
		destManager = new WikiPageMigratableManager(destStubDao);
	}
	
	@Test
	public void testRoundTrip() throws Exception{
		// Add a backup the source
		WikiPageBackup backup = createBackup();
		WikiPageKey key = sourceStubDao.createOrUpdateFromBackup(backup);
		
		// backup source
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// Write the data to the stream.
		sourceManager.writeBackupToOutputStream(key.getKeyString(), out);
		String data = new String(out.toByteArray(), "UTF-8");
		System.out.println(data);
		// Read the file into the destination.
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes( "UTF-8"));
		destManager.createOrUpdateFromBackupStream(in);
		
		// Make sure the data moved to the destination
		WikiPageBackup clone = destStubDao.getWikiPageBackup(key);
		assertNotNull(clone);
		assertEquals(backup, clone);
	}
	
	public WikiPageBackup createBackup(){
		WikiPageBackup backup = new WikiPageBackup();
		backup.setOwnerId(123l);
		backup.setOwnerType(ObjectType.EVALUATION.name());
		backup.setId(456l);
		backup.setAttachmentFileHandles(new LinkedList<WikiPageAttachmentBackup>());
		backup.getAttachmentFileHandles().add(new WikiPageAttachmentBackup(9999l, "foo.bar"));
		backup.getAttachmentFileHandles().add(new WikiPageAttachmentBackup(8888l, "bar.foo"));
		backup.setCreatedBy(3333l);
		backup.setCreatedOn(1l);
		backup.setEtag("etag");
		backup.setMarkdown("markdown with xml: <tag>data</tag>");
		backup.setModifiedBy(6l);
		backup.setModifiedOn(2l);
		backup.setParentWikiId(7654l);
		backup.setTitle("title");
		return backup;
	}
}
