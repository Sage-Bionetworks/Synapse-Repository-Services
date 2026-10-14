package org.sagebionetworks.repo.model.dbo.agent;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_ACT_SETTINGS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_ACT_SETTINGS_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_ACT_SETTINGS_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_ACT_SETTINGS_REGISTRATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_ACT_SETTINGS_SETTINGS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AGENT_REGISTRATION_ACT_SETTINGS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AGENT_REG_ACT_SETTINGS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAgentRegistrationActSettings
		implements MigratableDatabaseObject<DBOAgentRegistrationActSettings, DBOAgentRegistrationActSettings> {

	private Long registrationId;
	private String etag;
	private Timestamp modifiedOn;
	private Long modifiedBy;
	private String settingsJson;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("registrationId", COL_AGENT_REG_ACT_SETTINGS_REGISTRATION_ID).withIsPrimaryKey(true)
					.withIsBackupId(true),
			new FieldColumn("etag", COL_AGENT_REG_ACT_SETTINGS_ETAG).withIsEtag(true),
			new FieldColumn("modifiedOn", COL_AGENT_REG_ACT_SETTINGS_MODIFIED_ON),
			new FieldColumn("modifiedBy", COL_AGENT_REG_ACT_SETTINGS_MODIFIED_BY),
			new FieldColumn("settingsJson", COL_AGENT_REG_ACT_SETTINGS_SETTINGS), };

	@Override
	public TableMapping<DBOAgentRegistrationActSettings> getTableMapping() {
		return new TableMapping<DBOAgentRegistrationActSettings>() {

			@Override
			public DBOAgentRegistrationActSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOAgentRegistrationActSettings()
						.setRegistrationId(rs.getLong(COL_AGENT_REG_ACT_SETTINGS_REGISTRATION_ID))
						.setEtag(rs.getString(COL_AGENT_REG_ACT_SETTINGS_ETAG))
						.setModifiedOn(rs.getTimestamp(COL_AGENT_REG_ACT_SETTINGS_MODIFIED_ON))
						.setModifiedBy(rs.getLong(COL_AGENT_REG_ACT_SETTINGS_MODIFIED_BY))
						.setSettingsJson(rs.getString(COL_AGENT_REG_ACT_SETTINGS_SETTINGS));
			}

			@Override
			public String getTableName() {
				return TABLE_AGENT_REG_ACT_SETTINGS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_AGENT_REGISTRATION_ACT_SETTINGS;
			}

			@Override
			public Class<? extends DBOAgentRegistrationActSettings> getDBOClass() {
				return DBOAgentRegistrationActSettings.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AGENT_REGISTRATION_ACT_SETTINGS;
	}

	@Override
	public MigratableTableTranslation<DBOAgentRegistrationActSettings, DBOAgentRegistrationActSettings> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOAgentRegistrationActSettings> getBackupClass() {
		return DBOAgentRegistrationActSettings.class;
	}

	@Override
	public Class<? extends DBOAgentRegistrationActSettings> getDatabaseObjectClass() {
		return DBOAgentRegistrationActSettings.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getRegistrationId() {
		return registrationId;
	}

	public DBOAgentRegistrationActSettings setRegistrationId(Long registrationId) {
		this.registrationId = registrationId;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOAgentRegistrationActSettings setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOAgentRegistrationActSettings setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public DBOAgentRegistrationActSettings setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
		return this;
	}

	public String getSettingsJson() {
		return settingsJson;
	}

	public DBOAgentRegistrationActSettings setSettingsJson(String settingsJson) {
		this.settingsJson = settingsJson;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, modifiedBy, modifiedOn, registrationId, settingsJson);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOAgentRegistrationActSettings other = (DBOAgentRegistrationActSettings) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(registrationId, other.registrationId)
				&& Objects.equals(settingsJson, other.settingsJson);
	}

	@Override
	public String toString() {
		return "DBOAgentRegistrationActSettings [registrationId=" + registrationId + ", etag=" + etag + ", modifiedOn="
				+ modifiedOn + ", modifiedBy=" + modifiedBy + ", settingsJson=" + settingsJson + "]";
	}

}
