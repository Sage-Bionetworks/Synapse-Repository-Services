package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;

public class ViewIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSql(){
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		String sql = vid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T999( "
				+ "ROW_ID BIGINT NOT NULL, "
				+ "ROW_VERSION BIGINT NOT NULL, "
				+ "ROW_ETAG varchar(36) NOT NULL, "
				+ "ROW_BENEFACTOR BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "KEY `IDX_ETAG` (ROW_ETAG), "
				+ "KEY `IDX_BENEFACTOR` (ROW_BENEFACTOR))", sql);
	}
	
	@Test
	public void testGetBenefactorColumnNames() {
		ViewIndexDescription vid = new ViewIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		assertEquals(Collections.singletonList(TableConstants.ROW_BENEFACTOR), vid.getBenefactorColumnNames());
	}
}
