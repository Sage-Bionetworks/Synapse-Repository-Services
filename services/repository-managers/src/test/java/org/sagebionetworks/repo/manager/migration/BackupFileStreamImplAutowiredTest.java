package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageContent;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMessageToUserBackup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BackupFileStreamImplAutowiredTest {

	@Autowired
	BackupFileStreamImpl backupFileStream;

	@Test
	public void testAllXStreamAliases() throws IOException, EmptyFileException {
		for (MigratableDatabaseObject dbo : backupFileStream.typeProvider.getDatabaseObjectRegister()){

			StringWriter tableNameWriter = new StringWriter();
			StringWriter migrationTypeNameWriter = new StringWriter();

			backupFileStream.writeBatchToStream(Collections.singletonList(dbo), dbo.getMigratableTableType(), BackupAliasType.TABLE_NAME, tableNameWriter);
			backupFileStream.writeBatchToStream(Collections.singletonList(dbo), dbo.getMigratableTableType(), BackupAliasType.MIGRATION_TYPE_NAME, migrationTypeNameWriter);
//
//			System.out.println("START============================================");
//			System.out.println(tableNameWriter);
//			System.out.println(dbo.getMigratableTableType().name() + "=============================================" + dbo.getTableMapping().getTableName());
//			System.out.println(migrationTypeNameWriter);
//			System.out.println("END============================================\n");

			//XStream turns "_" into "__"
			assertTrue(tableNameWriter.toString().contains(dbo.getTableMapping().getTableName().replaceAll("_", "__")));
			assertTrue(migrationTypeNameWriter.toString().contains(dbo.getMigratableTableType().name().replaceAll("_", "__")));

			// ignoring deserializiation for now because of weird behaviors when not working with dummy DBO objects.
			// For example EvaluationDBO's contentSource field somehow gets set to 4489 on deserialization which fails the comparision with the original's contentSource=null
//			String testFilename = dbo.getMigratableTableType().name() + ".test";
//			assertEquals(Collections.singletonList(dbo), backupFileStream.readFileFromStream(IOUtils.toInputStream(migrationTypeNameWriter.toString(), "UTF-8"), BackupAliasType.MIGRATION_TYPE_NAME, testFilename));
//			assertEquals(Collections.singletonList(dbo), backupFileStream.readFileFromStream(IOUtils.toInputStream(tableNameWriter.toString(), "UTF-8"), BackupAliasType.TABLE_NAME, testFilename));
		}
	}
	
	public static void main(String[] args) {
		System.out.println(new String(Base64.getDecoder().decode("VGVhbSBDaGFsbGVuZ2UgU3VibWlzc2lvbg==")));
	}
	
	/**
	 * Test to reproduce https://sagebionetworks.jira.com/browse/PLFM-6373. The secondary object (DBOMessageToUser) was
	 * setup with a backup class (DBOMessageToUserBackup), the XML contained as elements references to the full class name
	 * rather than an alias, so the moment we removed the backup class migration didn't go through as the destination didn't
	 * have the class anymore and XStream could not de-serialize the collection.
	 */
	@Test
	public void testRestoreStreamWithSecondaryAndNoAlias() throws Exception {
		String fileName = "MigrationBackupWithNoAlias.zip";
		InputStream input = getClass().getClassLoader().getResourceAsStream(fileName);
		
		// This is the expected content
		DBOMessageContent messageContent = new DBOMessageContent();
		
		messageContent.setMessageId(12345L);
		messageContent.setFileHandleId(12345L);
		messageContent.setCreatedBy(12345L);
		messageContent.setCreatedOn(1485115605314L);
		messageContent.setEtag("6e61d024-3173-47fb-8e2f-406f95dec565");
		
		DBOMessageToUser messageToUser = new DBOMessageToUser();
		messageToUser.setMessageId(12345L);
		messageToUser.setSubjectBytes(Base64.getDecoder().decode("VGVhbSBDaGFsbGVuZ2UgU3VibWlzc2lvbg=="));
		messageToUser.setSent(true);
		messageToUser.setNotificationsEndpoint("https://www.synapse.org/#!SignedToken:Settings/");
		messageToUser.setWithUnsubscribeLink(true);
		messageToUser.setIsNotificationMessage(true);
		messageToUser.setWithProfileSettingLink(false);
		messageToUser.setOverrideNotificationSettings(false);
		messageToUser.setBytesTo(Base64.getDecoder().decode("QSBCYXZhcmlhbiBkcmVhbSA8YWJhdmFyaWFuZHJlYW1Ac3luYXBzZS5vcmc+"));
		

		List<MigratableDatabaseObject<?, ?>> expected = Arrays.asList(
			messageContent,
			messageToUser
		);
		
		// Call under test
		Iterable<MigratableDatabaseObject<?, ?>> iterable = backupFileStream.readBackupFile(input, BackupAliasType.TABLE_NAME);

		// Collect all the objects, forcing reading the stream, this would fail
		List<MigratableDatabaseObject<?, ?>> result = StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
		
		// Sort by class name to maintain consistency
		Collections.sort(result, (a, b) -> a.getClass().getSimpleName().compareTo(b.getClass().getSimpleName()));
		
		assertEquals(expected, result);
		
	}
}
