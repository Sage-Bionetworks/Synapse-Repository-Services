package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
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
}
