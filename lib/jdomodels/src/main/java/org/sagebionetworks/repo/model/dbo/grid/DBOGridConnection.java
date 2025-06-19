package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_CONNECTION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_REPLICA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_CON_SOURCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_GRID_CONNECTION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GRID_CONNECTION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOGridConnection implements DatabaseObject<DBOGridConnection> {

	private String connectionId;
	private String sessionId;
	private Long replicaId;
	private Long createdBy;
	private Timestamp createdOn;
	private String source;


	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("connectionId", COL_GRID_CON_CONNECTION_ID).withIsPrimaryKey(true),
			new FieldColumn("sessionId", COL_GRID_CON_SESSION_ID),
			new FieldColumn("replicaId", COL_GRID_CON_REPLICA_ID),
			new FieldColumn("createdBy", COL_GRID_CON_CREATED_BY),
			new FieldColumn("createdOn", COL_GRID_CON_CREATED_ON),
			new FieldColumn("source", COL_GRID_CON_SOURCE), };

	@Override
	public TableMapping<DBOGridConnection> getTableMapping() {
		return new TableMapping<DBOGridConnection>() {

			@Override
			public DBOGridConnection mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGridConnection dbo = new DBOGridConnection();
				dbo.setConnectionId(rs.getString(COL_GRID_CON_CONNECTION_ID));
				dbo.setSessionId(rs.getString(COL_GRID_CON_SESSION_ID));
				dbo.setReplciaId(rs.getLong(COL_GRID_CON_REPLICA_ID));
				dbo.setCreatedBy(rs.getLong(COL_GRID_CON_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_GRID_CON_CREATED_ON));
				dbo.setSource(rs.getString(COL_GRID_CON_SOURCE));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GRID_CONNECTION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_GRID_CONNECTION;
			}

			@Override
			public Class<? extends DBOGridConnection> getDBOClass() {
				return DBOGridConnection.class;
			}
		};
	}

	public String getConnectionId() {
		return connectionId;
	}

	public DBOGridConnection setConnectionId(String connectionId) {
		this.connectionId = connectionId;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public DBOGridConnection setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Long getReplciaId() {
		return replicaId;
	}

	public DBOGridConnection setReplciaId(Long replicaId) {
		this.replicaId = replicaId;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOGridConnection setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public DBOGridConnection setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public String getSource() {
		return source;
	}

	public DBOGridConnection setSource(String source) {
		this.source = source;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionId, createdBy, createdOn, replicaId, sessionId, source);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOGridConnection other = (DBOGridConnection) obj;
		return Objects.equals(connectionId, other.connectionId) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(replicaId, other.replicaId)
				&& Objects.equals(sessionId, other.sessionId) && Objects.equals(source, other.source);
	}

	@Override
	public String toString() {
		return "DBOConnection [connectionId=" + connectionId + ", sessionId=" + sessionId + ", replicaId=" + replicaId
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn + ", source=" + source + "]";
	}



}
