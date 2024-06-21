package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.SqlContext;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewIndexDescriptionTest {
	
	@Mock
	private IndexDescriptionLookup mockLookup;
	
	private String definingSql;
	private TableIndexDescription tableIndexDescription;
	private ViewIndexDescription viewIndexDescription;
	private ViewIndexDescription viewIndexDescription2;
	private ViewIndexDescription viewIndexDescriptionV1;
	
	@BeforeEach
	public void before(){
		this.definingSql = "select * from syn999";
		this.tableIndexDescription = new TableIndexDescription(IdAndVersion.parse("syn999"));
		this.viewIndexDescription = new ViewIndexDescription(IdAndVersion.parse("syn999"), TableType.entityview, -1L);
		this.viewIndexDescription2 = new ViewIndexDescription(IdAndVersion.parse("syn888"), TableType.entityview, -1L);
		this.viewIndexDescriptionV1 = new ViewIndexDescription(IdAndVersion.parse("syn999.1"), TableType.entityview, -1L);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleTable() {
		setupLookup(tableIndexDescription);
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, definingSql, mockLookup);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT))", sql);

		verifyLookup(tableIndexDescription);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithSingleView() {
		setupLookup(viewIndexDescription);
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, definingSql, mockLookup);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR__A0 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR__A0))", sql);

		verifyLookup(viewIndexDescription);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleViews() {
		setupLookup(viewIndexDescription, viewIndexDescription2);
		
		definingSql = "select * from syn999 union select * from syn888";
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, definingSql, mockLookup);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR__A0 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR__A1 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR__A0), "
				+ "KEY (ROW_BENEFACTOR__A1))", sql);
		
		verifyLookup(viewIndexDescription, viewIndexDescription2);
	}
	
	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleOfSameView() {
		setupLookup(viewIndexDescription);
		
		definingSql = "select * from syn999 a join syn999 b on (a.id=b.id)";
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, definingSql, mockLookup);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR__A0 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR__A1 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR__A0), "
				+ "KEY (ROW_BENEFACTOR__A1))", sql);
		
		verify(mockLookup, times(2)).getIndexDescription(viewIndexDescription.getIdAndVersion());
	}
	
	@Test
	public void testGetCreateOrUpdateIndexSqlWithMultipleOfSameViewMultipleVersions() {
		setupLookup(viewIndexDescription, viewIndexDescriptionV1);
		
		definingSql = "select * from syn999 union select * from syn999.1";
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId, definingSql, mockLookup);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR__A0 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR__A1 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR__A0), "
				+ "KEY (ROW_BENEFACTOR__A1))", sql);
		
		verifyLookup(viewIndexDescription, viewIndexDescriptionV1);
	}

	@Test
	public void testGetCreateOrUpdateIndexSqlWithMaterializedViewDependency() {
		setupLookup(viewIndexDescription, viewIndexDescription2);

		MaterializedViewIndexDescription dependency = new MaterializedViewIndexDescription(IdAndVersion.parse("456"),
				"select * from syn999 union select * from syn888", mockLookup);

		setupLookup(dependency);
		
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				"select * from syn456", mockLookup);
		// call under test
		String sql = mid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T123( "
				+ "ROW_ID BIGINT NOT NULL AUTO_INCREMENT, "
				+ "ROW_VERSION BIGINT NOT NULL DEFAULT 0, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "ROW_BENEFACTOR__A0__A0 BIGINT NOT NULL, "
				+ "ROW_BENEFACTOR__A1__A0 BIGINT NOT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT), "
				+ "KEY (ROW_BENEFACTOR__A0__A0), "
				+ "KEY (ROW_BENEFACTOR__A1__A0))", sql);
		
		verifyLookup(viewIndexDescription, viewIndexDescription2, dependency);
	}

	@Test
	public void testGetBenefactorColumnNames() {
		setupLookup(viewIndexDescription, viewIndexDescriptionV1);
		
		
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				"select * from syn999.1 a join syn999 b on (a.id=b.id)", mockLookup);
		List<BenefactorDescription> expected = Arrays.asList(
				new BenefactorDescription("ROW_BENEFACTOR__A0", ObjectType.ENTITY),
				new BenefactorDescription("ROW_BENEFACTOR__A1", ObjectType.ENTITY));
		// call under test
		assertEquals(expected, mid.getBenefactors());
		
		verifyLookup(viewIndexDescription, viewIndexDescriptionV1);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithQueryAndNonaggregate() {
		
		setupLookup(viewIndexDescription, viewIndexDescription2);
		
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn888 b on (a.id=b.id)", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.query, includeEtag, isAggregate);
		assertEquals(Arrays.asList(new ColumnToAdd(materializedViewId, ROW_ID), new ColumnToAdd(materializedViewId, ROW_VERSION)), result);
		
		verifyLookup(viewIndexDescription, viewIndexDescription2);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithQueryAndAggregate() {
		setupLookup(viewIndexDescription, viewIndexDescription2);
		
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn888 b on (a.id=b.id)", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.query, includeEtag, isAggregate);
		assertEquals(Collections.emptyList(), result);
		
		verifyLookup(viewIndexDescription, viewIndexDescription2);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndNonAggregate() {
		setupLookup(viewIndexDescription, viewIndexDescriptionV1);
		
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn999.1 b on (a.id=b.id)", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		assertEquals(
				Arrays.asList(new ColumnToAdd(IdAndVersion.parse("syn999"), "IFNULL( a.ROW_BENEFACTOR , -1)"),
						new ColumnToAdd(IdAndVersion.parse("syn999.1"), "IFNULL( b.ROW_BENEFACTOR , -1)")),
				result);
		
		verifyLookup(viewIndexDescription, viewIndexDescriptionV1);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndAggregateWithViewDependency() {
		setupLookup(viewIndexDescription, viewIndexDescriptionV1);
		
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn999.1 b on (a.id=b.id)", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		String message = assertThrows(IllegalArgumentException.class, ()->{
			mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		}).getMessage();
		assertEquals(message, TableConstants.DEFINING_SQL_WITH_GROUP_BY_ERROR);
		verifyLookup(viewIndexDescription, viewIndexDescriptionV1);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndAggregateWithTableDependency() {
		setupLookup(tableIndexDescription);
		
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		assertEquals(Collections.emptyList(), result);
		verifyLookup(tableIndexDescription);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuildAndDuplicateViewId() {
		setupLookup(viewIndexDescription);
		
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn999 b on (a.id=b.id)", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = mid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		assertEquals(
				Arrays.asList(
						new ColumnToAdd(IdAndVersion.parse("syn999"), "IFNULL( a.ROW_BENEFACTOR , -1)"),
						new ColumnToAdd(IdAndVersion.parse("syn999"), "IFNULL( b.ROW_BENEFACTOR , -1)")),
				result);
		
		verify(mockLookup, times(2)).getIndexDescription(viewIndexDescription.getIdAndVersion());
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithNull() {
		setupLookup(viewIndexDescription, viewIndexDescription2);
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn888 b on (a.id=b.id)", mockLookup);
		boolean includeEtag = true;
		boolean isAggregate = false;
		assertThrows(IllegalArgumentException.class, ()->{
			mid.getColumnNamesToAddToSelect(null, includeEtag, isAggregate);
		});
		
		verifyLookup(tableIndexDescription, viewIndexDescription2);
	}
	
	@Test
	public void testGetDependencies() {
		setupLookup(viewIndexDescription, viewIndexDescription2);
		IdAndVersion materializedViewId = IdAndVersion.parse("syn123");
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(materializedViewId,
				"select * from syn999 a join syn888 b on (a.id=b.id)", mockLookup);
		// put in IdAndVersion order
		List<IndexDescription> expectedDependencies = Arrays.asList(
				viewIndexDescription2,
				viewIndexDescription
				);
		assertEquals(expectedDependencies, mid.getDependencies());
		
		verifyLookup(tableIndexDescription, viewIndexDescription2);
	}
	
	@Test
	public void testSupportQueryCache() {
		setupLookup(viewIndexDescription);
		MaterializedViewIndexDescription mid = new MaterializedViewIndexDescription(IdAndVersion.parse("syn123"),
				definingSql, mockLookup);

		// Call under test
		assertFalse(mid.supportQueryCache());
	}
	
	public void setupLookup(IndexDescription...all){
		Arrays.stream(all).forEach(d->when(mockLookup.getIndexDescription(d.getIdAndVersion())).thenReturn(d));
	}
	
	public void verifyLookup(IndexDescription...all){
		Arrays.stream(all).forEach(d->verify(mockLookup).getIndexDescription(d.getIdAndVersion()));
	}
}
