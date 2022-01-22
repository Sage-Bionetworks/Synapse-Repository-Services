package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;

public class TableIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSql(){
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		String sql = tid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T999( "
				+ "ROW_ID BIGINT NOT NULL, "
				+ "ROW_VERSION BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID))", sql);
	}
	
	@Test
	public void testGetBenefactorColumnNames() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		assertEquals(Collections.emptyList(), tid.getBenefactors());
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithQuery() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		List<String> result = tid.getColumnNamesToAddToSelect(SqlType.query);
		assertEquals(Arrays.asList(TableConstants.ROW_ID, TableConstants.ROW_VERSION), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuild() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tid.getColumnNamesToAddToSelect(SqlType.build);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for tables", message);
	}
	@Test
	public void testGetColumnNamesToAddToSelectWithNull() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tid.getColumnNamesToAddToSelect(null);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for tables", message);
	}
}
