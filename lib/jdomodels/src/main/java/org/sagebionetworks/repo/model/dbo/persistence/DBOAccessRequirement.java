/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ACCESS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CONCRETE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_IDENTITY_PROVIDERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_ACCESS_REQUIREMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ACCESS_REQUIREMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * @author brucehoff
 *
 */
public class DBOAccessRequirement implements MigratableDatabaseObject<DBOAccessRequirement, DBOAccessRequirement> {
	private Long id;
	private String eTag;
	private String name;
	private Long createdBy;
	private long createdOn;
	private String accessType;
	private String concreteType;
	private Long currentRevNumber;
	private Boolean isTwoFaRequired;
	private String identityProviders;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_ACCESS_REQUIREMENT_ID, true).withIsBackupId(true),
		new FieldColumn("eTag", COL_ACCESS_REQUIREMENT_ETAG).withIsEtag(true),
		new FieldColumn("name", COL_ACCESS_REQUIREMENT_NAME),
		new FieldColumn("currentRevNumber", COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER),
		new FieldColumn("createdBy", COL_ACCESS_REQUIREMENT_CREATED_BY),
		new FieldColumn("createdOn", COL_ACCESS_REQUIREMENT_CREATED_ON),
		new FieldColumn("accessType", COL_ACCESS_REQUIREMENT_ACCESS_TYPE),
		new FieldColumn("concreteType", COL_ACCESS_REQUIREMENT_CONCRETE_TYPE),
		new FieldColumn("isTwoFaRequired", COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED),
		new FieldColumn("identityProviders", COL_ACCESS_REQUIREMENT_IDENTITY_PROVIDERS)
	};

	private static final MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirement> MIGRATION_MAPPER = new BasicMigratableTableTranslation<>() {
		
		@Override
		public DBOAccessRequirement createDatabaseObjectFromBackup(DBOAccessRequirement backup) {
			if (backup.getIsTwoFaRequired() == null) {
				backup.setIsTwoFaRequired(false);
			}
			return backup;
		}
	};

	@Override
	public TableMapping<DBOAccessRequirement> getTableMapping() {
		return new TableMapping<DBOAccessRequirement>() {
			// Map a result set to this object
			@Override
			public DBOAccessRequirement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOAccessRequirement ar = new DBOAccessRequirement();
				ar.setId(rs.getLong(COL_ACCESS_REQUIREMENT_ID));
				ar.seteTag(rs.getString(COL_ACCESS_REQUIREMENT_ETAG));
				ar.setName(rs.getString(COL_ACCESS_REQUIREMENT_NAME));
				ar.setCurrentRevNumber(rs.getLong(COL_ACCESS_REQUIREMENT_CURRENT_REVISION_NUMBER));
				ar.setCreatedBy(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_BY));
				ar.setCreatedOn(rs.getLong(COL_ACCESS_REQUIREMENT_CREATED_ON));
				ar.setAccessType(rs.getString(COL_ACCESS_REQUIREMENT_ACCESS_TYPE));
				ar.setConcreteType(rs.getString(COL_ACCESS_REQUIREMENT_CONCRETE_TYPE));
				ar.setIsTwoFaRequired(rs.getBoolean(COL_ACCESS_REQUIREMENT_IS_TWO_FA_REQUIRED));
				ar.setIdentityProviders(rs.getString(COL_ACCESS_REQUIREMENT_IDENTITY_PROVIDERS));
				return ar;
			}

			@Override
			public String getTableName() {
				return TABLE_ACCESS_REQUIREMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_ACCESS_REQUIREMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOAccessRequirement> getDBOClass() {
				return DBOAccessRequirement.class;
			}
		};
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String geteTag() {
		return eTag;
	}


	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public Long getCurrentRevNumber() {
		return currentRevNumber;
	}


	public void setCurrentRevNumber(Long currentRevNumber) {
		this.currentRevNumber = currentRevNumber;
	}


	public Long getCreatedBy() {
		return createdBy;
	}


	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public String getAccessType() {
		return accessType;
	}


	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}

	public String getConcreteType() {
		return concreteType;
	}


	public void setConcreteType(String concreteType) {
		this.concreteType = concreteType;
	}
	
	public Boolean getIsTwoFaRequired() {
		return isTwoFaRequired;
	}
	
	public void setIsTwoFaRequired(Boolean isTwoFaRequired) {
		this.isTwoFaRequired = isTwoFaRequired;
	}

	/**
	 * @return The JSON array of the identity provider names that an accessor must have authenticated
	 *         through, or null if the requirement does not restrict how the accessor authenticated.
	 */
	public String getIdentityProviders() {
		return identityProviders;
	}

	public void setIdentityProviders(String identityProviders) {
		this.identityProviders = identityProviders;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessType, concreteType, createdBy, createdOn, currentRevNumber, eTag, id, isTwoFaRequired,
				identityProviders, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOAccessRequirement)) {
			return false;
		}
		DBOAccessRequirement other = (DBOAccessRequirement) obj;
		return Objects.equals(accessType, other.accessType) && Objects.equals(concreteType, other.concreteType)
				&& Objects.equals(createdBy, other.createdBy) && createdOn == other.createdOn
				&& Objects.equals(currentRevNumber, other.currentRevNumber) && Objects.equals(eTag, other.eTag)
				&& Objects.equals(id, other.id) && Objects.equals(isTwoFaRequired, other.isTwoFaRequired)
				&& Objects.equals(identityProviders, other.identityProviders) && Objects.equals(name, other.name);
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ACCESS_REQUIREMENT;
	}

	@Override
	public MigratableTableTranslation<DBOAccessRequirement, DBOAccessRequirement> getTranslator() { return MIGRATION_MAPPER; }

	@Override
	public Class<? extends DBOAccessRequirement> getBackupClass() {
		return DBOAccessRequirement.class;
	}

	@Override
	public Class<? extends DBOAccessRequirement> getDatabaseObjectClass() {
		return DBOAccessRequirement.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOSubjectAccessRequirement());
		list.add(new DBOAccessRequirementRevision());
		return list;
	}
}
