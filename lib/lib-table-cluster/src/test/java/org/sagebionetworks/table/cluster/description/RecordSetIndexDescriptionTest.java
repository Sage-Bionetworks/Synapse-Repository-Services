package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.model.SqlContext;

@ExtendWith(MockitoExtension.class)
public class RecordSetIndexDescriptionTest {

	@Mock
	private IndexDescriptionLookup mockLookup;

	@Test
	public void testGetCreateOrUpdateIndexSql() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.3"), 3L);
		// call under test
		String sql = rid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T999_3( "
				+ "ROW_ID BIGINT NOT NULL, "
				+ "ROW_VERSION BIGINT NOT NULL, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT))", sql);
	}

	@Test
	public void testGetBenefactors() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		// call under test
		assertEquals(Collections.emptyList(), rid.getBenefactors());
	}

	@Test
	public void testGetTableType() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		// call under test
		assertEquals(TableType.recordset, rid.getTableType());
	}

	@Test
	public void testGetDependencies() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		// call under test
		assertEquals(Collections.emptyList(), rid.getDependencies());
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithQueryWithNonAggregate() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn999.1");
		RecordSetIndexDescription rid = new RecordSetIndexDescription(idAndVersion, 1L);
		// call under test
		List<ColumnToAdd> result = rid.getColumnNamesToAddToSelect(SqlContext.query, true, false);
		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION)),
				result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithQueryWithAggregate() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		// call under test
		List<ColumnToAdd> result = rid.getColumnNamesToAddToSelect(SqlContext.query, true, true);
		assertEquals(Collections.emptyList(), result);
	}

	@Test
	public void testGetColumnNamesToAddToSelectWithBuild() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			rid.getColumnNamesToAddToSelect(SqlContext.build, true, false);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for record sets", message);
	}

	@Test
	public void testGetLastTableChangeNumber() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.7"), 7L);
		// call under test
		assertEquals(Optional.of(7L), rid.getLastTableChangeNumber());
	}

	@Test
	public void testGetLastTableChangeNumberWithNullConstructor() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"));
		// call under test
		assertEquals(Optional.empty(), rid.getLastTableChangeNumber());
	}

	@Test
	public void testGetTableHashChangesWithVersion() {
		RecordSetIndexDescription v1 = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		RecordSetIndexDescription v2 = new RecordSetIndexDescription(IdAndVersion.parse("syn999.2"), 2L);
		// call under test
		assertNotEquals(v1.getTableHash(), v2.getTableHash());
		assertEquals(DigestUtils.md5Hex("+syn999.1-1"), v1.getTableHash());
		assertEquals(DigestUtils.md5Hex("+syn999.2-2"), v2.getTableHash());
	}

	@Test
	public void testGetTableHashRecursiveInMaterializedView() {
		// MV that joins two RecordSet versions should hash the RecordSet change numbers.
		RecordSetIndexDescription recordSet = new RecordSetIndexDescription(IdAndVersion.parse("syn1.4"), 4L);
		when(mockLookup.getIndexDescription(IdAndVersion.parse("syn1.4"))).thenReturn(recordSet);
		MaterializedViewIndexDescription mv = new MaterializedViewIndexDescription(IdAndVersion.parse("syn99"),
				"select * from syn1.4", mockLookup);
		// call under test
		assertEquals(DigestUtils.md5Hex("+syn1.4-4"), mv.getTableHash());
	}

	@Test
	public void testSupportQueryCache() {
		RecordSetIndexDescription rid = new RecordSetIndexDescription(IdAndVersion.parse("syn999.1"), 1L);
		// call under test
		assertFalse(rid.supportQueryCache());
	}

	@Test
	public void testEquals() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn999.1");
		RecordSetIndexDescription a = new RecordSetIndexDescription(idAndVersion, 1L);
		RecordSetIndexDescription b = new RecordSetIndexDescription(idAndVersion, 1L);
		RecordSetIndexDescription c = new RecordSetIndexDescription(idAndVersion, 2L);
		// call under test
		assertEquals(a, b);
		assertNotEquals(a, c);
	}

}
