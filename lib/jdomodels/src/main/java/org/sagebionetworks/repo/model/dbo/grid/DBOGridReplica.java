package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_CREATE_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_IS_AGENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SEQUENCE_HIGH_WATER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_REPLICA_SOURCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_GRID_REPLICA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GRID_REPLICA;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

public class DBOGridReplica implements MigratableDatabaseObject<DBOGridReplica, DBOGridReplica> {

	private Long id;
	private String etag;
	private String replicaId;
	private Long createdBy;
	private Timestamp createdOn;
	private String sessionId;
	private Boolean isAgent;
	private Long sequenceHighWater;
	private String source;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_GRID_REPLICA_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_GRID_REPLICA_ETAG).withIsEtag(true),
			new FieldColumn("replicaId", COL_GRID_REPLICA_REPLICA_ID),
			new FieldColumn("createdBy", COL_GRID_REPLICA_CREATE_BY),
			new FieldColumn("createdOn", COL_GRID_REPLICA_CREATE_ON),
			new FieldColumn("sessionId", COL_GRID_REPLICA_SESSION_ID),
			new FieldColumn("isAgent", COL_GRID_REPLICA_IS_AGENT),
			new FieldColumn("sequenceHighWater", COL_GRID_REPLICA_SEQUENCE_HIGH_WATER),
			new FieldColumn("source", COL_GRID_REPLICA_SOURCE), };

	private static final MigratableTableTranslation<DBOGridReplica, DBOGridReplica> TRANSLATOR = new BasicMigratableTableTranslation<>() {

		@Override
		@TemporaryCode(author = "john.hill", comment = "PLFM-10043: can be removed once production backups include the etag")
		public DBOGridReplica createDatabaseObjectFromBackup(DBOGridReplica backup) {
			if (backup.getEtag() == null) {
				backup.setEtag(UUID.randomUUID().toString());
			}
			return backup;
		}
	};

	@Override
	public TableMapping<DBOGridReplica> getTableMapping() {
		return new TableMapping<DBOGridReplica>() {

			@Override
			public DBOGridReplica mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGridReplica dbo = new DBOGridReplica();
				dbo.setId(rs.getLong(COL_GRID_REPLICA_ID));
				dbo.setEtag(rs.getString(COL_GRID_REPLICA_ETAG));
				dbo.setReplicaId(rs.getString(COL_GRID_REPLICA_REPLICA_ID));
				dbo.setCreatedBy(rs.getLong(COL_GRID_REPLICA_CREATE_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_GRID_REPLICA_CREATE_ON));
				dbo.setSessionId(rs.getString(COL_GRID_REPLICA_SESSION_ID));
				dbo.setIsAgent(rs.getBoolean(COL_GRID_REPLICA_IS_AGENT));
				long sequenceHighWater = rs.getLong(COL_GRID_REPLICA_SEQUENCE_HIGH_WATER);
				dbo.setSequenceHighWater(rs.wasNull() ? null : sequenceHighWater);
				dbo.setSource(rs.getString(COL_GRID_REPLICA_SOURCE));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GRID_REPLICA;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_GRID_REPLICA;
			}

			@Override
			public Class<? extends DBOGridReplica> getDBOClass() {
				return DBOGridReplica.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.GRID_REPLICA;
	}

	@Override
	public MigratableTableTranslation<DBOGridReplica, DBOGridReplica> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOGridReplica> getBackupClass() {
		return DBOGridReplica.class;
	}

	@Override
	public Class<? extends DBOGridReplica> getDatabaseObjectClass() {
		return DBOGridReplica.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getReplicaId() {
		return replicaId;
	}

	public void setReplicaId(String replicaId) {
		this.replicaId = replicaId;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Boolean getIsAgent() {
		return isAgent;
	}

	public void setIsAgent(Boolean isAgent) {
		this.isAgent = isAgent;
	}

	public Long getSequenceHighWater() {
		return sequenceHighWater;
	}

	public void setSequenceHighWater(Long sequenceHighWater) {
		this.sequenceHighWater = sequenceHighWater;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, etag, id, isAgent, replicaId, sequenceHighWater, sessionId, source);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOGridReplica other = (DBOGridReplica) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(isAgent, other.isAgent) && Objects.equals(replicaId, other.replicaId)
				&& Objects.equals(sequenceHighWater, other.sequenceHighWater)
				&& Objects.equals(sessionId, other.sessionId) && Objects.equals(source, other.source);
	}

	@Override
	public String toString() {
		return "DBOGridReplica [id=" + id + ", etag=" + etag + ", replicaId=" + replicaId + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + ", sessionId=" + sessionId + ", isAgent=" + isAgent
				+ ", sequenceHighWater=" + sequenceHighWater + ", source=" + source + "]";
	}

}
