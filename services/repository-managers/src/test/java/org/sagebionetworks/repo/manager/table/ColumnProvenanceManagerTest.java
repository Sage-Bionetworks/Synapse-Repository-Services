package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.table.ColumnProvenanceDao;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnProvenance;
import org.sagebionetworks.repo.model.table.ColumnProvenanceEntry;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.SourceColumnReference;

@ExtendWith(MockitoExtension.class)
public class ColumnProvenanceManagerTest {

	@Mock
	private ColumnProvenanceDao mockColumnProvenanceDao;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private TableManagerSupport mockTableManagerSupport;

	@InjectMocks
	private ColumnProvenanceManager manager;

	@Captor
	private ArgumentCaptor<ColumnProvenance> provenanceCaptor;

	private final IdAndVersion objectId = IdAndVersion.parse("syn999");

	// syn123 source schema: foo(111), bar(222)
	private final ColumnModel foo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.INTEGER);
	private final ColumnModel bar = TableModelTestUtils.createColumn(222L, "bar", ColumnType.STRING);
	private final List<ColumnModel> syn123Schema = Arrays.asList(foo, bar);

	// syn456 source schema: baz(333), qux(444)
	private final ColumnModel baz = TableModelTestUtils.createColumn(333L, "baz", ColumnType.INTEGER);
	private final ColumnModel qux = TableModelTestUtils.createColumn(444L, "qux", ColumnType.STRING);
	private final List<ColumnModel> syn456Schema = Arrays.asList(baz, qux);

	// --- lifecycle ---

	@Test
	public void testGetColumnProvenanceWithCacheHit() {
		ColumnProvenance cached = new ColumnProvenance().setObjectId("syn999");
		when(mockColumnProvenanceDao.getColumnProvenance(objectId)).thenReturn(Optional.of(cached));

		// call under test
		Optional<ColumnProvenance> result = manager.getColumnProvenance(objectId);

		assertEquals(Optional.of(cached), result);
		// A hit must not translate SQL or write.
		verifyNoInteractions(mockNodeDao, mockColumnModelManager, mockTableManagerSupport);
		verify(mockColumnProvenanceDao, never()).saveColumnProvenance(any(), any());
	}

	@Test
	public void testGetColumnProvenanceWithNonDefiningSqlObject() {
		when(mockColumnProvenanceDao.getColumnProvenance(objectId)).thenReturn(Optional.empty());
		when(mockNodeDao.getDefiningSql(objectId)).thenReturn(Optional.empty());

		// call under test
		Optional<ColumnProvenance> result = manager.getColumnProvenance(objectId);

		assertEquals(Optional.empty(), result);
		verify(mockColumnProvenanceDao, never()).saveColumnProvenance(any(), any());
	}

	@Test
	public void testGetColumnProvenanceWithCacheMissComputesAndSaves() {
		when(mockColumnProvenanceDao.getColumnProvenance(objectId)).thenReturn(Optional.empty());
		when(mockNodeDao.getDefiningSql(objectId)).thenReturn(Optional.of("select foo, count(bar) as bar_count from syn123 group by foo"));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		// The bound schema carries the output column ids and the names the defining SQL produces.
		when(mockColumnModelManager.getTableSchema(objectId)).thenReturn(Arrays.asList(
				new ColumnModel().setId("501").setName("foo"),
				new ColumnModel().setId("502").setName("bar_count")));

		ColumnProvenance expected = new ColumnProvenance().setObjectId("syn999").setVersionNumber(null).setColumns(Arrays.asList(
				new ColumnProvenanceEntry().setOutputColumnId("501").setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "111"))),
				new ColumnProvenanceEntry().setOutputColumnId("502").setDerivationKind(DerivationKind.AGGREGATE)
						.setSetFunctionType("COUNT")
						.setInputs(Collections.singletonList(source("syn123", null, "222")))));

		// call under test
		Optional<ColumnProvenance> result = manager.getColumnProvenance(objectId);

		assertEquals(Optional.of(expected), result);
		verify(mockColumnProvenanceDao).saveColumnProvenance(objectId, expected);
	}

	@Test
	public void testComputeColumnProvenanceWithBoundSchemaSizeMismatch() {
		when(mockColumnProvenanceDao.getColumnProvenance(objectId)).thenReturn(Optional.empty());
		when(mockNodeDao.getDefiningSql(objectId)).thenReturn(Optional.of("select foo, count(bar) from syn123 group by foo"));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		// The bound schema disagrees with the two computed select columns - a corrupted system
		// invariant that must fail loudly as an IllegalStateException (HTTP 500), not a 400.
		when(mockColumnModelManager.getTableSchema(objectId))
				.thenReturn(Collections.singletonList(new ColumnModel().setId("501").setName("foo")));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.getColumnProvenance(objectId);
		}).getMessage();
		assertEquals("Expected 1 bound columns to match the defining SQL of syn999 but computed 2", message);

		verify(mockColumnProvenanceDao, never()).saveColumnProvenance(any(), any());
	}

	@Test
	public void testComputeColumnProvenanceWithBoundSchemaNameMismatch() {
		when(mockColumnProvenanceDao.getColumnProvenance(objectId)).thenReturn(Optional.empty());
		when(mockNodeDao.getDefiningSql(objectId)).thenReturn(Optional.of("select foo, count(bar) as bar_count from syn123 group by foo"));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		// The bound name at position 1 no longer matches the name the defining SQL produces - the
		// schema and SQL have drifted, which must fail loudly as an IllegalStateException (HTTP 500).
		when(mockColumnModelManager.getTableSchema(objectId)).thenReturn(Arrays.asList(
				new ColumnModel().setId("501").setName("foo"),
				new ColumnModel().setId("502").setName("stale_name")));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.getColumnProvenance(objectId);
		}).getMessage();
		assertEquals("The bound schema of syn999 is out of sync with its defining SQL at column 1:"
				+ " bound column 'stale_name' but the defining SQL produced 'bar_count'", message);

		verify(mockColumnProvenanceDao, never()).saveColumnProvenance(any(), any());
	}

	@Test
	public void testGetColumnProvenanceWithNullObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getColumnProvenance(null);
		}).getMessage();
		assertEquals("object is required.", message);
	}

	@Test
	public void testInvalidate() {
		// call under test
		manager.invalidate(objectId);
		verify(mockColumnProvenanceDao).clear(objectId);
	}

	@Test
	public void testInvalidateWithNullObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.invalidate(null);
		}).getMessage();
		assertEquals("object is required.", message);
	}

	@Test
	public void testBindSchemaAndInvalidate() {
		List<String> schemaIds = Arrays.asList("111", "222");

		// call under test
		manager.bindSchemaAndInvalidate(schemaIds, objectId);

		// The schema must be bound before the stale provenance is discarded.
		InOrder inOrder = inOrder(mockColumnModelManager, mockColumnProvenanceDao);
		inOrder.verify(mockColumnModelManager).bindColumnsToVersionOfObject(schemaIds, objectId);
		inOrder.verify(mockColumnProvenanceDao).clear(objectId);
	}

	// --- classification (computeEntries) ---

	@Test
	public void testComputeEntriesWithIdentity() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select foo from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithRenameIsStillIdentity() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - an 'as' alias renames the output but the value is still a bare reference.
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select foo as foo_renamed from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithExpression() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select foo + 1 from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithAggregateCount() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - count(*) has no input column.
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select count(*) from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.AGGREGATE)
				.setSetFunctionType("COUNT").setInputs(Collections.emptyList())), entries);
	}

	@Test
	public void testComputeEntriesWithAggregateMax() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select max(bar) from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.AGGREGATE)
				.setSetFunctionType("MAX").setInputs(Collections.singletonList(source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithLiteral() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - a constant has no input columns.
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select 'tag' as tag from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.LITERAL)
				.setInputs(Collections.emptyList())), entries);
	}

	@Test
	public void testComputeEntriesWithSelectStarExpandsToEachColumn() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - 'select *' is expanded to one identity entry per source column.
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select * from syn123");

		assertEquals(Arrays.asList(
				new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "111"))),
				new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithVersionPinnedSource() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123.5"))).thenReturn(syn123Schema);

		// call under test - a pinned source version is carried on the input reference.
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select foo from syn123.5");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", 5L, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnionMergesInputsAcrossBranches() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - a UNION unions the inputs of each branch by output position; an expression on
		// either branch dominates an identity on the other.
		List<ColumnProvenanceEntry> entries = manager
				.computeEntries("select foo from syn123 union select bar + 1 from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnionAcrossDifferentTables() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn456"))).thenReturn(syn456Schema);

		// call under test - each branch reads an identity column from a different source table; the single
		// output column carries the union of both source inputs.
		List<ColumnProvenanceEntry> entries = manager
				.computeEntries("select foo from syn123 union select baz from syn456");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn456", null, "333")))), entries);
	}

	@Test
	public void testComputeEntriesWithMultiColumnExpression() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - an expression combining two columns is not an identity (more than one input).
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select foo + bar from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnionOfThreeBranchesFirstNonIdentity() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - the first branch is an expression, so the merged column is already non-identity
		// when the later identity branches are folded in.
		List<ColumnProvenanceEntry> entries = manager
				.computeEntries("select bar + 1 from syn123 union select foo from syn123 union select bar from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "222"), source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithCommonTableExpressionIgnoresMismatchedWidthPart() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn456"))).thenReturn(syn456Schema);

		// call under test - the CTE's inner query is a part of a different width than the main select, so it
		// is ignored and only the main select's columns are described.
		List<ColumnProvenanceEntry> entries = manager
				.computeEntries("with cte as (select foo, bar from syn123) select baz from syn456");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn456", null, "333")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnparsableSql() {
		// call under test - a defining SQL that does not parse surfaces as an IllegalArgumentException.
		assertThrows(IllegalArgumentException.class, () -> {
			manager.computeEntries("this is not valid sql");
		});
	}

	@Test
	public void testComputeEntriesWithCast() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - a CAST transforms the value, so the output is an expression over its input column.
		List<ColumnProvenanceEntry> entries = manager.computeEntries("select cast(foo as STRING) from syn123");

		assertEquals(Collections.singletonList(new ColumnProvenanceEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithJoinAcrossTables() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn456"))).thenReturn(syn456Schema);

		// call under test - a join draws one output column from each joined table.
		List<ColumnProvenanceEntry> entries = manager
				.computeEntries("select syn123.foo, syn456.baz from syn123 join syn456 on (syn123.foo = syn456.baz)");

		assertEquals(Arrays.asList(
				new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "111"))),
				new ColumnProvenanceEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn456", null, "333")))), entries);
	}

	private static SourceColumnReference source(String objectId, Long version, String columnId) {
		return new SourceColumnReference().setSourceObjectId(objectId).setSourceVersionNumber(version)
				.setSourceColumnId(columnId);
	}

}
