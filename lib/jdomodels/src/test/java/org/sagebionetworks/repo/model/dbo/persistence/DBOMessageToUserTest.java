package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMessageToUserTest {
	@Test
	public void testMigrationTranslator() throws UnsupportedEncodingException {
		DBOMessageToUserBackup backup = createBackup();
		MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> translator = new DBOMessageToUser().getTranslator();
		DBOMessageToUser translated = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(backup.getMessageId(), translated.getMessageId());
		assertEquals(backup.getRootMessageId(), translated.getRootMessageId());
		assertEquals(backup.getInReplyTo(), translated.getInReplyTo());
		assertArrayEquals(backup.getSubjectBytes(), translated.getSubjectBytes());
		assertEquals(backup.getSent(), translated.getSent());
		assertEquals(backup.getNotificationsEndpoint(), translated.getNotificationsEndpoint());
		assertEquals(backup.getProfileSettingEndpoint(), translated.getProfileSettingEndpoint());
		assertEquals(backup.getWithUnsubscribeLink(), translated.getWithUnsubscribeLink());
		assertEquals(backup.getWithProfileSettingLink(), translated.getWithProfileSettingLink());
		assertEquals(backup.getIsNotificationMessage(), translated.getIsNotificationMessage());
		assertArrayEquals(backup.getTo().getBytes("UTF-8"), translated.getTo());
		assertArrayEquals(backup.getCc().getBytes("UTF-8"), translated.getCc());
		assertArrayEquals(backup.getBcc().getBytes("UTF-8"), translated.getBcc());
	}

	private DBOMessageToUserBackup createBackup() throws UnsupportedEncodingException {
		DBOMessageToUserBackup backup = new DBOMessageToUserBackup();
		backup.setMessageId(1L);
		backup.setRootMessageId(1L);
		backup.setInReplyTo(null);
		backup.setSubjectBytes("subject".getBytes("UTF-8"));
		backup.setSent(false);
		backup.setNotificationsEndpoint("notifications.endpoint.org");
		backup.setProfileSettingEndpoint("profile-setting.endpoint.org");
		backup.setWithUnsubscribeLink(false);
		backup.setWithProfileSettingLink(false);
		backup.setIsNotificationMessage(false);
		backup.setTo("Foo <foo@foo.foo>");
		backup.setCc("Bar <bar@bar.bar>");
		backup.setBcc("Baz <baz@baz.baz>");
		return backup;
	}
}
