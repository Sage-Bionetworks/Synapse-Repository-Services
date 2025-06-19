package org.sagebionetworks.repo.manager.grid;

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
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;

@ExtendWith(MockitoExtension.class)
public class PatchRowHandlerTest {

	@Mock
	private PatchStore mockStore;

	private String sessionId;
	private Long replicaId;
	private List<ColumnModel> schema;
	private int maxRowsPerPatch;

	@BeforeEach
	public void before() {
		sessionId = "s123";
		replicaId = 19L;
		schema = List.of(new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));
		maxRowsPerPatch = 100;
	}

	@Test
	public void testNoColumnsNoRows() throws IOException {
		schema = Collections.emptyList();
		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowsPerPatch)) {
			// no row to add
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// This patch has been tested with the JSON-Joy TypeScript library.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.0.2\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
						+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1]]");
	}

	@Test
	public void testWithColumnNoRows() throws IOException {

		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowsPerPatch)) {
			// no row to add
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// This patch has been tested with the JSON-Joy TypeScript library.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.0.2\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
						+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"],"
						+ "[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]]]");
	}

	@Test
	public void testWithRows() throws IOException {

		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowsPerPatch)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101")));
			handler.nextRow(new Row().setValues(Arrays.asList("two", "202")));
			handler.nextRow(new Row().setValues(Arrays.asList("three", "303")));
		}
		verify(mockStore, times(1)).savePatch(any(), any(), any());
		// This patch has been tested with the JSON-Joy TypeScript library.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.0.2\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
						+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"],"
						+ "[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]],"
						+ "[3],[0,\"one\"],[0,101],[11,15,[[0,16],[1,17]]],[14,5,5,[15]],"
						+ "[3],[0,\"two\"],[0,202],[11,20,[[0,21],[1,22]]],[14,5,19,[20]],"
						+ "[3],[0,\"three\"],[0,303],[11,25,[[0,26],[1,27]]],[14,5,24,[25]]]");
	}

	@Test
	public void testWithRowsWithOneRowPerPatch() throws IOException {
		maxRowsPerPatch = 1;
		// call under test
		try (PatchRowHandler handler = new PatchRowHandler(mockStore, sessionId, replicaId, schema, maxRowsPerPatch)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101")));
			handler.nextRow(new Row().setValues(Arrays.asList("two", "202")));
			handler.nextRow(new Row().setValues(Arrays.asList("three", "303")));
		}
		verify(mockStore, times(3)).savePatch(any(), any(), any());
		// All three patch has been tested with the JSON-Joy TypeScript library.
		// The first patch includes the grid setup and the first row
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L),
				"[[[19,1]],[2],[0,\"0.0.2\"],[3],[6],[6],[10,1,[[\"doc_version\",2],[\"columnNames\",3],"
				+ "[\"columnOrder\",4],[\"rows\",5]]],[9,[0,0],1],[0,\"aString\"],[0,0],[0,\"anInt\"],"
				+ "[0,1],[11,3,[[0,8],[1,10]]],[14,4,4,[9,11]],[3],[0,\"one\"],[0,101],[11,15,[[0,16],[1,17]]],[14,5,5,[15]]]");
		// second patch includes only the second row.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(20L),
				"[[[19,20]],[3],[0,\"two\"],[0,202],[11,20,[[0,21],[1,22]]],[14,5,19,[20]]]");
		// last patch includes only the last row.
		verify(mockStore).savePatch(sessionId, new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(25L),
				"[[[19,25]],[3],[0,\"three\"],[0,303],[11,25,[[0,26],[1,27]]],[14,5,24,[25]]]");

	}

}
