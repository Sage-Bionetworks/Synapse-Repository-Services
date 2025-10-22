package org.sagebionetworks.repo.manager.grid.internal.replica;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.node.IndexType;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class ReplicaChangeSetTest {

	private GridConnectionInfo connection;
	private LogicalTimestamp patchId;
	private Map<IndexType, Set<LogicalTimestamp>> map;

	@BeforeEach
	public void before() {
		connection = new GridConnectionInfo().setConnectionId("con123").setReplicaId(2L).setSessionId("session444");
		patchId = new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(99L);
		map = new LinkedHashMap<>();
		List<LogicalTimestamp> arrs = List.of(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(110L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(8L),
				new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(112L));
		map.put(IndexType.arr, new LinkedHashSet<>(arrs));
		List<LogicalTimestamp> cons = List.of(new LogicalTimestamp().setReplicaId(3L).setSequenceNumber(113L),
				new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(10L));
		map.put(IndexType.con, new LinkedHashSet<>(cons));
		map.put(IndexType.obj, Collections.emptySet());
	}

	@Test
	public void testToAndFromJson() {

		// call under test
		ReplicaChangeSet rcs = new ReplicaChangeSet(connection, patchId, map);
		// call under test
		String json = rcs.toJson();
		assertEquals("{\"sessionId\":\"session444\",\"replicaId\":2,\"patchId\":[3,99],"
				+ "\"changes\":{\"arr\":[110,[2,8],112],\"con\":[113,[2,10]],\"obj\":[]}}", json);

		// call under test
		ReplicaChangeSet clone = new ReplicaChangeSet(json);
		assertEquals(rcs, clone);
	}

	@Test
	public void testToAndFromJsonWithNull() {

		// call under test
		ReplicaChangeSet rcs = new ReplicaChangeSet(connection, patchId, null);
		// call under test
		String json = rcs.toJson();
		assertEquals("{\"sessionId\":\"session444\",\"replicaId\":2,\"patchId\":[3,99]}", json);

		// call under test
		ReplicaChangeSet clone = new ReplicaChangeSet(json);
		assertEquals(rcs, clone);
	}

}
