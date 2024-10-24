package org.sagebionetworks.repo.model.dbo.agent;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_ACCESS_LEVEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_REGISTRATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AGENT_SESSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AGENT_SESSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.util.TemporaryCode;

public class DBOAgentSession implements MigratableDatabaseObject<DBOAgentSession, DBOAgentSession> {
	
	public static final Long BOOTSTRAP_REGISTRATION_ID = 1L;

	private Long id;
	private String etag;
	private Long createdBy;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private String sessionId;
	// will be removed after stack-518
	@Deprecated
	private String agentId;
	private Long registrationId;
	private String accessLevel;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_AGENT_SESSION_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_AGENT_SESSION_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_AGENT_SESSION_CREATED_BY),
			new FieldColumn("createdOn", COL_AGENT_SESSION_CREATED_ON),
			new FieldColumn("modifiedOn", COL_AGENT_SESSION_MODIFIED_ON),
			new FieldColumn("sessionId", COL_AGENT_SESSION_SESSION_ID),
			new FieldColumn("registrationId", COL_AGENT_SESSION_REGISTRATION_ID),
			new FieldColumn("accessLevel", COL_AGENT_SESSION_ACCESS_LEVEL), };

	@Override
	public TableMapping<DBOAgentSession> getTableMapping() {
		return new TableMapping<DBOAgentSession>() {

			@Override
			public DBOAgentSession mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOAgentSession().setId(rs.getLong(COL_AGENT_SESSION_ID))
						.setEtag(rs.getString(COL_AGENT_SESSION_ETAG))
						.setCreatedBy(rs.getLong(COL_AGENT_SESSION_CREATED_BY))
						.setCreatedOn(rs.getTimestamp(COL_AGENT_SESSION_CREATED_ON))
						.setModifiedOn(rs.getTimestamp(COL_AGENT_SESSION_MODIFIED_ON))
						.setSessionId(rs.getString(COL_AGENT_SESSION_SESSION_ID))
						.setRegistrationId(rs.getLong(COL_AGENT_SESSION_REGISTRATION_ID))
						.setAccessLevel(rs.getString(COL_AGENT_SESSION_ACCESS_LEVEL));
			}

			@Override
			public String getTableName() {
				return TABLE_AGENT_SESSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_AGENT_SESSION;
			}

			@Override
			public Class<? extends DBOAgentSession> getDBOClass() {
				return DBOAgentSession.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AGENT_SESSION;
	}

	@Override
	public MigratableTableTranslation<DBOAgentSession, DBOAgentSession> getTranslator() {
		return new MigratableTableTranslation<DBOAgentSession, DBOAgentSession>() {
			
			@TemporaryCode(author = "john", comment = "Can be removed after the migration from agentId to registrationId is complete")
			@Override
			public DBOAgentSession createDatabaseObjectFromBackup(DBOAgentSession backup) {
				if(backup.getAgentId() != null && backup.getRegistrationId() == null) {
					backup.setRegistrationId(BOOTSTRAP_REGISTRATION_ID);
				}
				return backup;
			}
			
			@Override
			public DBOAgentSession createBackupFromDatabaseObject(DBOAgentSession dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOAgentSession> getBackupClass() {
		return DBOAgentSession.class;
	}

	@Override
	public Class<? extends DBOAgentSession> getDatabaseObjectClass() {
		return DBOAgentSession.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOAgentSession setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOAgentSession setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOAgentSession setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOAgentSession setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOAgentSession setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public DBOAgentSession setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public String getAgentId() {
		return agentId;
	}

	public DBOAgentSession setAgentId(String agentId) {
		this.agentId = agentId;
		return this;
	}

	public Long getRegistrationId() {
		return registrationId;
	}

	public DBOAgentSession setRegistrationId(Long registrationId) {
		this.registrationId = registrationId;
		return this;
	}

	public String getAccessLevel() {
		return accessLevel;
	}

	public DBOAgentSession setAccessLevel(String accessLevel) {
		this.accessLevel = accessLevel;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessLevel, agentId, createdBy, createdOn, etag, id, modifiedOn, registrationId,
				sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOAgentSession other = (DBOAgentSession) obj;
		return Objects.equals(accessLevel, other.accessLevel) && Objects.equals(agentId, other.agentId)
				&& Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(registrationId, other.registrationId)
				&& Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "DBOAgentSession [id=" + id + ", etag=" + etag + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedOn=" + modifiedOn + ", sessionId=" + sessionId + ", agentId=" + agentId
				+ ", registrationId=" + registrationId + ", accessLevel=" + accessLevel + "]";
	}

}
