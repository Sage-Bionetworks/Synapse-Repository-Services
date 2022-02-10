package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.model.SqlContext;

public class MaterializedViewIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleTable() {
		List<IndexDescription> dependencies = Arrays.asList(new TableIndexDescription(IdAndVersion.parse("syn999")));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123(" + " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0," + " PRIMARY KEY (ROW_ID))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleView() {
		List<IndexDescription> dependencies = Arrays
				.asList(new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123("
				+ " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0,"
				+ " ROW_BENEFACTOR_T999 BIGINT NOT NULL,"
				+ " PRIMARY KEY (ROW_ID),"
				+ " KEY (ROW_BENEFACTOR_T999))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleViews() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888"), EntityType.entityview));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123("
				+ " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0,"
				+ " ROW_BENEFACTOR_T888 BIGINT NOT NULL,"
				+ " ROW_BENEFACTOR_T999 BIGINT NOT NULL,"
				+ " PRIMARY KEY (ROW_ID),"
				+ " KEY (ROW_BENEFACTOR_T888),"
				+ " KEY (ROW_BENEFACTOR_T999))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithMaterializedViewDependency() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888"), EntityType.entityview));
		MaterializedViewIndexDescription dependency = new MaterializedViewIndexDescription(IdAndVersion.parse("456"),
				dependencies);
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				Arrays.asList(dependency));
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123("
				+ " ROW_ID BIGINT NOT NULL AUTO_INCREMENT,"
				+ " ROW_VERSION BIGINT NOT NULL DEFAULT 0,"
				+ " ROW_BENEFACTOR_T888_T456 BIGINT NOT NULL,"
				+ " ROW_BENEFACTOR_T999_T456 BIGINT NOT NULL,"
				+ " PRIMARY KEY (ROW_ID),"
				+ " KEY (ROW_BENEFACTOR_T888_T456),"
				+ " KEY (ROW_BENEFACTOR_T999_T456))", sql);
	}

	@Test
	public void testGetBenefactorColumnNames() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888.2"), EntityType.entityview));
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				dependencies);
		List<BenefactorDescription> expected = Arrays.asList(
				new BenefactorDescription("ROW_BENEFACTOR_T888_2", ObjectType.ENTITY),
				new BenefactorDescription("ROW_BENEFACTOR_T999", ObjectType.ENTITY));
		// call under test
		assertEquals(expected, mid.getBenefactors());
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithQuery() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888"), EntityType.entityview));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		List<String> result = mid.getColumnNamesToAddToSelect(SqlContext.query, true);
		assertEquals(Arrays.asList(ROW_ID, ROW_VERSION), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuild() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888.3"), EntityType.entityview));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		List<String> result = mid.getColumnNamesToAddToSelect(SqlContext.build, true);
		assertEquals(Arrays.asList("T888_3.ROW_BENEFACTOR", "T999.ROW_BENEFACTOR"), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithNull() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888"), EntityType.entityview));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		assertThrows(IllegalArgumentException.class, ()->{
			mid.getColumnNamesToAddToSelect(null, true);
		});
	}
	
	@Test
	public void testGetDependencies() {
		List<IndexDescription> dependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888.2"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888.1"), EntityType.entityview));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// put in IdAndVersion order
		List<IndexDescription> expectedDependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn888"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888.1"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn888.2"), EntityType.entityview),
				new ViewIndexDescription(IdAndVersion.parse("syn999"), EntityType.entityview));
		assertEquals(expectedDependencies, mid.getDependencies());
	}
}
