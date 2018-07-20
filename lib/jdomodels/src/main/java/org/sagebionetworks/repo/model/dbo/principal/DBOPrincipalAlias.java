package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_UNIQUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.CONSTRAINT_PRINCIPAL_ALIAS_UNIQUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
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
@Table(name = TABLE_PRINCIPAL_ALIAS, constraints={CONSTRAINT_PRINCIPAL_ALIAS_UNIQUE})
public class DBOPrincipalAlias implements MigratableDatabaseObject<DBOPrincipalAlias, DBOPrincipalAlias>  {
	
	private static TableMapping<DBOPrincipalAlias> tableMapping = AutoTableMapping.create(DBOPrincipalAlias.class);
	
	
	/**
	 * The primary key for this table.
	 */
	@Field(name = COL_PRINCIPAL_ALIAS_ID, nullable = false, primary=true, backupId=true)
	private Long id;
	
	@Field(name = COL_PRINCIPAL_ALIAS_ETAG, varchar = 500, etag = true, nullable = false)
	private String etag;
	
	/**
	 * The principal ID.
	 */
	@Field(name = COL_PRINCIPAL_ALIAS_PRINCIPAL_ID, nullable = false)
	@ForeignKey(table=TABLE_USER_GROUP, field=COL_USER_GROUP_ID ,cascadeDelete=true)
	private Long principalId;
	
	/**
	 * The unique version of the alias
	 */
	@Field(name = COL_PRINCIPAL_ALIAS_UNIQUE, varchar = 500, nullable = false)
	private String aliasUnique;
	
	/**
	 * The display version of the alias.
	 */
	@Field(name = COL_PRINCIPAL_ALIAS_DISPLAY, varchar = 500, nullable = false)
	private String aliasDisplay;
	
	/**
	 * The type of the alias.
	 */
	@Field(name = COL_PRINCIPAL_ALIAS_TYPE, nullable = false)
	private AliasEnum aliasType;
	
	/**
	 * Has this alias been validated?
	 */
	// NOTE THIS IS NO LONGER A DATABASE COLUMN.  We leave it in so migration won't break.
	private Boolean validated;

	@Override
	public TableMapping<DBOPrincipalAlias> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PRINCIPAL_ALIAS;
	}

	@Override
	public MigratableTableTranslation<DBOPrincipalAlias, DBOPrincipalAlias> getTranslator() {
		return new BasicMigratableTableTranslation<DBOPrincipalAlias>();
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

	public AliasEnum getAliasType() {
		return aliasType;
	}

	public String getAliasTypeAsString() {
		return aliasType.name();
	}
	
	public void setAliasType(AliasEnum aliasType) {
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
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((aliasDisplay == null) ? 0 : aliasDisplay.hashCode());
		result = prime * result
				+ ((aliasType == null) ? 0 : aliasType.hashCode());
		result = prime * result
				+ ((aliasUnique == null) ? 0 : aliasUnique.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result
				+ ((validated == null) ? 0 : validated.hashCode());
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
		DBOPrincipalAlias other = (DBOPrincipalAlias) obj;
		if (aliasDisplay == null) {
			if (other.aliasDisplay != null)
				return false;
		} else if (!aliasDisplay.equals(other.aliasDisplay))
			return false;
		if (aliasType != other.aliasType)
			return false;
		if (aliasUnique == null) {
			if (other.aliasUnique != null)
				return false;
		} else if (!aliasUnique.equals(other.aliasUnique))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (validated == null) {
			if (other.validated != null)
				return false;
		} else if (!validated.equals(other.validated))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOPrincipalAlias [id=" + id + ", etag=" + etag
				+ ", principalId=" + principalId + ", aliasUnique="
				+ aliasUnique + ", aliasDisplay=" + aliasDisplay
				+ ", aliasType=" + aliasType + ", validated=" + validated + "]";
	}
	
}
