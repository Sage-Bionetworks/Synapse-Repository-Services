/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_IS_VERIFIED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECRET_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_CLIENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigrateFromXStreamToJSON;
import org.sagebionetworks.repo.model.dbo.migration.XStreamToJsonTranslator;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.util.TemporaryCode;

/**
 *
 */
public class DBOOAuthClient implements MigratableDatabaseObject<DBOOAuthClient, DBOOAuthClient> {
	private Long id;
	private String name;
	private Long createdOn;
	private Long modifiedOn;
	private Long createdBy;
	private String eTag;
	private String secretHash;
	private String sectorIdentifierUri;
	@TemporaryCode(author = "John", comment = "replaced properties with json")
	private byte[] properties;
	private String json;
	private boolean verified;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_OAUTH_CLIENT_ID, true).withIsBackupId(true),
			new FieldColumn("name", COL_OAUTH_CLIENT_NAME), new FieldColumn("createdBy", COL_OAUTH_CLIENT_CREATED_BY),
			new FieldColumn("createdOn", COL_OAUTH_CLIENT_CREATED_ON),
			new FieldColumn("modifiedOn", COL_OAUTH_CLIENT_MODIFIED_ON),
			new FieldColumn("sectorIdentifierUri", COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI),
			new FieldColumn("json", COL_OAUTH_CLIENT_JSON),
			new FieldColumn("eTag", COL_OAUTH_CLIENT_ETAG).withIsEtag(true),
			new FieldColumn("secretHash", COL_OAUTH_CLIENT_SECRET_HASH),
			new FieldColumn("verified", COL_OAUTH_CLIENT_IS_VERIFIED), };

	@Override
	public TableMapping<DBOOAuthClient> getTableMapping() {
		return new TableMapping<DBOOAuthClient>() {
			// Map a result set to this object
			@Override
			public DBOOAuthClient mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOOAuthClient client = new DBOOAuthClient();
				client.setId(rs.getLong(COL_OAUTH_CLIENT_ID));
				client.setCreatedOn(rs.getLong(COL_OAUTH_CLIENT_CREATED_ON));
				client.setName(rs.getString(COL_OAUTH_CLIENT_NAME));
				client.setCreatedBy(rs.getLong(COL_OAUTH_CLIENT_CREATED_BY));
				client.setModifiedOn(rs.getLong(COL_OAUTH_CLIENT_MODIFIED_ON));
				client.setSectorIdentifierUri(rs.getString(COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI));
				client.setJson(rs.getString(COL_OAUTH_CLIENT_JSON));
				client.seteTag(rs.getString(COL_OAUTH_CLIENT_ETAG));
				client.setSecretHash(rs.getString(COL_OAUTH_CLIENT_SECRET_HASH));
				client.setVerified(rs.getBoolean(COL_OAUTH_CLIENT_IS_VERIFIED));
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public String geteTag() {
		return eTag;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public String getSecretHash() {
		return secretHash;
	}

	public void setSecretHash(String secretHash) {
		this.secretHash = secretHash;
	}

	public String getSectorIdentifierUri() {
		return sectorIdentifierUri;
	}

	public void setSectorIdentifierUri(String sectorIdentifierUri) {
		this.sectorIdentifierUri = sectorIdentifierUri;
	}

	public byte[] getProperties() {
		return properties;
	}

	public void setProperties(byte[] properties) {
		this.properties = properties;
	}

	public boolean getVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	@Override
	public String toString() {
		return "DBOOAuthClient [id=" + id + ", name=" + name + ", createdOn=" + createdOn + ", modifiedOn=" + modifiedOn
				+ ", createdBy=" + createdBy + ", eTag=" + eTag + ", secretHash=" + secretHash
				+ ", sectorIdentifierUri=" + sectorIdentifierUri + ", properties=" + Arrays.toString(properties)
				+ ", isVerified=" + verified + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(properties);
		result = prime * result + Objects.hash(createdBy, createdOn, eTag, id, json, modifiedOn, name, secretHash,
				sectorIdentifierUri, verified);
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
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(eTag, other.eTag) && Objects.equals(id, other.id) && Objects.equals(json, other.json)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(name, other.name)
				&& Arrays.equals(properties, other.properties) && Objects.equals(secretHash, other.secretHash)
				&& Objects.equals(sectorIdentifierUri, other.sectorIdentifierUri) && verified == other.verified;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_CLIENT;
	}

	@Override
	public MigratableTableTranslation<DBOOAuthClient, DBOOAuthClient> getTranslator() {
		return new MigrateFromXStreamToJSON<>(XStreamToJsonTranslator.builder().setDtoType(OAuthClient.class)
				.setDboType(DBOOAuthClient.class).setFromName("properties").setToName("json").build());
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
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

}
