package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

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
}
