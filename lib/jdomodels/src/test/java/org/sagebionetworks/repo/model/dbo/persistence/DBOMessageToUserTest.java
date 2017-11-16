package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
	public void testMigrationTranslatorWithStringToCcBcc() throws UnsupportedEncodingException {
		DBOMessageToUserBackup backup = createBackup();
		backup.setTo("Foo <foo@foo.foo>");
		backup.setCc("Bar <bar@bar.bar>");
		backup.setBcc("Baz <baz@baz.baz>");
		MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> translator = new DBOMessageToUser().getTranslator();
		DBOMessageToUser translated = translator.createDatabaseObjectFromBackup(backup);
		testNonToCcBccFieldsEqual(backup, translated);
		assertArrayEquals(backup.getTo().getBytes("UTF-8"), translated.getBytesTo());
		assertArrayEquals(backup.getCc().getBytes("UTF-8"), translated.getBytesCc());
		assertArrayEquals(backup.getBcc().getBytes("UTF-8"), translated.getBytesBcc());
	}

	@Test
	public void testMigrationTranslatorWithByteArrayToCcBcc() throws UnsupportedEncodingException {
		DBOMessageToUserBackup backup = createBackup();
		backup.setBytesTo("Foo <foo@foo.foo>".getBytes("UTF-8"));
		backup.setBytesCc("Bar <bar@bar.bar>".getBytes("UTF-8"));
		backup.setBytesBcc("Baz <baz@baz.baz>".getBytes("UTF-8"));
		MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> translator = new DBOMessageToUser().getTranslator();
		DBOMessageToUser translated = translator.createDatabaseObjectFromBackup(backup);
		testNonToCcBccFieldsEqual(backup, translated);
		assertArrayEquals(backup.getBytesTo(), translated.getBytesTo());
		assertArrayEquals(backup.getBytesCc(), translated.getBytesCc());
		assertArrayEquals(backup.getBytesBcc(), translated.getBytesBcc());
	}

	@Test
	public void testMigrationTranslatorWithMixedTypesToCcBcc() throws UnsupportedEncodingException {
		DBOMessageToUserBackup backup = createBackup();
		backup.setBytesTo("Foo <foo@foo.foo>".getBytes("UTF-8"));
		backup.setCc("Bar <bar@bar.bar>");
		backup.setBytesBcc("Baz <baz@baz.baz>".getBytes("UTF-8"));
		MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> translator = new DBOMessageToUser().getTranslator();
		try {
			translator.createDatabaseObjectFromBackup(backup);
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// As expected
		}
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
		return backup;
	}

	private void testNonToCcBccFieldsEqual(DBOMessageToUserBackup backup, DBOMessageToUser translated) {
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
	}
}

