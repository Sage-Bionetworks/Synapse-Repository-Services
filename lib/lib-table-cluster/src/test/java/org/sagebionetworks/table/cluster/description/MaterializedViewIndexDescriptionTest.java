package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

public class MaterializedViewIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleTable() {
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn999")));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123("
				+ " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0," 
				+ " PRIMARY KEY (ROW_ID))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleView() {
		List<IndexDescription> dependencies = Arrays.asList(new ViewIndexDescription(IdAndVersion.parse("syn999")));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123("
				+ " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0,"
				+ " ROW_BENEFACTOR_A0 BIGINT NOT NULL,"
				+ " PRIMARY KEY (ROW_ID),"
				+ " KEY (ROW_BENEFACTOR_A0))", sql);
	}
	
	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleViews() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999")),
				new ViewIndexDescription(IdAndVersion.parse("syn888"))
		);
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0,"
				+ " ROW_BENEFACTOR_A0 BIGINT NOT NULL,"
				+ " ROW_BENEFACTOR_A1 BIGINT NOT NULL,"
				+ " PRIMARY KEY (ROW_ID),"
				+ " KEY (ROW_BENEFACTOR_A0),"
				+ " KEY (ROW_BENEFACTOR_A1))", sql);
	}
	
	@Test
	public void testGetCreateOrUpdateIndexSqlWithMaterializedViewDependency() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999")),
				new ViewIndexDescription(IdAndVersion.parse("syn888"))
		);
		MaterializedViewIndexDescription dependency = new MaterializedViewIndexDescription(IdAndVersion.parse("456"), dependencies);
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"), Arrays.asList(dependency));
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123("
				+ " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0,"
				+ " ROW_BENEFACTOR_A0_A0 BIGINT NOT NULL,"
				+ " ROW_BENEFACTOR_A1_A0 BIGINT NOT NULL,"
				+ " PRIMARY KEY (ROW_ID),"
				+ " KEY (ROW_BENEFACTOR_A0_A0),"
				+ " KEY (ROW_BENEFACTOR_A1_A0))", sql);
	}
	
	@Test
	public void testGetBenefactorColumnNames() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999")),
				new ViewIndexDescription(IdAndVersion.parse("syn888"))
		);
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"), dependencies);
		// call under test
		assertEquals(Arrays.asList("ROW_BENEFACTOR_A0","ROW_BENEFACTOR_A1"), mid.getBenefactorColumnNames());
	}
}
