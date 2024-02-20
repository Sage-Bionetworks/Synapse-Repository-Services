package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.table.query.model.SqlContext;


public class TableIndexDescriptionTest {

	@Test
	public void testGetCreateOrUpdateIndexSql(){
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		String sql = tid.getCreateOrUpdateIndexSql();
		assertEquals("CREATE TABLE IF NOT EXISTS T999( "
				+ "ROW_ID BIGINT NOT NULL, "
				+ "ROW_VERSION BIGINT NOT NULL, "
				+ "ROW_SEARCH_CONTENT MEDIUMTEXT NULL, "
				+ "PRIMARY KEY (ROW_ID), "
				+ "FULLTEXT INDEX `ROW_SEARCH_CONTENT_INDEX` (ROW_SEARCH_CONTENT))", sql);
	}
	
	@Test
	public void testGetBenefactorColumnNames() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		// call under test
		assertEquals(Collections.emptyList(), tid.getBenefactors());
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithQueryWithNonAggregate() {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn999");
		TableIndexDescription tid = new TableIndexDescription(idAndVersion);
		boolean includeEtag = true;
		boolean isAggregate = false;
		// call under test
		List<ColumnToAdd> result = tid.getColumnNamesToAddToSelect(SqlContext.query, includeEtag, isAggregate);
		assertEquals(Arrays.asList(new ColumnToAdd(idAndVersion, ROW_ID), new ColumnToAdd(idAndVersion, ROW_VERSION)), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithQueryWithAggregate() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		boolean includeEtag = true;
		boolean isAggregate = true;
		// call under test
		List<ColumnToAdd> result = tid.getColumnNamesToAddToSelect(SqlContext.query, includeEtag, isAggregate);
		assertEquals(Collections.emptyList(), result);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithBuild() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		boolean includeEtag = true;
		boolean isAggregate = false;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tid.getColumnNamesToAddToSelect(SqlContext.build, includeEtag, isAggregate);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for tables", message);
	}
	
	@Test
	public void testGetColumnNamesToAddToSelectWithNull() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		boolean includeEtag = true;
		boolean isAggregate = false;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			tid.getColumnNamesToAddToSelect(null, includeEtag, isAggregate);
		}).getLocalizedMessage();
		assertEquals("Only 'query' is supported for tables", message);
	}
	
	@Test
	public void testGetDependencies() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		assertEquals(Collections.emptyList(), tid.getDependencies());
	}
	
	@Test
	public void testGetLastTableChangeNumber() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"), 12L);
		assertEquals(Optional.of(Long.valueOf(12L)), tid.getLastTableChangeNumber());
	}
	
	@Test
	public void testGetLastTableChangeNumberWithNull() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"), null);
		assertEquals(Optional.empty(), tid.getLastTableChangeNumber());
	}
	
	@Test
	public void testGetLastTableChangeNumberWithNullConstructor() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		assertEquals(Optional.empty(), tid.getLastTableChangeNumber());
	}
	
	@Test
	public void testGetTableHash() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"), 12L);
		String expectedHash = DigestUtils.md5Hex("+syn999-12");
		assertEquals(expectedHash, tid.getTableHash());
	}
	
	@Test
	public void testGetTableHashWithNull() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"), null);
		String expectedHash = DigestUtils.md5Hex("");
		assertEquals(expectedHash, tid.getTableHash());
	}
	
	@Test
	public void testGetTableHashRecursive() {
		TableIndexDescription one = new TableIndexDescription(IdAndVersion.parse("syn1.1"), 1L);
		TableIndexDescription two = new TableIndexDescription(IdAndVersion.parse("syn1.2"), 2L);
		TableIndexDescription three = new TableIndexDescription(IdAndVersion.parse("syn3"), 3L);
		MaterializedViewIndexDescription mv1 = new MaterializedViewIndexDescription(IdAndVersion.parse("syn4"), List.of(one, two));
		MaterializedViewIndexDescription mv2 = new MaterializedViewIndexDescription(IdAndVersion.parse("syn5"), List.of(mv1, three));
		// call under test
		String expectedHash = DigestUtils.md5Hex("+syn3-3+syn1.1-1+syn1.2-2");
		assertEquals(expectedHash, mv2.getTableHash());
	}
	
	@Test
	public void testSupportQueryCache() {
		TableIndexDescription tid = new TableIndexDescription(IdAndVersion.parse("syn999"));
		
		// Call under test
		assertFalse(tid.supportQueryCache());
	}
}
