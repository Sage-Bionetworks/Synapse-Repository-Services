package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnLineageEntry;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IndexAuthorizationSnapshot;
import org.sagebionetworks.repo.model.table.IndexDescriptionSnapshot;
import org.sagebionetworks.table.cluster.SchemaProvider;

@ExtendWith(MockitoExtension.class)
public class SnapshotSchemaProviderTest {

	@Mock
	private SchemaProvider mockWrapped;

	private IndexAuthorizationSnapshot snapshot() {
		return new IndexAuthorizationSnapshot()
				.setIndexDescription(new IndexDescriptionSnapshot().setObjectId("syn123").setVersionNumber(null)
						.setTableType(TableType.entityview.name()))
				.setColumnLineage(Arrays.asList(new ColumnLineageEntry().setOutputColumnId("11"),
						new ColumnLineageEntry().setOutputColumnId("22")));
	}

	@Test
	public void testGetTableSchemaForQueriedObject() {
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot());
		ColumnModel one = new ColumnModel().setId("11").setName("one");
		ColumnModel two = new ColumnModel().setId("22").setName("two");
		when(mockWrapped.getColumnModel("11")).thenReturn(one);
		when(mockWrapped.getColumnModel("22")).thenReturn(two);

		// call under test
		List<ColumnModel> result = provider.getTableSchema(IdAndVersion.parse("syn123"));

		// The as-built id set is resolved live, in select-list order.
		assertEquals(Arrays.asList(one, two), result);
	}

	@Test
	public void testGetTableSchemaForOtherObjectDelegates() {
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot());
		IdAndVersion other = IdAndVersion.parse("syn456");
		List<ColumnModel> otherSchema = Collections.singletonList(new ColumnModel().setId("99").setName("other"));
		when(mockWrapped.getTableSchema(other)).thenReturn(otherSchema);

		// call under test
		List<ColumnModel> result = provider.getTableSchema(other);

		assertEquals(otherSchema, result);
	}

	@Test
	public void testGetTableSchemaForQueriedObjectWithEmptyLineage() {
		IndexAuthorizationSnapshot snapshot = new IndexAuthorizationSnapshot().setIndexDescription(
				new IndexDescriptionSnapshot().setObjectId("syn123").setTableType(TableType.table.name()))
				.setColumnLineage(null);
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot);

		// call under test
		List<ColumnModel> result = provider.getTableSchema(IdAndVersion.parse("syn123"));

		assertEquals(Collections.emptyList(), result);
		verifyNoInteractions(mockWrapped);
	}

	@Test
	public void testGetTableTypeForQueriedObject() {
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot());

		// call under test
		TableType result = provider.getTableType(IdAndVersion.parse("syn123"));

		// The queried object's type comes from the snapshot, not a live read.
		assertEquals(TableType.entityview, result);
		verifyNoInteractions(mockWrapped);
	}

	@Test
	public void testGetTableTypeForOtherObjectDelegates() {
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot());
		IdAndVersion other = IdAndVersion.parse("syn456");
		when(mockWrapped.getTableType(other)).thenReturn(TableType.table);

		// call under test
		TableType result = provider.getTableType(other);

		assertEquals(TableType.table, result);
	}

	@Test
	public void testGetColumnModelDelegatesLive() {
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot());
		ColumnModel live = new ColumnModel().setId("11").setName("one");
		when(mockWrapped.getColumnModel("11")).thenReturn(live);

		// call under test
		ColumnModel result = provider.getColumnModel("11");

		assertEquals(live, result);
		verify(mockWrapped).getColumnModel("11");
	}

	@Test
	public void testGetTableSchemaForVersionPinnedQueriedObject() {
		IndexAuthorizationSnapshot snapshot = new IndexAuthorizationSnapshot()
				.setIndexDescription(new IndexDescriptionSnapshot().setObjectId("syn123").setVersionNumber(4L)
						.setTableType(TableType.entityview.name()))
				.setColumnLineage(Collections.singletonList(new ColumnLineageEntry().setOutputColumnId("11")));
		SnapshotSchemaProvider provider = new SnapshotSchemaProvider(mockWrapped, snapshot);
		ColumnModel one = new ColumnModel().setId("11").setName("one");
		when(mockWrapped.getColumnModel("11")).thenReturn(one);

		// call under test
		List<ColumnModel> result = provider.getTableSchema(IdAndVersion.parse("syn123.4"));

		assertEquals(Collections.singletonList(one), result);
	}

	@Test
	public void testConstructorWithNullWrapped() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotSchemaProvider(null, snapshot());
		}).getMessage();
		assertEquals("wrapped is required.", message);
	}

	@Test
	public void testConstructorWithNullSnapshot() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotSchemaProvider(mockWrapped, null);
		}).getMessage();
		assertEquals("snapshot is required.", message);
	}

	@Test
	public void testConstructorWithNullIndexDescription() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotSchemaProvider(mockWrapped, new IndexAuthorizationSnapshot().setIndexDescription(null));
		}).getMessage();
		assertEquals("snapshot.indexDescription is required.", message);
	}
}
