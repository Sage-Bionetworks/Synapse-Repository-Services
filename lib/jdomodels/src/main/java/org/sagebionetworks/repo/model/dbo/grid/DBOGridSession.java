package org.sagebionetworks.repo.model.dbo.grid;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_AUTH_MODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_BENEFACTOR_IDS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_REP_ID_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_REP_ID_SERVICE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SCHEMA_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SESSION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SOURCE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_GRID_SESSION_SOURCE_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_GRID_SESSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_GRID_SESSION;

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

public class DBOGridSession implements MigratableDatabaseObject<DBOGridSession, DBOGridSession> {

	private Long id;
	private String etag;
	private Long createdBy;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private String sessionId;
	private Long repIdClient;
	private Long repIdService;
	private Long sourceId;
	private Long sourceVersion;
	private String schemaId;
	private Long owner;
	private String authorizationMode;
	private String benefactorIds;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_GRID_SESSION_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_GRID_SESSION_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_GRID_SESSION_CREATED_BY),
			new FieldColumn("createdOn", COL_GRID_SESSION_CREATED_ON),
			new FieldColumn("modifiedOn", COL_GRID_SESSION_MODIFIED_ON),
			new FieldColumn("sessionId", COL_GRID_SESSION_SESSION_ID),
			new FieldColumn("repIdClient", COL_GRID_SESSION_REP_ID_CLIENT),
			new FieldColumn("repIdService", COL_GRID_SESSION_REP_ID_SERVICE),
			new FieldColumn("sourceId", COL_GRID_SESSION_SOURCE_ID),
			new FieldColumn("sourceVersion", COL_GRID_SESSION_SOURCE_VERSION),
			new FieldColumn("schemaId", COL_GRID_SESSION_SCHEMA_ID),
			new FieldColumn("owner", COL_GRID_SESSION_OWNER),
			new FieldColumn("authorizationMode", COL_GRID_SESSION_AUTH_MODE),
			new FieldColumn("benefactorIds", COL_GRID_SESSION_BENEFACTOR_IDS), };

	@Override
	public TableMapping<DBOGridSession> getTableMapping() {
		return new TableMapping<DBOGridSession>() {

			@Override
			public DBOGridSession mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOGridSession dbo = new DBOGridSession();
				dbo.setId(rs.getLong(COL_GRID_SESSION_ID));
				dbo.setEtag(rs.getString(COL_GRID_SESSION_ETAG));
				dbo.setCreatedBy(rs.getLong(COL_GRID_SESSION_CREATED_BY));
				dbo.setCreatedOn(rs.getTimestamp(COL_GRID_SESSION_CREATED_ON));
				dbo.setModifiedOn(rs.getTimestamp(COL_GRID_SESSION_MODIFIED_ON));
				dbo.setSessionId(rs.getString(COL_GRID_SESSION_SESSION_ID));
				dbo.setRepIdClient(rs.getLong(COL_GRID_SESSION_REP_ID_CLIENT));
				dbo.setRepIdService(rs.getLong(COL_GRID_SESSION_REP_ID_SERVICE));
				dbo.setSourceId(rs.getLong(COL_GRID_SESSION_SOURCE_ID));
				dbo.setSourceVersion(rs.getLong(COL_GRID_SESSION_SOURCE_VERSION));
				dbo.setSchemaId(rs.getString(COL_GRID_SESSION_SCHEMA_ID));
				dbo.setOwner(rs.getLong(COL_GRID_SESSION_OWNER));
				dbo.setAuthorizationMode(rs.getString(COL_GRID_SESSION_AUTH_MODE));
				dbo.setBenefactorIds(rs.getString(COL_GRID_SESSION_BENEFACTOR_IDS));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_GRID_SESSION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_GRID_SESSION;
			}

			@Override
			public Class<? extends DBOGridSession> getDBOClass() {
				return DBOGridSession.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.GRID_SESSION;
	}

	@Override
	public MigratableTableTranslation<DBOGridSession, DBOGridSession> getTranslator() {
		return new MigratableTableTranslation<DBOGridSession, DBOGridSession>() {
			@TemporaryCode(author = "john.hill@sagebase.org", comment = "Set default owner to be the creator. Can be removed after the first migration.")

			@Override
			public DBOGridSession createDatabaseObjectFromBackup(DBOGridSession backup) {
				if (backup.getOwner() == null) {
					backup.setOwner(backup.getCreatedBy());
				}
				return backup;
			}

			@Override
			public DBOGridSession createBackupFromDatabaseObject(DBOGridSession dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOGridSession> getBackupClass() {
		return DBOGridSession.class;
	}

	@Override
	public Class<? extends DBOGridSession> getDatabaseObjectClass() {
		return DBOGridSession.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public String getAuthorizationMode() {
		return authorizationMode;
	}

	public DBOGridSession setAuthorizationMode(String authorizationMode) {
		this.authorizationMode = authorizationMode;
		return this;
	}

	public String getBenefactorIds() {
		return benefactorIds;
	}

	public DBOGridSession setBenefactorIds(String benefactorIds) {
		this.benefactorIds = benefactorIds;
		return this;
	}

	public Long getId() {
		return id;
	}

	public DBOGridSession setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOGridSession setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOGridSession setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public DBOGridSession setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOGridSession setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public DBOGridSession setSessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public Long getRepIdClient() {
		return repIdClient;
	}

	public DBOGridSession setRepIdClient(Long repIdClient) {
		this.repIdClient = repIdClient;
		return this;
	}

	public Long getRepIdService() {
		return repIdService;
	}

	public DBOGridSession setRepIdService(Long repIdService) {
		this.repIdService = repIdService;
		return this;
	}

	public Long getSourceId() {
		return sourceId;
	}

	public DBOGridSession setSourceId(Long sourceId) {
		this.sourceId = sourceId;
		return this;
	}

	public Long getSourceVersion() {
		return sourceVersion;
	}

	public DBOGridSession setSourceVersion(Long sourceVersion) {
		this.sourceVersion = sourceVersion;
		return this;
	}

	public String getSchemaId() {
		return schemaId;
	}

	public DBOGridSession setSchemaId(String schemaId) {
		this.schemaId = schemaId;
		return this;
	}

	public Long getOwner() {
		return owner;
	}

	public DBOGridSession setOwner(Long owner) {
		this.owner = owner;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(authorizationMode, benefactorIds, createdBy, createdOn, etag, id, modifiedOn, owner,
				repIdClient, repIdService, schemaId, sessionId, sourceId, sourceVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOGridSession other = (DBOGridSession) obj;
		return Objects.equals(authorizationMode, other.authorizationMode)
				&& Objects.equals(benefactorIds, other.benefactorIds)
				&& Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(owner, other.owner)
				&& Objects.equals(repIdClient, other.repIdClient) && Objects.equals(repIdService, other.repIdService)
				&& Objects.equals(schemaId, other.schemaId) && Objects.equals(sessionId, other.sessionId)
				&& Objects.equals(sourceId, other.sourceId) && Objects.equals(sourceVersion, other.sourceVersion);
	}

	@Override
	public String toString() {
		return "DBOGridSession [id=" + id + ", etag=" + etag + ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedOn=" + modifiedOn + ", sessionId=" + sessionId + ", repIdClient=" + repIdClient
				+ ", repIdService=" + repIdService + ", sourceId=" + sourceId + ", sourceVersion=" + sourceVersion
				+ ", schemaId=" + schemaId + ", authorizationMode=" + authorizationMode + ", benefactorIds="
				+ benefactorIds + "]";
	}

}
