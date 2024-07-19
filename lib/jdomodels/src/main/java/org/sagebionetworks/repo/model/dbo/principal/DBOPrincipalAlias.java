package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_UNIQUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;

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
import org.sagebionetworks.repo.model.principal.AliasEnum;

/**
 * This table tracks alias that uniquely identify principals.
 * @author John
 *
 */
public class DBOPrincipalAlias implements MigratableDatabaseObject<DBOPrincipalAlias, DBOPrincipalAlias>  {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_PRINCIPAL_ALIAS_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", COL_PRINCIPAL_ALIAS_ETAG).withIsEtag(true),
			new FieldColumn("principalId", COL_PRINCIPAL_ALIAS_PRINCIPAL_ID),
			new FieldColumn("aliasUnique", COL_PRINCIPAL_ALIAS_UNIQUE),
			new FieldColumn("aliasDisplay", COL_PRINCIPAL_ALIAS_DISPLAY),
			new FieldColumn("aliasType", COL_PRINCIPAL_ALIAS_TYPE),
	};
	private static final MigratableTableTranslation<DBOPrincipalAlias, DBOPrincipalAlias> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	private Long id;
	private String etag;
	private Long principalId;
	private String aliasUnique;
	private String aliasDisplay;
	private String aliasType;
	/**
	 * Has this alias been validated?
	 */
	// NOTE THIS IS NO LONGER A DATABASE COLUMN.  We leave it in so migration won't break.
	private Boolean validated;

	@Override
	public TableMapping<DBOPrincipalAlias> getTableMapping() {
		return new TableMapping<DBOPrincipalAlias>() {
			
			@Override
			public DBOPrincipalAlias mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOPrincipalAlias dbo = new DBOPrincipalAlias();
				dbo.setId(rs.getLong(COL_PRINCIPAL_ALIAS_ID));
				dbo.setEtag(rs.getNString(COL_PRINCIPAL_ALIAS_ETAG));
				dbo.setPrincipalId(rs.getLong(COL_PRINCIPAL_ALIAS_PRINCIPAL_ID));
				dbo.setAliasUnique(rs.getString(COL_PRINCIPAL_ALIAS_UNIQUE));
				dbo.setAliasDisplay(rs.getString(COL_PRINCIPAL_ALIAS_DISPLAY));
				dbo.setAliasType(rs.getString(COL_PRINCIPAL_ALIAS_TYPE));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_PRINCIPAL_ALIAS;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_PRINCIPAL_ALIAS;
			}
			
			@Override
			public Class<? extends DBOPrincipalAlias> getDBOClass() {
				return DBOPrincipalAlias.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PRINCIPAL_ALIAS;
	}
	
	@Override
	public MigratableTableTranslation<DBOPrincipalAlias, DBOPrincipalAlias> getTranslator() {
		return MIGRATION_TRANSLATOR; 
	}

	@Override
	public Class<? extends DBOPrincipalAlias> getBackupClass() {
		return DBOPrincipalAlias.class;
	}

	@Override
	public Class<? extends DBOPrincipalAlias> getDatabaseObjectClass() {
		return DBOPrincipalAlias.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public String getAliasUnique() {
		return aliasUnique;
	}

	public void setAliasUnique(String aliasUnique) {
		this.aliasUnique = aliasUnique;
	}

	public String getAliasDisplay() {
		return aliasDisplay;
	}

	public void setAliasDisplay(String aliasDisplay) {
		this.aliasDisplay = aliasDisplay;
	}

	public String getAliasType() {
		return aliasType;
	}

	public AliasEnum getAliasTypeAsEnum() {
		return AliasEnum.valueOf(aliasType);
	}
	
	public void setAliasType(String aliasType) {
		this.aliasType = aliasType;
	}

	public Boolean getValidated() {
		return validated;
	}

	public void setValidated(Boolean validated) {
		this.validated = validated;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aliasDisplay, aliasType, aliasUnique, etag, id, principalId, validated);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOPrincipalAlias other = (DBOPrincipalAlias) obj;
		return Objects.equals(aliasDisplay, other.aliasDisplay) && Objects.equals(aliasType, other.aliasType)
				&& Objects.equals(aliasUnique, other.aliasUnique) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(principalId, other.principalId)
				&& Objects.equals(validated, other.validated);
	}

	@Override
	public String toString() {
		return "DBOPrincipalAlias [id=" + id + ", etag=" + etag + ", principalId=" + principalId + ", aliasUnique="
				+ aliasUnique + ", aliasDisplay=" + aliasDisplay + ", aliasType=" + aliasType + ", validated="
				+ validated + "]";
	}
	
}
