package org.sagebionetworks.repo.model.dbo.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_IS_AGENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SEQUENCE_HIGH_WATER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SOURCE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DBOGridReplicaTest {

	@Mock
	private ResultSet mockResultSet;

	private DBOGridReplica backup;

	@BeforeEach
	public void before() {
		backup = new DBOGridReplica();
		backup.setId(123L);
		backup.setReplicaId("66533");
		backup.setCreatedBy(1L);
		backup.setCreatedOn(new Timestamp(1000L));
		backup.setSessionId("MTE5NjY5Ng==");
		backup.setIsAgent(false);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithNullEtag() {
		// a backup taken before the etag and high-water columns existed
		backup.setEtag(null);
		backup.setSequenceHighWater(null);

		// call under test
		DBOGridReplica dbo = new DBOGridReplica().getTranslator().createDatabaseObjectFromBackup(backup);

		assertNotNull(dbo.getEtag());
		assertNull(dbo.getSequenceHighWater());
		assertEquals("66533", dbo.getReplicaId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithEtag() {
		backup.setEtag("an-etag");
		backup.setSequenceHighWater(60L);

		// call under test
		DBOGridReplica dbo = new DBOGridReplica().getTranslator().createDatabaseObjectFromBackup(backup);

		assertEquals("an-etag", dbo.getEtag());
		assertEquals(60L, dbo.getSequenceHighWater());
	}

	@Test
	public void testMapRow() throws SQLException {
		when(mockResultSet.getLong(COL_GRID_REPLICA_ID)).thenReturn(123L);
		when(mockResultSet.getString(COL_GRID_REPLICA_ETAG)).thenReturn("an-etag");
		when(mockResultSet.getString(COL_GRID_REPLICA_REPLICA_ID)).thenReturn("66533");
		when(mockResultSet.getLong(COL_GRID_REPLICA_CREATE_BY)).thenReturn(1L);
		when(mockResultSet.getTimestamp(COL_GRID_REPLICA_CREATE_ON)).thenReturn(new Timestamp(1000L));
		when(mockResultSet.getString(COL_GRID_REPLICA_SESSION_ID)).thenReturn("MTE5NjY5Ng==");
		when(mockResultSet.getBoolean(COL_GRID_REPLICA_IS_AGENT)).thenReturn(false);
		when(mockResultSet.getLong(COL_GRID_REPLICA_SEQUENCE_HIGH_WATER)).thenReturn(60L);
		when(mockResultSet.wasNull()).thenReturn(false);
		when(mockResultSet.getString(COL_GRID_REPLICA_SOURCE)).thenReturn("VALIDATION");

		DBOGridReplica expected = backup;
		expected.setEtag("an-etag");
		expected.setSequenceHighWater(60L);
		expected.setSource("VALIDATION");

		// call under test
		DBOGridReplica dbo = new DBOGridReplica().getTableMapping().mapRow(mockResultSet, 0);

		assertEquals(expected, dbo);
	}
}
