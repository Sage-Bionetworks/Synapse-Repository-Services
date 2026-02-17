package org.sagebionetworks.repo.model.grid.node;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class IndexNodeTest {

	private LogicalTimestamp id;

	@BeforeEach
	public void before() {
		id = new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L);
	}

	@Test
	public void testStreamReferencedTimestamps() {
		IndexNode index = new IndexNode().setId(id).setType(IndexType.con);

		// call under test
		List<LogicalTimestamp> timestamps = index.streamReferencedTimestamps().collect(Collectors.toList());

		assertEquals(1, timestamps.size());
		assertEquals(id, timestamps.get(0)); // only node ID
	}

}
