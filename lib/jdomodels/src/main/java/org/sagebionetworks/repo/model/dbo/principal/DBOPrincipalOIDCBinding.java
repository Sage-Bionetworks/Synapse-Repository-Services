package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_PROVIDER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_OIDC_BINDING_SUBJECT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PRINCIPAL_OIDC_BINDING;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_OIDC_BINDING;

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

public class DBOPrincipalOIDCBinding implements MigratableDatabaseObject<DBOPrincipalOIDCBinding, DBOPrincipalOIDCBinding> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_PRINCIPAL_OIDC_BINDING_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_PRINCIPAL_OIDC_BINDING_ETAG).withIsEtag(true),
		new FieldColumn("createdOn", COL_PRINCIPAL_OIDC_BINDING_CREATED_ON),
		new FieldColumn("principalId", COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID),
		new FieldColumn("aliasId", COL_PRINCIPAL_OIDC_BINDING_ALIAS_ID),
		new FieldColumn("provider", COL_PRINCIPAL_OIDC_BINDING_PROVIDER),
		new FieldColumn("subject", COL_PRINCIPAL_OIDC_BINDING_SUBJECT)
	};
	
	private static TableMapping<DBOPrincipalOIDCBinding> TABLE_MAPPING = new TableMapping<DBOPrincipalOIDCBinding>() {

		@Override
		public DBOPrincipalOIDCBinding mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOPrincipalOIDCBinding subject = new DBOPrincipalOIDCBinding();
			
			subject.setId(rs.getLong(COL_PRINCIPAL_OIDC_BINDING_ID));
			subject.setCreatedOn(rs.getTimestamp(COL_PRINCIPAL_OIDC_BINDING_CREATED_ON));
			subject.setPrincipalId(rs.getLong(COL_PRINCIPAL_OIDC_BINDING_PRINCIPAL_ID));
			subject.setAliasId(rs.getLong(COL_PRINCIPAL_OIDC_BINDING_ALIAS_ID));
			subject.setProvider(rs.getString(COL_PRINCIPAL_OIDC_BINDING_PROVIDER));
			subject.setSubject(rs.getString(COL_PRINCIPAL_OIDC_BINDING_SUBJECT));

			return subject;
		}

		@Override
		public String getTableName() {
			return TABLE_PRINCIPAL_OIDC_BINDING;
		}

		@Override
		public String getDDLFileName() {
			return DDL_PRINCIPAL_OIDC_BINDING;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOPrincipalOIDCBinding> getDBOClass() {
			return DBOPrincipalOIDCBinding.class;
		}
	};
	
	private static MigratableTableTranslation<DBOPrincipalOIDCBinding, DBOPrincipalOIDCBinding> TRANSLATOR = new BasicMigratableTableTranslation<>() {
		
		@Override
		public DBOPrincipalOIDCBinding createDatabaseObjectFromBackup(DBOPrincipalOIDCBinding backup) {
			if (backup.getEtag() == null) {
				backup.setEtag(UUID.randomUUID().toString());
			}
			return backup;
		}
	};

	private Long id;
	private String etag;
	private Timestamp createdOn;
	private Long principalId;
	private Long aliasId;
	private String provider;
	private String subject;
	
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

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}
	
	public Long getAliasId() {
		return aliasId;
	}
	
	public void setAliasId(Long aliasId) {
		this.aliasId = aliasId;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public TableMapping<DBOPrincipalOIDCBinding> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PRINCIPAL_OIDC_BINDING;
	}

	@Override
	public MigratableTableTranslation<DBOPrincipalOIDCBinding, DBOPrincipalOIDCBinding> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOPrincipalOIDCBinding> getBackupClass() {
		return DBOPrincipalOIDCBinding.class;
	}

	@Override
	public Class<? extends DBOPrincipalOIDCBinding> getDatabaseObjectClass() {
		return DBOPrincipalOIDCBinding.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aliasId, createdOn, etag, id, principalId, provider, subject);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOPrincipalOIDCBinding)) {
			return false;
		}
		DBOPrincipalOIDCBinding other = (DBOPrincipalOIDCBinding) obj;
		return Objects.equals(aliasId, other.aliasId) && Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(principalId, other.principalId)
				&& Objects.equals(provider, other.provider) && Objects.equals(subject, other.subject);
	}

	@Override
	public String toString() {
		return "DBOPrincipalOIDCBinding [id=" + id + ", etag=" + etag + ", createdOn=" + createdOn + ", principalId=" + principalId
				+ ", aliasId=" + aliasId + ", provider=" + provider + ", subject=" + subject + "]";
	}
	
}
