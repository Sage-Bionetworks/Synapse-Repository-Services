package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.ClasspathUtil;

@ExtendWith(MockitoExtension.class)
public class PatchRowHandlerTest {

	@Mock
	private PatchStore mockStore;

	private String sessionId;
	private Long replicaId;
	private List<ColumnModel> schema;
	private Long maxRowSizeBytes;
	private List<Integer> requiredColumnIndices;

	@BeforeEach
	public void before() {
		sessionId = "s123";
		replicaId = 19L;
		schema = List.of(new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));
		maxRowSizeBytes = 100L;
		requiredColumnIndices = Collections.emptyList();
	}

	@Test
	public void testNoColumnsNoRows() throws IOException {
		schema = Collections.emptyList();
		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowSizeBytes, requiredColumnIndices)) {
			// no row to add
			assertEquals(PatchUtils.calculateRowsPerPatch(maxRowSizeBytes), handler.getRowsPerPatch());
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// This patch has been tested with the JSON-Joy TypeScript library.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.1.0\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
						+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1]]");
	}

	@Test
	public void testWithColumnNoRows() throws IOException {

		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowSizeBytes, requiredColumnIndices)) {
			// no row to add
			assertEquals(PatchUtils.calculateRowsPerPatch(maxRowSizeBytes), handler.getRowsPerPatch());
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// This patch has been tested with the JSON-Joy TypeScript library.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.1.0\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
						+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"],"
						+ "[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]]]");
	}

	@Test
	public void testWithRows() throws IOException {

		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowSizeBytes, requiredColumnIndices)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101")).setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
			handler.nextRow(new Row().setValues(Arrays.asList("two", "202")).setRowId(2L).setVersionNumber(5L).setEtag("fake-etag-2"));
			handler.nextRow(new Row().setValues(Arrays.asList("three", "303")).setRowId(3L).setVersionNumber(6L).setEtag("fake-etag-3"));
			// A row with partial metadata
			handler.nextRow(new Row().setValues(Arrays.asList("four", "404")).setRowId(3L).setEtag("fake-etag-4"));
			// A row with no metadata
			handler.nextRow(new Row().setValues(Arrays.asList("five", "505")));
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// This patch has been tested with the JSON-Joy TypeScript library.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
		 "[[[19,1]],[2],[0,\"0.1.0\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
				 + "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"],"
				 + "[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]],[2],"
				 + "[3],[0,\"one\"],[0,101],[11,16,[[0,17],[1,18]]],[2],[0,[1,4,\"fake-etag-1\"]],[10,20,[[\"synapseRow\",21]]],[10,15,[[\"data\",16],[\"metadata\",20]]],[14,5,5,[15]],[2],"
				 + "[3],[0,\"two\"],[0,202],[11,26,[[0,27],[1,28]]],[2],[0,[2,5,\"fake-etag-2\"]],[10,30,[[\"synapseRow\",31]]],[10,25,[[\"data\",26],[\"metadata\",30]]],[14,5,24,[25]],[2],"
				 + "[3],[0,\"three\"],[0,303],[11,36,[[0,37],[1,38]]],[2],[0,[3,6,\"fake-etag-3\"]],[10,40,[[\"synapseRow\",41]]],[10,35,[[\"data\",36],[\"metadata\",40]]],[14,5,34,[35]],[2],"
				 + "[3],[0,\"four\"],[0,404],[11,46,[[0,47],[1,48]]],[2],[0,[3,null,\"fake-etag-4\"]],[10,50,[[\"synapseRow\",51]]],[10,45,[[\"data\",46],[\"metadata\",50]]],[14,5,44,[45]],[2],"
				 + "[3],[0,\"five\"],[0,505],[11,56,[[0,57],[1,58]]],[10,55,[[\"data\",56]]],[14,5,54,[55]]]"
		);
	}

	@Test
	public void testWithRowsWithOneRowPerPatch() throws IOException {
		maxRowSizeBytes = Long.MAX_VALUE;
		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowSizeBytes, requiredColumnIndices)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101")).setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
			handler.nextRow(new Row().setValues(Arrays.asList("two", "202")).setRowId(2L).setVersionNumber(5L).setEtag("fake-etag-2"));
			handler.nextRow(new Row().setValues(Arrays.asList("three", "303")).setRowId(3L).setVersionNumber(6L).setEtag("fake-etag-3"));
			assertEquals(PatchUtils.calculateRowsPerPatch(maxRowSizeBytes), handler.getRowsPerPatch());
		}
		verify(mockStore, times(3)).savePatch(any(), any(), any());
		// All three patch has been tested with the JSON-Joy TypeScript library.
		// The first patch includes the grid setup and the first row
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.1.0\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
				+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"],"
				+ "[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]],[2],[3],[0,\"one\"],[0,101],[11,16,[[0,17],[1,18]]],[2],"
				+ "[0,[1,4,\"fake-etag-1\"]],[10,20,[[\"synapseRow\",21]]],[10,15,[[\"data\",16],[\"metadata\",20]]],[14,5,5,[15]]]");
		// second patch includes only the second row.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(25L),
				"[[[19,25]],[2],[3],[0,\"two\"],[0,202],[11,26,[[0,27],[1,28]]],[2],"
				+ "[0,[2,5,\"fake-etag-2\"]],"
				+ "[10,30,[[\"synapseRow\",31]]],[10,25,[[\"data\",26],[\"metadata\",30]]],[14,5,24,[25]]]");
		// last patch includes only the last row.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(35L),
				"[[[19,35]],[2],[3],[0,\"three\"],[0,303],[11,36,[[0,37],[1,38]]],[2],"
				+ "[0,[3,6,\"fake-etag-3\"]],"
				+ "[10,40,[[\"synapseRow\",41]]],[10,35,[[\"data\",36],[\"metadata\",40]]],[14,5,34,[35]]]");

	}

	@Test
	public void testEachType() throws IOException {
		boolean hasDefault = false;
		schema = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(schema, 3,
				new TableModelTestUtils.ValueOptions().includeSpace(false));

		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowSizeBytes, requiredColumnIndices)) {
			rows.forEach(r -> {
				handler.nextRow(r);
			});
			assertEquals(PatchUtils.calculateRowsPerPatch(maxRowSizeBytes), handler.getRowsPerPatch());
		}
		String expectedPatch = ClasspathUtil.loadFromClasspath("AllTypesPatch.json");
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				expectedPatch);
	}

	@Test
	public void testWriteNullOrUndefinedUsingRequiredColumnIndices() throws Exception {
		requiredColumnIndices = List.of(1); // only the second column is required
		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowSizeBytes, requiredColumnIndices)) {
			handler.nextRow(new Row().setValues(Arrays.asList(null, null)).setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
			assertEquals(PatchUtils.calculateRowsPerPatch(maxRowSizeBytes), handler.getRowsPerPatch());
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// The first value should be an UNDEFINED constant, the second should be NULL
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.1.0\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],[\"columnOrder\",4]," +
						"[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"]," +
						"[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]],[2],[3]," +
						"[0]," + // the `undefined` constant
						"[0,null]," + // the `null` constant
						"[11,16,[[0,17],[1,18]]],[2]," +
						"[0,[1,4,\"fake-etag-1\"]],[10,20,[[\"synapseRow\",21]]],[10,15,[[\"data\",16],[\"metadata\",20]]],[14,5,5,[15]]]");
	}

}
