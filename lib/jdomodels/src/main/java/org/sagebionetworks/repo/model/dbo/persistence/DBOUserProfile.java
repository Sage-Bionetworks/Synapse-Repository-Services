/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_USER_PROFILE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * @author brucehoff
 *
 */
public class DBOUserProfile implements MigratableDatabaseObject<DBOUserProfile, DBOUserProfile> {
	private Long ownerId;
	private byte[] properties;
	private String eTag;
	
	public static final String OWNER_ID_FIELD_NAME = "ownerId";

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn(OWNER_ID_FIELD_NAME, COL_USER_PROFILE_ID, true).withIsBackupId(true),
		new FieldColumn("properties", COL_USER_PROFILE_PROPS_BLOB),
		new FieldColumn("eTag", COL_USER_PROFILE_ETAG).withIsEtag(true)
		};


	@Override
	public TableMapping<DBOUserProfile> getTableMapping() {
		return new TableMapping<DBOUserProfile>() {
			// Map a result set to this object
			@Override
			public DBOUserProfile mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBOUserProfile up = new DBOUserProfile();
				up.setOwnerId(rs.getLong(COL_USER_PROFILE_ID));
				java.sql.Blob blob = rs.getBlob(COL_USER_PROFILE_PROPS_BLOB);
				if(blob != null){
					up.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				up.seteTag(rs.getString(COL_USER_PROFILE_ETAG));
				return up;
			}

			@Override
			public String getTableName() {
				return TABLE_USER_PROFILE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_USER_PROFILE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOUserProfile> getDBOClass() {
				return DBOUserProfile.class;
			}
		};
	}


	/**
	 * @return the properties
	 */
	public byte[] getProperties() {
		return properties;
	}


	/**
	 * @param properties the properties to set
	 */
	public void setProperties(byte[] properties) {
		this.properties = properties;
	}

	/**
	 * @return the eTag
	 */
	public String geteTag() {
		return eTag;
	}


	/**
	 * @param eTag the eTag to set
	 */
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}


	/**
	 * @return the ownerId
	 */
	public Long getOwnerId() {
		return ownerId;
	}


	/**
	 * @param ownerId the ownerId to set
	 */
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + Arrays.hashCode(properties);
		return result;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOUserProfile other = (DBOUserProfile) obj;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (!Arrays.equals(properties, other.properties))
			return false;
		return true;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.USER_PROFILE;
	}


	@Override
	public MigratableTableTranslation<DBOUserProfile, DBOUserProfile> getTranslator() {
		return new MigratableTableTranslation<DBOUserProfile, DBOUserProfile>(){

			@Override
			public DBOUserProfile createDatabaseObjectFromBackup(
					DBOUserProfile backup) {
				return backup;
			}

			@Override
			public DBOUserProfile createBackupFromDatabaseObject(
					DBOUserProfile dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBOUserProfile> getBackupClass() {
		return DBOUserProfile.class;
	}


	@Override
	public Class<? extends DBOUserProfile> getDatabaseObjectClass() {
		return DBOUserProfile.class;
	}


	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}



}
