package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.SqlContext;

public class MaterializedViewIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleTable() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new TableIndexDescription(IdAndVersion.parse("syn999"))));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleView() {
		List<TableDependency> dependencies = Arrays.asList(new TableDependency().withIndexDescription(
				new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR_T999 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR_T999))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleViews() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR_T888 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR_T999 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR_T888), "
				+ "KEY (ROW_BENEFACTOR_T999))", sql);
	}
	
	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleOfSameView() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)).withTableAlias("a"),
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)).withTableAlias("b"));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR_A BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR_B BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR_A), "
				+ "KEY (ROW_BENEFACTOR_B))", sql);
	}
	
	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleOfSameViewMultipleVersions() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999.1"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR_T999 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR_T999_1 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR_T999), "
				+ "KEY (ROW_BENEFACTOR_T999_1))", sql);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithMaterializedViewDependency() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L)));
		MaterializedViewIndexDescription dependency = new MaterializedViewIndexDescription(IdAndVersion.parse("456"),
				dependencies);
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				Arrays.asList(new TableDependency().withIndexDescription(dependency)));
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR_T888_T456 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR_T999_T456 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR_T888_T456), "
				+ "KEY (ROW_BENEFACTOR_T999_T456))", sql);
	}

	@Test
	public void testGetBenefactorColumnNames() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn888.2"), TableType.entityview, -1L)));
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				dependencies);
		List<BenefactorDescription> expected = Arrays.asList(
				new BenefactorDescription("ROW_BENEFACTOR_T888_2", ObjectType.ENTITY),
				new BenefactorDescription("ROW_BENEFACTOR_T999", ObjectType.ENTITY));
		// call under test
		assertEquals(expected, mid.getBenefactors());
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithQueryAndNonaggregate() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(
						new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.query, includeEtag, isAggregate);
		assertEquals(Arrays.asList(new ColumnToAdd(materializedViewId, ROW_ID), new ColumnToAdd(materializedViewId, ROW_VERSION)), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithQueryAndAggregate() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.query, includeEtag, isAggregate);
		assertEquals(Collections.emptyList(), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndNonAggregate() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888.3"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		assertEquals(
				Arrays.asList(new ColumnToAdd(IdAndVersion.parse("syn888.3"), "IFNULL( T888_3.ROW_BENEFACTOR , -1)"),
						new ColumnToAdd(IdAndVersion.parse("syn999"), "IFNULL( T999.ROW_BENEFACTOR , -1)")),
				result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndAggregateWithViewDependency() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888.3"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		String message = assertThrows(IllegalArgumentException.class, ()->{
			mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		}).getMessage();
		assertEquals(message, TableConstants.DEFINING_SQL_WITH_GROUP_BY_ERROR);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndAggregateWithTableDependency() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new TableIndexDescription(IdAndVersion.parse("syn999"))));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		assertEquals(Collections.emptyList(), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndDuplicateViewId() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency()
						.withIndexDescription(
								new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L))
						.withTableAlias("foo"),
				new TableDependency()
						.withIndexDescription(
								new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L))
						.withTableAlias("bar"));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		assertEquals(
				Arrays.asList(
						new ColumnToAdd(IdAndVersion.parse("syn999"), "IFNULL( foo.ROW_BENEFACTOR , -1)"),
						new ColumnToAdd(IdAndVersion.parse("syn999"), "IFNULL( bar.ROW_BENEFACTOR , -1)")),
				result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithNull() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		boolean includeEtag = true;
		boolean isAggregate = false;
		assertThrows(IllegalArgumentException.class, ()->{
			mid.getColumnNamesToAddToSelect(null, includeEtag, isAggregate);
		});
	}
	
	@Test
	public void testGetDependencies() {
		List<TableDependency> dependencies = Arrays.asList(
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888.2"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L)),
				new TableDependency().withIndexDescription(new ViewIndexDescription(IdAndVersion.parse("syn888.1"), TableType.entityview, -1L)));
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, dependencies);
		// put in IdAndVersion order
		List<IndexDescription> expectedDependencies = Arrays.asList(
				new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L),
				new ViewIndexDescription(IdAndVersion.parse("syn888.1"), TableType.entityview, -1L),
				new ViewIndexDescription(IdAndVersion.parse("syn888.2"), TableType.entityview, -1L),
				new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L));
		assertEquals(expectedDependencies, mid.getDependencies());
		assertEquals(dependencies, mid.getFullDependencies());
	}
	
	@Test
	public void testSupportQueryCache() {
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"), Collections.emptyList());
		
		// Call under test
		assertFalse(mid.supportQueryCache());
	}
}
