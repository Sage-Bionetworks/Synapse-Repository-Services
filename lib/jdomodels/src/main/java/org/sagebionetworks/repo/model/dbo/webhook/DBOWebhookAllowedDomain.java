package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ALLOWED_DOMAIN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ALLOWED_DOMAIN_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ALLOWED_DOMAIN_PATTERN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_WEBHOOK_ALLOWED_DOMAIN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK_ALLOWED_DOMAIN;

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

public class DBOWebhookAllowedDomain implements MigratableDatabaseObject<DBOWebhookAllowedDomain, DBOWebhookAllowedDomain> {
	
	private Long id;
	private String etag;
	private String pattern;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_WEBHOOK_ALLOWED_DOMAIN_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_WEBHOOK_ALLOWED_DOMAIN_ETAG).withIsEtag(true),
		new FieldColumn("pattern", COL_WEBHOOK_ALLOWED_DOMAIN_PATTERN)
	};
	
	private static final TableMapping<DBOWebhookAllowedDomain> TABLE_MAPPING = new TableMapping<>() {
		@Override
		public DBOWebhookAllowedDomain mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new DBOWebhookAllowedDomain()
				.setId(rs.getLong(COL_WEBHOOK_ALLOWED_DOMAIN_ID))
				.setEtag(rs.getString(COL_WEBHOOK_ALLOWED_DOMAIN_ETAG))
				.setPattern(rs.getString(COL_WEBHOOK_ALLOWED_DOMAIN_PATTERN));
		}

		@Override
		public String getTableName() {
			return TABLE_WEBHOOK_ALLOWED_DOMAIN;
		}

		@Override
		public String getDDLFileName() {
			return DDL_FILE_WEBHOOK_ALLOWED_DOMAIN;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOWebhookAllowedDomain> getDBOClass() {
			return DBOWebhookAllowedDomain.class;
		}
	};

	private static final BasicMigratableTableTranslation<DBOWebhookAllowedDomain> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<DBOWebhookAllowedDomain>();

	@Override
	public TableMapping<DBOWebhookAllowedDomain> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.WEBHOOK_ALLOWED_DOMAIN;
	}

	@Override
	public MigratableTableTranslation<DBOWebhookAllowedDomain, DBOWebhookAllowedDomain> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOWebhookAllowedDomain> getBackupClass() {
		return DBOWebhookAllowedDomain.class;
	}

	@Override
	public Class<? extends DBOWebhookAllowedDomain> getDatabaseObjectClass() {
		return DBOWebhookAllowedDomain.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public DBOWebhookAllowedDomain setId(Long id) {
		this.id = id;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOWebhookAllowedDomain setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public String getPattern() {
		return pattern;
	}

	public DBOWebhookAllowedDomain setPattern(String pattern) {
		this.pattern = pattern;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, id, pattern);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOWebhookAllowedDomain)) {
			return false;
		}
		DBOWebhookAllowedDomain other = (DBOWebhookAllowedDomain) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(id, other.id) && Objects.equals(pattern, other.pattern);
	}

	@Override
	public String toString() {
		return "DBOWebhookAllowedDomain [id=" + id + ", etag=" + etag + ", pattern=" + pattern + "]";
	}

}
