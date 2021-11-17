package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.TableNameCorrelation;

public class TableInfoTest {

	@Test
	public void tesConstructorWithNoVersionNoAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation);
		assertEquals("syn123", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123"), info.getTableIdAndVersion());
		assertEquals("T123", info.getTranslatedTableName());
		assertEquals(null, info.getTableAlias().orElse(null));
	}
	
	@Test
	public void tesConstructorWithNoVersionWithAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123 t").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation);
		assertEquals("syn123", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123"), info.getTableIdAndVersion());
		assertEquals("T123", info.getTranslatedTableName());
		assertEquals("t", info.getTableAlias().orElse(null));
	}
	
	@Test
	public void tesConstructorWithVersionNoAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123.4").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation);
		assertEquals("syn123.4", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123.4"), info.getTableIdAndVersion());
		assertEquals("T123_4", info.getTranslatedTableName());
		assertEquals(null, info.getTableAlias().orElse(null));
	}
	
	@Test
	public void tesConstructorWithVersionWithAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123.4 as \"has space\"").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation);
		assertEquals("syn123.4", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123.4"), info.getTableIdAndVersion());
		assertEquals("T123_4", info.getTranslatedTableName());
		assertEquals("\"has space\"", info.getTableAlias().orElse(null));
	}
	
	@Test
	public void testConstructorWithNull() {
		TableNameCorrelation tableNameCorrelation = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new TableInfo(tableNameCorrelation);
		}).getMessage();
		assertEquals("TableNameCorrelation is required.", message);
	}

}
