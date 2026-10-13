package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DerivationKind;
import org.sagebionetworks.repo.model.table.SourceColumnReference;
import org.sagebionetworks.table.cluster.description.QueryIndexDescription;

@ExtendWith(MockitoExtension.class)
public class AggregateQidColumnResolverTest {

	@Mock
	private TableManagerSupport mockTableManagerSupport;

	@Mock
	private QueryIndexDescription mockIndexDescription;

	@InjectMocks
	private AggregateQidColumnResolver resolver;

	private String sourceId;
	private String otherSourceId;

	@BeforeEach
	public void before() {
		sourceId = "syn123";
		otherSourceId = "syn456";
	}

	/**
	 * The source's configuration names 'participantId', which is the name of the ColumnModel the
	 * lineage leaf points at.
	 */
	private void setupQuasiIdentifier(String objectId, String leafColumnId, String leafColumnName) {
		when(mockTableManagerSupport.getAggregateDataConfiguration(objectId)).thenReturn(Optional
				.of(new AggregateDataConfiguration().setQuasiIdentifierColumnNames(List.of(leafColumnName))));
		when(mockTableManagerSupport.getColumnModel(leafColumnId))
				.thenReturn(new ColumnModel().setId(leafColumnId).setName(leafColumnName));
	}

	private ColumnLineageEntry entry(String outputColumnId, DerivationKind kind, String sourceObjectId,
			String sourceColumnId) {
		return new ColumnLineageEntry().setOutputColumnId(outputColumnId).setDerivationKind(kind)
				.setInputs(List.of(new SourceColumnReference().setSourceObjectId(sourceObjectId)
						.setSourceColumnId(sourceColumnId)));
	}

	@Test
	public void testResolveWithPassThroughColumn() {
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("111", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("111"), result);
	}

	@Test
	public void testResolveWithRenamedColumn() {
		// The defining SQL 'select participantId as pid from syn123' binds a different ColumnModel,
		// so the output id must still be recognized through its lineage.
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("222", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
	}

	@Test
	public void testResolveWithAggregateOfQuasiIdentifier() {
		// A pre-baked aggregate over a quasi-identifier never passed through query-time cell
		// suppression, so it is still treated as carrying the quasi-identifier.
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("222", DerivationKind.AGGREGATE, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
	}

	@Test
	public void testResolveWithExpressionOfQuasiIdentifier() {
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("222", DerivationKind.EXPRESSION, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
	}

	@Test
	public void testResolveWithNonQuasiIdentifierColumn() {
		when(mockTableManagerSupport.getAggregateDataConfiguration(sourceId)).thenReturn(Optional
				.of(new AggregateDataConfiguration().setQuasiIdentifierColumnNames(List.of("participantId"))));
		when(mockTableManagerSupport.getColumnModel("112")).thenReturn(new ColumnModel().setId("112").setName("site"));
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("222", DerivationKind.IDENTITY, sourceId, "112")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testResolveWithMixedColumns() {
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockTableManagerSupport.getColumnModel("112")).thenReturn(new ColumnModel().setId("112").setName("site"));
		when(mockIndexDescription.getColumnLineage()).thenReturn(
				List.of(entry("221", DerivationKind.IDENTITY, sourceId, "112"),
						entry("222", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
		// The configuration of a source is read once however many of its columns the lineage names.
		verify(mockTableManagerSupport, times(1)).getAggregateDataConfiguration(sourceId);
	}

	@Test
	public void testResolveWithTwoSources() {
		// A join flattens to leaves in more than one source; each source's configuration decides its
		// own leaves.
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockTableManagerSupport.getAggregateDataConfiguration(otherSourceId)).thenReturn(Optional.empty());
		when(mockIndexDescription.getColumnLineage()).thenReturn(
				List.of(entry("221", DerivationKind.IDENTITY, otherSourceId, "911"),
						entry("222", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
		// A source with no configuration has no quasi-identifiers, so its leaves are never resolved.
		verify(mockTableManagerSupport, never()).getColumnModel("911");
	}

	@Test
	public void testResolveWithAnyInputOfMultipleInputs() {
		// An expression over several columns carries the quasi-identifier if any input is one.
		setupQuasiIdentifier(sourceId, "111", "participantId");
		when(mockTableManagerSupport.getColumnModel("112")).thenReturn(new ColumnModel().setId("112").setName("site"));
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(new ColumnLineageEntry().setOutputColumnId("222")
						.setDerivationKind(DerivationKind.EXPRESSION)
						.setInputs(List.of(
								new SourceColumnReference().setSourceObjectId(sourceId).setSourceColumnId("112"),
								new SourceColumnReference().setSourceObjectId(sourceId).setSourceColumnId("111")))));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
	}

	@Test
	public void testResolveWithNoConfigurationOnSource() {
		when(mockTableManagerSupport.getAggregateDataConfiguration(sourceId)).thenReturn(Optional.empty());
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("111", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertTrue(result.isEmpty());
		verify(mockTableManagerSupport, never()).getColumnModel("111");
	}

	@Test
	public void testResolveWithNullQuasiIdentifierNames() {
		when(mockTableManagerSupport.getAggregateDataConfiguration(sourceId))
				.thenReturn(Optional.of(new AggregateDataConfiguration().setSuppressionThreshold(10L)));
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("111", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertTrue(result.isEmpty());
		verify(mockTableManagerSupport, never()).getColumnModel("111");
	}

	@Test
	public void testResolveWithLiteralColumn() {
		// A literal has no inputs, so it can never reach a quasi-identifier.
		when(mockIndexDescription.getColumnLineage()).thenReturn(
				List.of(new ColumnLineageEntry().setOutputColumnId("222").setDerivationKind(DerivationKind.LITERAL)));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertTrue(result.isEmpty());
		verifyNoMoreInteractions(mockTableManagerSupport);
	}

	@Test
	public void testResolveWithEmptyLineage() {
		when(mockIndexDescription.getColumnLineage()).thenReturn(List.of());

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertTrue(result.isEmpty());
		verifyNoMoreInteractions(mockTableManagerSupport);
	}

	@Test
	public void testResolveWithNullIndexDescription() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			resolver.resolve(null);
		}).getMessage();
		assertEquals("indexDescription is required.", message);
		verifyNoMoreInteractions(mockTableManagerSupport);
	}

	@Test
	public void testResolveWithUnavailableLineage() {
		// A description that cannot supply a lineage must fail rather than resolve nothing and
		// release rows that carry a quasi-identifier.
		when(mockIndexDescription.getColumnLineage()).thenThrow(new IllegalStateException("no lineage"));

		assertThrows(IllegalStateException.class, () -> {
			// call under test
			resolver.resolve(mockIndexDescription);
		});
		verifyNoMoreInteractions(mockTableManagerSupport);
	}

	@Test
	public void testResolveWithQuasiIdentifierNameCaseAndWhitespace() {
		// Column names are compared the way the rest of the table stack compares them: case
		// insensitively, ignoring surrounding whitespace.
		when(mockTableManagerSupport.getAggregateDataConfiguration(sourceId)).thenReturn(Optional
				.of(new AggregateDataConfiguration().setQuasiIdentifierColumnNames(List.of("  PARTICIPANTid "))));
		when(mockTableManagerSupport.getColumnModel("111"))
				.thenReturn(new ColumnModel().setId("111").setName("participantId"));
		when(mockIndexDescription.getColumnLineage())
				.thenReturn(List.of(entry("222", DerivationKind.IDENTITY, sourceId, "111")));

		// call under test
		Set<String> result = resolver.resolve(mockIndexDescription);

		assertEquals(Set.of("222"), result);
	}
}
