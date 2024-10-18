package org.sagebionetworks.repo.model.dbo.agent;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_AWS_AGENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_AWS_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_REGISTRATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_REG_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AGENT_REGISTRATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AGENT_REGISTRATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAgentRegistration implements MigratableDatabaseObject<DBOAgentRegistration, DBOAgentRegistration> {
	
	private Long registrationId;
	private String awsAgentId;
	private String awsAliasId;
	private Long createdOn;
	private String type;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("registrationId", COL_AGENT_REG_REGISTRATION_ID).withIsPrimaryKey(true)
					.withIsBackupId(true),
			new FieldColumn("awsAgentId", COL_AGENT_REG_AWS_AGENT_ID),
			new FieldColumn("awsAliasId", COL_AGENT_REG_AWS_ALIAS_ID),
			new FieldColumn("createdOn", COL_AGENT_REG_CREATED_ON),
			new FieldColumn("type", COL_AGENT_REG_TYPE), };

	@Override
	public TableMapping<DBOAgentRegistration> getTableMapping() {
		return new TableMapping<DBOAgentRegistration>() {

			@Override
			public DBOAgentRegistration mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOAgentRegistration().setRegistrationId(rs.getLong(COL_AGENT_REG_REGISTRATION_ID))
						.setAwsAgentId(rs.getString(COL_AGENT_REG_AWS_AGENT_ID))
						.setAwsAliasId(rs.getString(COL_AGENT_REG_AWS_ALIAS_ID))
						.setCreatedOn(rs.getLong(COL_AGENT_REG_CREATED_ON));
			}

			@Override
			public String getTableName() {
				return TABLE_AGENT_REGISTRATION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_AGENT_REGISTRATION;
			}

			@Override
			public Class<? extends DBOAgentRegistration> getDBOClass() {
				return DBOAgentRegistration.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AGENT_REGISTRATION;
	}

	@Override
	public MigratableTableTranslation<DBOAgentRegistration, DBOAgentRegistration> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOAgentRegistration> getBackupClass() {
		return DBOAgentRegistration.class;
	}

	@Override
	public Class<? extends DBOAgentRegistration> getDatabaseObjectClass() {
		return DBOAgentRegistration.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getRegistrationId() {
		return registrationId;
	}

	public DBOAgentRegistration setRegistrationId(Long registrationId) {
		this.registrationId = registrationId;
		return this;
	}

	public String getAwsAgentId() {
		return awsAgentId;
	}

	public DBOAgentRegistration setAwsAgentId(String awsAgentId) {
		this.awsAgentId = awsAgentId;
		return this;
	}

	public String getAwsAliasId() {
		return awsAliasId;
	}

	public DBOAgentRegistration setAwsAliasId(String awsAliasId) {
		this.awsAliasId = awsAliasId;
		return this;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public DBOAgentRegistration setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public String getType() {
		return type;
	}

	public DBOAgentRegistration setType(String type) {
		this.type = type;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(awsAgentId, awsAliasId, createdOn, registrationId, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOAgentRegistration other = (DBOAgentRegistration) obj;
		return Objects.equals(awsAgentId, other.awsAgentId) && Objects.equals(awsAliasId, other.awsAliasId)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(registrationId, other.registrationId)
				&& Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "DBOAgentRegistration [registrationId=" + registrationId + ", awsAgentId=" + awsAgentId + ", awsAliasId="
				+ awsAliasId + ", createdOn=" + createdOn + ", type=" + type + "]";
	}

}
