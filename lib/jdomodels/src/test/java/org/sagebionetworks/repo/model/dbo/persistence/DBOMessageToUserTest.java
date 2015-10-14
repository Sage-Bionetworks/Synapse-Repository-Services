package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;

public class DBOMessageToUserTest {
	
	private static MigratableTableTranslation<DBOMessageToUser, DBOMessageToUser> 
		TRANSLATOR = (new DBOMessageToUser()).getTranslator();
	
	@Test
	public void testBackupRoundTripSubject() throws Exception {
		DBOMessageToUserBackup backup = new DBOMessageToUserBackup();
		backup.setBcc("bcc");
		backup.setCc("cc");
		backup.setInReplyTo(111L);
		backup.setMessageId(222L);
		backup.setNotificationsEndpoint("synapse.org");
		backup.setRootMessageId(333L);
		backup.setSent(true);
		backup.setSubject("foo");
		backup.setTo("to");
		DBOMessageToUser dbo = TRANSLATOR.createDatabaseObjectFromBackup(backup);
		assertEquals("bcc", dbo.getBcc());
		assertEquals("cc", dbo.getCc());
		assertEquals(new Long(111L), dbo.getInReplyTo());
		assertEquals(new Long(222L), dbo.getMessageId());
		assertEquals("synapse.org", dbo.getNotificationsEndpoint());
		assertEquals(new Long(333L), dbo.getRootMessageId());
		assertEquals(true, dbo.getSent());
		assertEquals("foo", new String(dbo.getSubjectBytes(), "utf-8"));
		assertEquals("to", dbo.getTo());
	}

	@Test
	public void testBackupRoundTripSubjectBytes() throws Exception {
		DBOMessageToUserBackup backup = new DBOMessageToUserBackup();
		backup.setBcc("bcc");
		backup.setCc("cc");
		backup.setInReplyTo(111L);
		backup.setMessageId(222L);
		backup.setNotificationsEndpoint("synapse.org");
		backup.setRootMessageId(333L);
		backup.setSent(true);
		backup.setSubjectBytes("foo".getBytes("utf-8"));
		backup.setTo("to");
		DBOMessageToUser dbo = TRANSLATOR.createDatabaseObjectFromBackup(backup);
		assertEquals("bcc", dbo.getBcc());
		assertEquals("cc", dbo.getCc());
		assertEquals(new Long(111L), dbo.getInReplyTo());
		assertEquals(new Long(222L), dbo.getMessageId());
		assertEquals("synapse.org", dbo.getNotificationsEndpoint());
		assertEquals(new Long(333L), dbo.getRootMessageId());
		assertEquals(true, dbo.getSent());
		assertEquals("foo", new String(dbo.getSubjectBytes(), "utf-8"));
		assertEquals("to", dbo.getTo());
	}

}
