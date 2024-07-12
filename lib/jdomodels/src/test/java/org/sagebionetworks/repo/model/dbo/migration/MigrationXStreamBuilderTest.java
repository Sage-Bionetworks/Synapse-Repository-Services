package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;

@ExtendWith(MockitoExtension.class)
public class MigrationXStreamBuilderTest {

	/**
	 * The only way to test that the initialize method worked is to actually use the XStream object since there is
	 * currently no way to interrogate the XStream object about what aliases it currently uses
	 */
	@Test
	public void testInitializeAliasTypeToXStreamMap(){
		StubAutoIncrement stubAutoIncrement = new StubAutoIncrement();
		Map<BackupAliasType, UnmodifiableXStream> map = MigrationXStreamBuilder.buildXStream(Collections.singletonList(stubAutoIncrement));
		UnmodifiableXStream tableNameXStream = map.get(BackupAliasType.TABLE_NAME);
		UnmodifiableXStream migrationTypeNameXStream = map.get(BackupAliasType.MIGRATION_TYPE_NAME);

		assertNotSame(tableNameXStream, migrationTypeNameXStream);

		StringWriter tableNameXMLStringWriter = new StringWriter();
		StringWriter migrationTypeXMLStringWriter = new StringWriter();

		tableNameXStream.toXML(stubAutoIncrement, tableNameXMLStringWriter);
		migrationTypeNameXStream.toXML(stubAutoIncrement, migrationTypeXMLStringWriter);

		assertNotEquals(tableNameXMLStringWriter.toString(), migrationTypeNameXStream.toString());
		assertTrue(tableNameXMLStringWriter.toString().contains("STUB"));
		// gave stub fake migration type of VIEW_SCOPE to the stub class.
		// double underscore is because of the way XSTREAM uses to escape underscores:
		// http://x-stream.github.io/faq.html#XML_double_underscores
		assertTrue(migrationTypeXMLStringWriter.toString().contains("VIEW__SCOPE"));
	}
	
	@Test
	public void testInitializeAliasTypeToXStreamMapWithSecondaryTypes(){
		PrimaryClass primaryClass = new PrimaryClass();
		Map<BackupAliasType, UnmodifiableXStream> map = MigrationXStreamBuilder.buildXStream(Collections.singletonList(primaryClass));
		UnmodifiableXStream tableNameXStream = map.get(BackupAliasType.TABLE_NAME);
		UnmodifiableXStream migrationTypeNameXStream = map.get(BackupAliasType.MIGRATION_TYPE_NAME);

		assertNotSame(tableNameXStream, migrationTypeNameXStream);

		StringWriter tableNameXMLStringWriter = new StringWriter();
		StringWriter migrationTypeXMLStringWriter = new StringWriter();
		
		SecondaryClass secondaryClass = new SecondaryClass();

		tableNameXStream.toXML(secondaryClass, tableNameXMLStringWriter);
		migrationTypeNameXStream.toXML(secondaryClass, migrationTypeXMLStringWriter);
		
		String tableXML = tableNameXMLStringWriter.toString();
		String typeXML = migrationTypeXMLStringWriter.toString();

		assertEquals("<" +secondaryClass.getTableMapping().getTableName().replaceAll("_", "__")+ "/>", tableXML);
		assertEquals("<" +secondaryClass.getMigratableTableType().name().replaceAll("_", "__")+ "/>", typeXML);

	}
}
