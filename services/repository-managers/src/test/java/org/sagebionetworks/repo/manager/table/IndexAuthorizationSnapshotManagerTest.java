package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.BenefactorColumn;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.repo.model.table.SourceDependency;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.IndexDescriptionState;

@ExtendWith(MockitoExtension.class)
public class IndexAuthorizationSnapshotManagerTest {

	@Mock
	private TableManagerSupport mockTableManagerSupport;
	@Mock
	private TableIndexConnectionFactory mockConnectionFactory;
	@Mock
	private TableIndexManager mockTableIndexManager;

	@InjectMocks
	private IndexAuthorizationSnapshotManager manager;

	// syn123 source schema: foo(111), bar(222)
	private final ColumnModel foo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.INTEGER);
	private final ColumnModel bar = TableModelTestUtils.createColumn(222L, "bar", ColumnType.STRING);
	private final List<ColumnModel> syn123Schema = Arrays.asList(foo, bar);

	// syn456 source schema: baz(333), qux(444)
	private final ColumnModel baz = TableModelTestUtils.createColumn(333L, "baz", ColumnType.INTEGER);
	private final ColumnModel qux = TableModelTestUtils.createColumn(444L, "qux", ColumnType.STRING);
	private final List<ColumnModel> syn456Schema = Arrays.asList(baz, qux);

	// --- classification (computeEntries) ---

	@Test
	public void testComputeEntriesWithIdentity() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		List<ColumnLineageEntry> entries = manager.computeEntries("select foo from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithRenameIsStillIdentity() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - an 'as' alias renames the output but the value is still a bare reference.
		List<ColumnLineageEntry> entries = manager.computeEntries("select foo as foo_renamed from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithExpression() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		List<ColumnLineageEntry> entries = manager.computeEntries("select foo + 1 from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithAggregateCount() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - count(*) is a function of row cardinality, not of any column value, so it has
		// no input columns by design. See the AGGREGATE branch of computeEntry: the differencing risk of a
		// filtered count is a query-time concern, not a select-list lineage hole.
		List<ColumnLineageEntry> entries = manager.computeEntries("select count(*) from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.AGGREGATE)
				.setSetFunctionType("COUNT").setInputs(Collections.emptyList())), entries);
	}

	@Test
	public void testComputeEntriesWithAggregateMax() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		List<ColumnLineageEntry> entries = manager.computeEntries("select max(bar) from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.AGGREGATE)
				.setSetFunctionType("MAX").setInputs(Collections.singletonList(source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithLiteral() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - a constant has no input columns.
		List<ColumnLineageEntry> entries = manager.computeEntries("select 'tag' as tag from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.LITERAL)
				.setInputs(Collections.emptyList())), entries);
	}

	@Test
	public void testComputeEntriesWithSelectStarExpandsToEachColumn() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - 'select *' is expanded to one identity entry per source column.
		List<ColumnLineageEntry> entries = manager.computeEntries("select * from syn123");

		assertEquals(Arrays.asList(
				new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "111"))),
				new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithVersionPinnedSource() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123.5"))).thenReturn(syn123Schema);

		// call under test - a pinned source version is carried on the input reference.
		List<ColumnLineageEntry> entries = manager.computeEntries("select foo from syn123.5");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", 5L, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnionMergesInputsAcrossBranches() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - a UNION unions the inputs of each branch by output position; an expression on
		// either branch dominates an identity on the other.
		List<ColumnLineageEntry> entries = manager
				.computeEntries("select foo from syn123 union select bar + 1 from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnionAcrossDifferentTables() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn456"))).thenReturn(syn456Schema);

		// call under test - each branch reads an identity column from a different source table; the single
		// output column carries the union of both source inputs.
		List<ColumnLineageEntry> entries = manager
				.computeEntries("select foo from syn123 union select baz from syn456");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn456", null, "333")))), entries);
	}

	@Test
	public void testComputeEntriesWithMultiColumnExpression() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - an expression combining two columns is not an identity (more than one input).
		List<ColumnLineageEntry> entries = manager.computeEntries("select foo + bar from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithMultiColumnFunction() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - a scalar function over two columns (plus a literal separator) is an expression;
		// both referenced columns are inputs and the string literal contributes none.
		List<ColumnLineageEntry> entries = manager.computeEntries("select concat(foo, '-', bar) as con from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn123", null, "222")))), entries);
	}

	@Test
	public void testComputeEntriesWithUnionOfThreeBranchesFirstNonIdentity() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test - the first branch is an expression, so the merged column is already non-identity
		// when the later identity branches are folded in.
		List<ColumnLineageEntry> entries = manager
				.computeEntries("select bar + 1 from syn123 union select foo from syn123 union select bar from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Arrays.asList(source("syn123", null, "222"), source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithCommonTableExpressionIgnoresMismatchedWidthPart() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn456"))).thenReturn(syn456Schema);

		// call under test - the CTE's inner query is a part of a different width than the main select, so it
		// is ignored and only the main select's columns are described.
		List<ColumnLineageEntry> entries = manager
				.computeEntries("with cte as (select foo, bar from syn123) select baz from syn456");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
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
		List<ColumnLineageEntry> entries = manager.computeEntries("select cast(foo as STRING) from syn123");

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))), entries);
	}

	@Test
	public void testComputeEntriesWithJoinAcrossTables() {
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn456"))).thenReturn(syn456Schema);

		// call under test - a join draws one output column from each joined table.
		List<ColumnLineageEntry> entries = manager
				.computeEntries("select syn123.foo, syn456.baz from syn123 join syn456 on (syn123.foo = syn456.baz)");

		assertEquals(Arrays.asList(
				new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn123", null, "111"))),
				new ColumnLineageEntry().setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source("syn456", null, "333")))), entries);
	}

	// --- IndexDescription authorization projection ---

	@Test
	public void testBuildIndexDescriptionSnapshotWithBenefactorsAndFlattenedDependencies() {
		// syn999 -> syn2 (materialized view) -> syn123 (table); syn999 -> syn456 (table)
		IndexDescription mv2 = mockLeaf("syn2", TableType.materializedview);
		IndexDescription table456 = mockLeaf("syn456", TableType.table);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select * from syn2 join syn456",
				mv2, table456);
		when(root.getBenefactors()).thenReturn(Arrays.asList(
				new BenefactorDescription("ROW_BENEFACTOR", ObjectType.ENTITY),
				new BenefactorDescription("ROW_BENEFACTOR_B", ObjectType.EVALUATION)));
		Map<IdAndVersion, IndexAuthorizationSnapshot> sources = sources(
				mvSnapshot("syn2", Collections.emptyList(), dependency("syn123", TableType.table)),
				tableSnapshot("syn456", syn456Schema));

		// call under test
		IndexDescriptionSnapshot snapshot = manager.buildIndexDescriptionSnapshot(root, sources);

		assertEquals(new IndexDescriptionSnapshot()
				.setObjectId("syn999")
				.setVersionNumber(null)
				.setTableType(TableType.materializedview.name())
				.setBenefactors(Arrays.asList(
						new BenefactorColumn().setBenefactorColumnName("ROW_BENEFACTOR").setBenefactorType(ObjectType.ENTITY.name()),
						new BenefactorColumn().setBenefactorColumnName("ROW_BENEFACTOR_B").setBenefactorType(ObjectType.EVALUATION.name())))
				// The flattened transitive closure of dependencies, excluding the root, first-seen order.
				.setDependencies(Arrays.asList(
						new SourceDependency().setObjectId("syn2").setVersionNumber(null).setTableType(TableType.materializedview.name()),
						new SourceDependency().setObjectId("syn123").setVersionNumber(null).setTableType(TableType.table.name()),
						new SourceDependency().setObjectId("syn456").setVersionNumber(null).setTableType(TableType.table.name())))
				// The state needed to rebuild the real MaterializedViewIndexDescription at query time.
				.setDefiningSql("select * from syn2 join syn456"),
				snapshot);
	}

	@Test
	public void testBuildIndexDescriptionSnapshotWithDiamondDependencyIsDeduplicated() {
		// syn999 -> syn2 -> syn123 and syn999 -> syn3 -> syn123 : syn123 appears once.
		IndexDescription mv2 = mockLeaf("syn2", TableType.materializedview);
		IndexDescription mv3 = mockLeaf("syn3", TableType.materializedview);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select * from syn2 join syn3", mv2, mv3);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		Map<IdAndVersion, IndexAuthorizationSnapshot> sources = sources(
				mvSnapshot("syn2", Collections.emptyList(), dependency("syn123", TableType.table)),
				mvSnapshot("syn3", Collections.emptyList(), dependency("syn123", TableType.table)));

		// call under test
		IndexDescriptionSnapshot snapshot = manager.buildIndexDescriptionSnapshot(root, sources);

		assertEquals(Arrays.asList(
				dependency("syn2", TableType.materializedview),
				dependency("syn123", TableType.table),
				dependency("syn3", TableType.materializedview)),
				snapshot.getDependencies());
	}

	// --- build-time lineage flattening ---

	@Test
	public void testBuildSnapshotWithBaseTableSourceKeepsLeafReference() {
		// syn999 reads directly from the base table syn123, whose identity snapshot resolves each column to
		// itself: the input stays a leaf reference, IDENTITY.
		IndexDescription table123 = mockLeaf("syn123", TableType.table);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select foo from syn123", table123);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		stubSnapshots(tableSnapshot("syn123", syn123Schema));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		ColumnModel rootFoo = TableModelTestUtils.createColumn(500L, "foo", ColumnType.INTEGER);

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(root, "select foo from syn123",
				Collections.singletonList(rootFoo));

		assertEquals(Collections.singletonList(new ColumnLineageEntry()
				.setOutputColumnId("500")
				.setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))),
				snapshot.getColumnLineage());
		assertEquals(Collections.singletonList(dependency("syn123", TableType.table)),
				snapshot.getIndexDescription().getDependencies());
	}

	@Test
	public void testBuildSnapshotWithTemporaryShadowRebuildIdUsesRealObjectId() {
		// A materialized view shadow rebuild builds under a temporary negated id (see
		// MaterializedViewManagerImpl), then atomically swaps the index - and its snapshot - into the real
		// id. The snapshot content is served for the real id, so it must carry the real object id, not the
		// negated build-target id, or a query resolving the snapshot would parse 'syn-999' and fail to find
		// the object.
		IndexDescription table123 = mockLeaf("syn123", TableType.table);
		IndexDescription root = mockRoot("syn-999", TableType.materializedview, "select foo from syn123", table123);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		stubSnapshots(tableSnapshot("syn123", syn123Schema));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		ColumnModel rootFoo = TableModelTestUtils.createColumn(500L, "foo", ColumnType.INTEGER);

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(root, "select foo from syn123",
				Collections.singletonList(rootFoo));

		assertEquals("syn999", snapshot.getObjectId());
		assertEquals("syn999", snapshot.getIndexDescription().getObjectId());
	}

	@Test
	public void testBuildSnapshotComposesAgainstPersistedSourceSnapshot() {
		// The root reads identity column 'a' from syn2, a materialized view. syn2 has a PERSISTED snapshot
		// whose 'a' is an EXPRESSION over the leaf syn123.foo. Composition must use that frozen snapshot -
		// not re-read syn2's current defining SQL - so drift in syn2's SQL after its build cannot change
		// the root's as-built lineage. Neither syn2's defining SQL nor syn123's schema is stubbed, proving
		// the recompute path is never taken.
		IndexDescription mv2 = mockLeaf("syn2", TableType.materializedview);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select a from syn2", mv2);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());

		IndexAuthorizationSnapshot syn2Snapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn2")
				.setColumnLineage(Collections.singletonList(new ColumnLineageEntry()
						.setOutputColumnId("20")
						.setDerivationKind(DerivationKind.EXPRESSION)
						.setInputs(Collections.singletonList(source("syn123", null, "111")))))
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn2")
						.setTableType(TableType.materializedview.name())
						.setDependencies(Collections.singletonList(new SourceDependency()
								.setObjectId("syn123").setVersionNumber(null).setTableType(TableType.table.name()))));
		when(mockConnectionFactory.connectToTableIndex(IdAndVersion.parse("syn2"))).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(IdAndVersion.parse("syn2"))).thenReturn(Optional.of(syn2Snapshot));

		// The root's own defining SQL selects the identity column 'a' from syn2.
		ColumnModel a = TableModelTestUtils.createColumn(20L, "a", ColumnType.INTEGER);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn2"))).thenReturn(Collections.singletonList(a));
		ColumnModel rootA = TableModelTestUtils.createColumn(500L, "a", ColumnType.INTEGER);

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(root, "select a from syn2",
				Collections.singletonList(rootA));

		// Lineage flattened through the persisted snapshot: EXPRESSION over the leaf syn123.foo.
		assertEquals(Collections.singletonList(new ColumnLineageEntry()
				.setOutputColumnId("500")
				.setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))),
				snapshot.getColumnLineage());
		// Transitive closure composed from the source snapshot: syn2 plus its own dependency syn123.
		assertEquals(Arrays.asList(
				new SourceDependency().setObjectId("syn2").setVersionNumber(null).setTableType(TableType.materializedview.name()),
				new SourceDependency().setObjectId("syn123").setVersionNumber(null).setTableType(TableType.table.name())),
				snapshot.getIndexDescription().getDependencies());
	}

	@Test
	public void testBuildSnapshotFirstNonIdentityDominatesWhenIdentityMergesMultipleChildren() {
		// The root's single output column is an IDENTITY produced by a UNION whose branches read a bare
		// column from two different materialized-view sources: 'a' from syn2 and 'b' from syn3. syn2's
		// persisted 'a' is an EXPRESSION; syn3's persisted 'b' is an AGGREGATE(MAX). When the merged
		// identity column is flattened, the first resolved non-identity child must dominate the derivation
		// (EXPRESSION), not be overwritten by whichever child is resolved last (which would wrongly report
		// AGGREGATE/MAX). Both children still contribute their leaf inputs.
		IndexDescription mv2 = mockLeaf("syn2", TableType.materializedview);
		IndexDescription mv3 = mockLeaf("syn3", TableType.materializedview);
		IndexDescription root = mockRoot("syn999", TableType.materializedview,
				"select a from syn2 union select b from syn3", mv2, mv3);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());

		IndexAuthorizationSnapshot syn2Snapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn2")
				.setColumnLineage(Collections.singletonList(new ColumnLineageEntry()
						.setOutputColumnId("20")
						.setDerivationKind(DerivationKind.EXPRESSION)
						.setInputs(Collections.singletonList(source("syn123", null, "111")))))
				.setIndexDescription(new IndexDescriptionSnapshot().setObjectId("syn2")
						.setTableType(TableType.materializedview.name())
						.setDependencies(Collections.singletonList(new SourceDependency()
								.setObjectId("syn123").setVersionNumber(null).setTableType(TableType.table.name()))));
		IndexAuthorizationSnapshot syn3Snapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn3")
				.setColumnLineage(Collections.singletonList(new ColumnLineageEntry()
						.setOutputColumnId("30")
						.setDerivationKind(DerivationKind.AGGREGATE)
						.setSetFunctionType("MAX")
						.setInputs(Collections.singletonList(source("syn456", null, "333")))))
				.setIndexDescription(new IndexDescriptionSnapshot().setObjectId("syn3")
						.setTableType(TableType.materializedview.name())
						.setDependencies(Collections.singletonList(new SourceDependency()
								.setObjectId("syn456").setVersionNumber(null).setTableType(TableType.table.name()))));
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(IdAndVersion.parse("syn2"))).thenReturn(Optional.of(syn2Snapshot));
		when(mockTableIndexManager.getAuthorizationSnapshot(IdAndVersion.parse("syn3"))).thenReturn(Optional.of(syn3Snapshot));

		// The root's defining SQL unions a bare column from each source.
		ColumnModel a = TableModelTestUtils.createColumn(20L, "a", ColumnType.INTEGER);
		ColumnModel b = TableModelTestUtils.createColumn(30L, "b", ColumnType.INTEGER);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn2"))).thenReturn(Collections.singletonList(a));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn3"))).thenReturn(Collections.singletonList(b));
		ColumnModel rootA = TableModelTestUtils.createColumn(500L, "a", ColumnType.INTEGER);

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(root, "select a from syn2 union select b from syn3",
				Collections.singletonList(rootA));

		// First non-identity child (EXPRESSION) dominates; the AGGREGATE resolved last does not overwrite it.
		assertEquals(Collections.singletonList(new ColumnLineageEntry()
				.setOutputColumnId("500")
				.setDerivationKind(DerivationKind.EXPRESSION)
				.setSetFunctionType(null)
				.setInputs(Arrays.asList(source("syn123", null, "111"), source("syn456", null, "333")))),
				snapshot.getColumnLineage());
	}

	@Test
	public void testBuildSnapshotWithBoundSchemaSizeMismatch() {
		IndexDescription table123 = mockLeaf("syn123", TableType.table);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select foo, bar from syn123", table123);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		stubSnapshots(tableSnapshot("syn123", syn123Schema));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// The bound schema has one column but the defining SQL produces two - a corrupted invariant (500).
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.buildSnapshot(root, "select foo, bar from syn123",
					Collections.singletonList(TableModelTestUtils.createColumn(500L, "foo", ColumnType.INTEGER)));
		}).getMessage();
		assertEquals("Expected 1 bound columns to match the defining SQL of syn999 but computed 2", message);
	}

	@Test
	public void testBuildSnapshotWithBoundSchemaNameMismatch() {
		IndexDescription table123 = mockLeaf("syn123", TableType.table);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select foo, bar as bar_out from syn123",
				table123);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		stubSnapshots(tableSnapshot("syn123", syn123Schema));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// The bound name at position 1 no longer matches the name the defining SQL produces (500).
		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.buildSnapshot(root, "select foo, bar as bar_out from syn123", Arrays.asList(
					TableModelTestUtils.createColumn(500L, "foo", ColumnType.INTEGER),
					TableModelTestUtils.createColumn(501L, "stale_name", ColumnType.STRING)));
		}).getMessage();
		assertEquals("The bound schema of syn999 is out of sync with its defining SQL at column 1:"
				+ " bound column 'stale_name' but the defining SQL produced 'bar_out'", message);
	}

	// --- base index types (no defining SQL) ---

	@Test
	public void testBuildSnapshotForBaseTableIsIdentityLineageWithNoDependencies() {
		// A plain table selects its own columns: each output column is an identity of itself, and it has no
		// benefactors or dependencies of its own.
		IndexDescription table = mockRoot("syn123", TableType.table, null);
		when(table.getBenefactors()).thenReturn(Collections.emptyList());

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(table, syn123Schema);

		assertEquals(new IndexAuthorizationSnapshot()
				.setObjectId("syn123")
				.setVersionNumber(null)
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn123")
						.setVersionNumber(null)
						.setTableType(TableType.table.name())
						.setBenefactors(Collections.emptyList())
						.setDependencies(Collections.emptyList()))
				.setColumnLineage(Arrays.asList(
						new ColumnLineageEntry().setOutputColumnId("111").setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Collections.singletonList(source("syn123", null, "111"))),
						new ColumnLineageEntry().setOutputColumnId("222").setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Collections.singletonList(source("syn123", null, "222"))))),
				snapshot);
	}

	@Test
	public void testBuildSnapshotForBaseViewCarriesItsBenefactorColumn() {
		// A view selects its own columns like a table but carries a single row-level benefactor column.
		IndexDescription view = mockRoot("syn123.4", TableType.entityview, null);
		when(view.getBenefactors()).thenReturn(Collections
				.singletonList(new BenefactorDescription("ROW_BENEFACTOR", ObjectType.ENTITY)));

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(view, Collections.singletonList(foo));

		assertEquals(new IndexAuthorizationSnapshot()
				.setObjectId("syn123")
				.setVersionNumber(4L)
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn123")
						.setVersionNumber(4L)
						.setTableType(TableType.entityview.name())
						.setBenefactors(Collections.singletonList(new BenefactorColumn()
								.setBenefactorColumnName("ROW_BENEFACTOR").setBenefactorType(ObjectType.ENTITY.name())))
						.setDependencies(Collections.emptyList()))
				.setColumnLineage(Collections.singletonList(
						new ColumnLineageEntry().setOutputColumnId("111").setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Collections.singletonList(source("syn123", 4L, "111"))))),
				snapshot);
	}

	// --- read API ---

	@Test
	public void testGetAuthorizationSnapshot() throws Exception {
		IdAndVersion object = IdAndVersion.parse("syn999");
		IndexAuthorizationSnapshot expected = new IndexAuthorizationSnapshot().setObjectId("syn999");
		when(mockConnectionFactory.connectToTableIndex(object)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(object)).thenReturn(Optional.of(expected));

		// call under test
		Optional<IndexAuthorizationSnapshot> result = manager.getAuthorizationSnapshot(object);

		assertEquals(Optional.of(expected), result);
	}

	@Test
	public void testGetAuthorizationSnapshotWithNullObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getAuthorizationSnapshot(null);
		}).getMessage();
		assertEquals("object is required.", message);
		verify(mockConnectionFactory, never()).connectToTableIndex(any());
	}

	private static SourceColumnReference source(String objectId, Long version, String columnId) {
		return new SourceColumnReference().setSourceObjectId(objectId).setSourceVersionNumber(version)
				.setSourceColumnId(columnId);
	}

	/**
	 * A mock IndexDescription node with the given id, table type, and immediate dependencies. Only the
	 * accessors the manager reads are stubbed.
	 */
	private IndexDescription mockNode(String idAndVersion, TableType tableType, IndexDescription... dependencies) {
		IndexDescription node = org.mockito.Mockito.mock(IndexDescription.class);
		when(node.getIdAndVersion()).thenReturn(IdAndVersion.parse(idAndVersion));
		when(node.getTableType()).thenReturn(tableType);
		when(node.getDependencies()).thenReturn(Arrays.asList(dependencies));
		return node;
	}

	/**
	 * A mock of the object a snapshot is being captured for. Only the root contributes its own
	 * {@link IndexDescriptionState}, so that stub belongs here rather than on every dependency node.
	 * A base index type passes a null defining SQL.
	 */
	private IndexDescription mockRoot(String idAndVersion, TableType tableType, String definingSql,
			IndexDescription... dependencies) {
		IndexDescription node = mockNode(idAndVersion, tableType, dependencies);
		when(node.getState()).thenReturn(
				new IndexDescriptionState(IdAndVersion.parse(idAndVersion), tableType, definingSql, null));
		return node;
	}

	/**
	 * A mock dependency of the object being captured. Its transitive closure and lineage come from its
	 * snapshot, so the manager never walks its {@code getDependencies()}. Only id and table type are stubbed.
	 */
	private IndexDescription mockLeaf(String idAndVersion, TableType tableType) {
		IndexDescription node = org.mockito.Mockito.mock(IndexDescription.class);
		when(node.getIdAndVersion()).thenReturn(IdAndVersion.parse(idAndVersion));
		when(node.getTableType()).thenReturn(tableType);
		return node;
	}

	/**
	 * Serve each snapshot as the persisted snapshot of its object.
	 */
	private void stubSnapshots(IndexAuthorizationSnapshot... snapshots) {
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		for (IndexAuthorizationSnapshot snapshot : snapshots) {
			when(mockTableIndexManager.getAuthorizationSnapshot(IdAndVersion.parse(snapshot.getObjectId())))
					.thenReturn(Optional.of(snapshot));
		}
	}

	/**
	 * The source snapshots keyed by object, in the given order.
	 */
	private static Map<IdAndVersion, IndexAuthorizationSnapshot> sources(IndexAuthorizationSnapshot... snapshots) {
		Map<IdAndVersion, IndexAuthorizationSnapshot> sources = new LinkedHashMap<>();
		for (IndexAuthorizationSnapshot snapshot : snapshots) {
			sources.put(IdAndVersion.parse(snapshot.getObjectId()), snapshot);
		}
		return sources;
	}

	/**
	 * The as-built snapshot of a base table: each column an identity of itself, no dependencies.
	 */
	private static IndexAuthorizationSnapshot tableSnapshot(String objectId, List<ColumnModel> schema) {
		List<ColumnLineageEntry> lineage = schema.stream()
				.map(column -> new ColumnLineageEntry().setOutputColumnId(column.getId())
						.setDerivationKind(DerivationKind.IDENTITY)
						.setInputs(Collections.singletonList(source(objectId, null, column.getId()))))
				.toList();
		return new IndexAuthorizationSnapshot().setObjectId(objectId).setColumnLineage(lineage)
				.setIndexDescription(new IndexDescriptionSnapshot().setObjectId(objectId)
						.setTableType(TableType.table.name()).setBenefactors(Collections.emptyList())
						.setDependencies(Collections.emptyList()));
	}

	/**
	 * The as-built snapshot of a materialized view with the given lineage and flattened dependency closure.
	 */
	private static IndexAuthorizationSnapshot mvSnapshot(String objectId, List<ColumnLineageEntry> lineage,
			SourceDependency... dependencies) {
		return new IndexAuthorizationSnapshot().setObjectId(objectId).setColumnLineage(lineage)
				.setIndexDescription(new IndexDescriptionSnapshot().setObjectId(objectId)
						.setTableType(TableType.materializedview.name()).setBenefactors(Collections.emptyList())
						.setDependencies(Arrays.asList(dependencies)));
	}

	private static SourceDependency dependency(String objectId, TableType tableType) {
		return new SourceDependency().setObjectId(objectId).setVersionNumber(null).setTableType(tableType.name());
	}

	// --- getSnapshotIndexDescription ---

	@Test
	public void testGetSnapshotIndexDescriptionWithPersistedSnapshot() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		IndexAuthorizationSnapshot persistedSnapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn123")
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn123")
						.setTableType(TableType.table.name())
						.setBenefactors(Collections.emptyList())
						.setDependencies(Collections.emptyList()))
				.setColumnLineage(Collections.singletonList(
						new ColumnLineageEntry().setOutputColumnId("111")));

		when(mockConnectionFactory.connectToTableIndex(id)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(id)).thenReturn(Optional.of(persistedSnapshot));

		// call under test
		var result = manager.getSnapshotIndexDescription(id);

		assertEquals(id, result.getIdAndVersion());
		assertEquals(TableType.table, result.getTableType());
		assertEquals(1, result.getColumnLineage().size());
		assertEquals("111", result.getColumnLineage().get(0).getOutputColumnId());
	}

	@Test
	public void testGetSnapshotIndexDescriptionWithVirtualTable() {
		IdAndVersion vtId = IdAndVersion.parse("syn200");
		IdAndVersion sourceId = IdAndVersion.parse("syn123");

		// VirtualTable has no persisted snapshot
		when(mockConnectionFactory.connectToTableIndex(vtId)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(vtId)).thenReturn(Optional.empty());
		when(mockTableManagerSupport.getTableType(vtId)).thenReturn(TableType.virtualtable);
		when(mockTableManagerSupport.getDefiningSql(vtId)).thenReturn(Optional.of("select foo as bar from syn123"));

		// VT's bound schema: the output column "bar" has id 222 (different from source's "foo" id 111
		// because aliasing → re-hash)
		ColumnModel vtColumn = TableModelTestUtils.createColumn(222L, "bar", ColumnType.INTEGER);
		when(mockTableManagerSupport.getTableSchema(vtId)).thenReturn(List.of(vtColumn));

		// Source has a persisted snapshot with identity lineage
		IndexAuthorizationSnapshot sourceSnapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn123")
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn123")
						.setTableType(TableType.table.name())
						.setBenefactors(Collections.emptyList())
						.setDependencies(Collections.emptyList()))
				.setColumnLineage(Collections.singletonList(
						new ColumnLineageEntry().setOutputColumnId("111")
								.setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Collections.singletonList(source("syn123", null, "111")))));

		when(mockConnectionFactory.connectToTableIndex(sourceId)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(sourceId)).thenReturn(Optional.of(sourceSnapshot));
		when(mockTableManagerSupport.getTableSchema(sourceId)).thenReturn(syn123Schema);

		// call under test
		var result = manager.getSnapshotIndexDescription(vtId);

		// The VT's lineage is computed on-the-fly and aligned to its bound schema
		assertEquals(vtId, result.getIdAndVersion());
		assertEquals(TableType.virtualtable, result.getTableType());
		assertEquals(1, result.getColumnLineage().size());
		// The output column id is the VT's bound column id (222), not the source's (111)
		assertEquals("222", result.getColumnLineage().get(0).getOutputColumnId());
		// But it derives from the source's column 111
		assertEquals("111", result.getColumnLineage().get(0).getInputs().get(0).getSourceColumnId());
	}

	@Test
	public void testGetSnapshotIndexDescriptionWithVirtualTableRenamingQuasiIdentifier() {
		// This is the exact scenario that causes the leak: a VT that renames a QID column.
		IdAndVersion vtId = IdAndVersion.parse("syn200");
		IdAndVersion sourceId = IdAndVersion.parse("syn123");

		when(mockConnectionFactory.connectToTableIndex(vtId)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(vtId)).thenReturn(Optional.empty());
		when(mockTableManagerSupport.getTableType(vtId)).thenReturn(TableType.virtualtable);
		when(mockTableManagerSupport.getDefiningSql(vtId)).thenReturn(Optional.of("select PART_ID as PART_IZ from syn123"));

		// VT output column: PART_IZ with id 222 (different from source's PART_ID id 111)
		ColumnModel vtColumn = TableModelTestUtils.createColumn(222L, "PART_IZ", ColumnType.INTEGER);
		when(mockTableManagerSupport.getTableSchema(vtId)).thenReturn(List.of(vtColumn));

		// Source has persisted snapshot with identity lineage for PART_ID (id 111)
		ColumnModel sourceColumn = TableModelTestUtils.createColumn(111L, "PART_ID", ColumnType.INTEGER);
		IndexAuthorizationSnapshot sourceSnapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn123")
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn123")
						.setTableType(TableType.table.name())
						.setBenefactors(Collections.emptyList())
						.setDependencies(Collections.emptyList()))
				.setColumnLineage(Collections.singletonList(
						new ColumnLineageEntry().setOutputColumnId("111")
								.setDerivationKind(DerivationKind.IDENTITY)
								.setInputs(Collections.singletonList(source("syn123", null, "111")))));

		when(mockConnectionFactory.connectToTableIndex(sourceId)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(sourceId)).thenReturn(Optional.of(sourceSnapshot));
		when(mockTableManagerSupport.getTableSchema(sourceId)).thenReturn(List.of(sourceColumn));

		// call under test
		var result = manager.getSnapshotIndexDescription(vtId);

		// The VT's lineage maps its output column id (222) → source column id (111)
		assertEquals("222", result.getColumnLineage().get(0).getOutputColumnId());
		assertEquals("111", result.getColumnLineage().get(0).getInputs().get(0).getSourceColumnId());
		// So when the resolver is given this lineage, it will correctly map VT column 222 → source
		// QID 111, and the validator will recognize column 222 as carrying the quasi-identifier.
	}

	@Test
	public void testGetSnapshotIndexDescriptionWithNoSnapshotAndNotVirtualTable() {
		IdAndVersion id = IdAndVersion.parse("syn123");

		// No persisted snapshot
		when(mockConnectionFactory.connectToTableIndex(id)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(id)).thenReturn(Optional.empty());
		// Not a VirtualTable
		when(mockTableManagerSupport.getTableType(id)).thenReturn(TableType.table);

		// call under test
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
			manager.getSnapshotIndexDescription(id);
		});
		assertEquals("No authorization snapshot exists for syn123 and it is not a VirtualTable", thrown.getMessage());
	}

	@Test
	public void testGetSnapshotIndexDescriptionWithNullIdAndVersion() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getSnapshotIndexDescription(null);
		}).getMessage();
		assertEquals("idAndVersion is required.", message);
	}

	// --- buildSnapshot source snapshot resolution ---

	@Test
	public void testBuildSnapshotPreLoadsDependencySnapshotAvoidingTOCTOU() {
		// PLFM-9939 TOCTOU fix: when building MV1 -> MV2, MV1's lineage is composed from MV2's persisted
		// snapshot (pinned by the read lock), never from MV2's current defining SQL.
		IdAndVersion mv2Id = IdAndVersion.parse("syn456");

		// MV2 has a persisted snapshot with lineage derived from leaf syn123
		IndexAuthorizationSnapshot mv2Snapshot = new IndexAuthorizationSnapshot()
				.setObjectId("syn456")
				.setIndexDescription(new IndexDescriptionSnapshot()
						.setObjectId("syn456")
						.setTableType(TableType.materializedview.name())
						.setBenefactors(Collections.emptyList())
						.setDependencies(Collections.emptyList()))
				.setColumnLineage(Collections.singletonList(
						new ColumnLineageEntry().setOutputColumnId("222")
								.setDerivationKind(DerivationKind.EXPRESSION)
								.setInputs(Collections.singletonList(source("syn123", null, "111")))));

		when(mockConnectionFactory.connectToTableIndex(mv2Id)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(mv2Id)).thenReturn(Optional.of(mv2Snapshot));
		when(mockTableManagerSupport.getTableSchema(mv2Id)).thenReturn(Collections.singletonList(
				TableModelTestUtils.createColumn(222L, "bar", ColumnType.INTEGER)));

		// MV1 depends on MV2. Use mockLeaf for mv2Desc since its dependencies are never walked (pre-loaded).
		IndexDescription mv2Desc = mockLeaf("syn456", TableType.materializedview);
		IndexDescription mv1Desc = mockRoot("syn789", TableType.materializedview,
				"select bar + 1 as result from syn456", mv2Desc);
		when(mv1Desc.getBenefactors()).thenReturn(Collections.emptyList());

		ColumnModel mv1Column = TableModelTestUtils.createColumn(333L, "result", ColumnType.INTEGER);

		// call under test
		IndexAuthorizationSnapshot mv1Snapshot = manager.buildSnapshot(mv1Desc,
				"select bar + 1 as result from syn456", Collections.singletonList(mv1Column));

		// MV1's lineage should flatten through MV2's pre-loaded snapshot down to the leaf syn123
		assertEquals(Collections.singletonList(new ColumnLineageEntry()
				.setOutputColumnId("333")
				.setDerivationKind(DerivationKind.EXPRESSION)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))),
				mv1Snapshot.getColumnLineage());
	}

	@Test
	public void testBuildSnapshotWithDependencyWithoutSnapshot() {
		// PLFM-10023: a dependency that is neither snapshotted nor a VirtualTable breaks the capture-at-build
		// invariant, so the build fails instead of capturing lineage from live state.
		IdAndVersion leafId = IdAndVersion.parse("syn123");
		IndexDescription leaf = Mockito.mock(IndexDescription.class);
		when(leaf.getIdAndVersion()).thenReturn(leafId);
		IndexDescription root = Mockito.mock(IndexDescription.class);
		when(root.getIdAndVersion()).thenReturn(IdAndVersion.parse("syn999"));
		when(root.getDependencies()).thenReturn(Collections.singletonList(leaf));
		when(mockConnectionFactory.connectToTableIndex(leafId)).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(leafId)).thenReturn(Optional.empty());
		when(mockTableManagerSupport.getTableType(leafId)).thenReturn(TableType.table);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.buildSnapshot(root, "select foo from syn123",
					Collections.singletonList(TableModelTestUtils.createColumn(500L, "foo", ColumnType.INTEGER)));
		}).getMessage();
		assertEquals("No authorization snapshot exists for syn123 and it is not a VirtualTable", message);
		verify(mockTableManagerSupport, never()).getDefiningSql(any());
		verify(mockTableManagerSupport, never()).getTableSchema(any());
	}

	@Test
	public void testBuildSnapshotWithVirtualTableDependencyDrift() {
		// PLFM-10023: a VirtualTable dependency whose bound schema has drifted from its defining SQL must fail
		// the build, not be swallowed into a live-state fallback.
		IdAndVersion vtId = IdAndVersion.parse("syn200");
		IndexDescription vt = Mockito.mock(IndexDescription.class);
		when(vt.getIdAndVersion()).thenReturn(vtId);
		IndexDescription root = Mockito.mock(IndexDescription.class);
		when(root.getIdAndVersion()).thenReturn(IdAndVersion.parse("syn999"));
		when(root.getDependencies()).thenReturn(Collections.singletonList(vt));
		when(mockConnectionFactory.connectToTableIndex(any())).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getAuthorizationSnapshot(vtId)).thenReturn(Optional.empty());
		when(mockTableIndexManager.getAuthorizationSnapshot(IdAndVersion.parse("syn123")))
				.thenReturn(Optional.of(tableSnapshot("syn123", syn123Schema)));
		when(mockTableManagerSupport.getTableType(vtId)).thenReturn(TableType.virtualtable);
		when(mockTableManagerSupport.getDefiningSql(vtId)).thenReturn(Optional.of("select foo from syn123"));
		when(mockTableManagerSupport.getTableSchema(vtId)).thenReturn(
				Collections.singletonList(TableModelTestUtils.createColumn(300L, "stale", ColumnType.INTEGER)));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.buildSnapshot(root, "select stale from syn200",
					Collections.singletonList(TableModelTestUtils.createColumn(500L, "stale", ColumnType.INTEGER)));
		}).getMessage();
		assertEquals("The bound schema of syn200 is out of sync with its defining SQL at column 0:"
				+ " bound column 'stale' but the defining SQL produced 'foo'", message);
	}

	@Test
	public void testBuildSnapshotWithDependencySnapshotNullLineage() {
		IndexDescription mv2 = mockLeaf("syn2", TableType.materializedview);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select a from syn2", mv2);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		stubSnapshots(mvSnapshot("syn2", null));
		ColumnModel a = TableModelTestUtils.createColumn(20L, "a", ColumnType.INTEGER);
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn2"))).thenReturn(Collections.singletonList(a));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			manager.buildSnapshot(root, "select a from syn2",
					Collections.singletonList(TableModelTestUtils.createColumn(500L, "a", ColumnType.INTEGER)));
		}).getMessage();
		assertEquals("The authorization snapshot of syn2 has no column lineage", message);
	}

	@Test
	public void testBuildSnapshotWithDependencySnapshotEmptyLineage() {
		// An empty lineage is a valid capture (a source with no columns), unlike a missing one: it contributes
		// nothing to the composition and does not fail the build.
		IndexDescription table123 = mockLeaf("syn123", TableType.table);
		IndexDescription table456 = mockLeaf("syn456", TableType.table);
		IndexDescription root = mockRoot("syn999", TableType.materializedview, "select foo from syn123", table123,
				table456);
		when(root.getBenefactors()).thenReturn(Collections.emptyList());
		stubSnapshots(tableSnapshot("syn123", syn123Schema), tableSnapshot("syn456", Collections.emptyList()));
		when(mockTableManagerSupport.getTableSchema(IdAndVersion.parse("syn123"))).thenReturn(syn123Schema);

		// call under test
		IndexAuthorizationSnapshot snapshot = manager.buildSnapshot(root, "select foo from syn123",
				Collections.singletonList(TableModelTestUtils.createColumn(500L, "foo", ColumnType.INTEGER)));

		assertEquals(Collections.singletonList(new ColumnLineageEntry().setOutputColumnId("500")
				.setDerivationKind(DerivationKind.IDENTITY)
				.setInputs(Collections.singletonList(source("syn123", null, "111")))),
				snapshot.getColumnLineage());
	}

}
