/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_CLIENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 *
 */
public class DBOOAuthClient implements MigratableDatabaseObject<DBOOAuthClient, DBOOAuthClient> {
	private Long id;
	private Long createdOn;
	private Long modifiedOn;
	private Long createdBy;
	private String eTag;
	private String secret;
	private String sectorIdentifierUri;
	private byte[] properties;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_OAUTH_CLIENT_ID, true).withIsBackupId(true),
		new FieldColumn("createdBy", COL_OAUTH_CLIENT_CREATED_BY),
		new FieldColumn("createdOn", COL_OAUTH_CLIENT_CREATED_ON),
		new FieldColumn("modifiedOn", COL_OAUTH_CLIENT_MODIFIED_ON),
		new FieldColumn("sectorIdentifierUri", COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI),
		new FieldColumn("properties", COL_OAUTH_CLIENT_PROPERTIES),
		new FieldColumn("eTag", COL_OAUTH_CLIENT_ETAG).withIsEtag(true),
		new FieldColumn("secret", COL_OAUTH_CLIENT_SECRET),
		};

	@Override
	public TableMapping<DBOOAuthClient> getTableMapping() {
		return new TableMapping<DBOOAuthClient>() {
			// Map a result set to this object
			@Override
			public DBOOAuthClient mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBOOAuthClient client = new DBOOAuthClient();
				client.setId(rs.getLong(COL_OAUTH_CLIENT_ID));
				client.setCreatedOn(rs.getLong(COL_OAUTH_CLIENT_CREATED_ON));
				client.setCreatedBy(rs.getLong(COL_OAUTH_CLIENT_CREATED_BY));
				client.setModifiedOn(rs.getLong(COL_OAUTH_CLIENT_MODIFIED_ON));
				client.setSectorIdentifierUri(rs.getString(COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI));
				java.sql.Blob blob = rs.getBlob(COL_OAUTH_CLIENT_PROPERTIES);
				if(blob != null){
					client.setProperties(blob.getBytes(1, (int) blob.length()));
				}
				client.seteTag(rs.getString(COL_OAUTH_CLIENT_ETAG));
				client.setSecret(rs.getString(COL_OAUTH_CLIENT_SECRET));
				return client;
			}

			@Override
			public String getTableName() {
				return TABLE_OAUTH_CLIENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_OAUTH_CLIENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOOAuthClient> getDBOClass() {
				return DBOOAuthClient.class;
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
	 * @return the  id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}



	public String getSectorIdentifierUri() {
		return sectorIdentifierUri;
	}

	public void setSectorIdentifierUri(String sectorIdentifierUri) {
		this.sectorIdentifierUri = sectorIdentifierUri;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	@Override
	public String toString() {
		return "DBOOAuthClient [id=" + id + ", createdOn=" + createdOn + ", modifiedOn=" + modifiedOn + ", createdBy="
				+ createdBy + ", eTag=" + eTag + ", secret=" + secret + ", sectorIdentifierUri=" + sectorIdentifierUri
				+ ", properties=" + Arrays.toString(properties) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + Arrays.hashCode(properties);
		result = prime * result + ((secret == null) ? 0 : secret.hashCode());
		result = prime * result + ((sectorIdentifierUri == null) ? 0 : sectorIdentifierUri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOOAuthClient other = (DBOOAuthClient) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (!Arrays.equals(properties, other.properties))
			return false;
		if (secret == null) {
			if (other.secret != null)
				return false;
		} else if (!secret.equals(other.secret))
			return false;
		if (sectorIdentifierUri == null) {
			if (other.sectorIdentifierUri != null)
				return false;
		} else if (!sectorIdentifierUri.equals(other.sectorIdentifierUri))
			return false;
		return true;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_CLIENT;
	}
	
	@Override
	public MigratableTableTranslation<DBOOAuthClient, DBOOAuthClient> getTranslator() {
			return new BasicMigratableTableTranslation<DBOOAuthClient>();
	}


	@Override
	public Class<? extends DBOOAuthClient> getBackupClass() {
		return DBOOAuthClient.class;
	}


	@Override
	public Class<? extends DBOOAuthClient> getDatabaseObjectClass() {
		return DBOOAuthClient.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



}
