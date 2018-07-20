/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_CREATION_DATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_E_TAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
public class DBOUserGroup implements MigratableDatabaseObject<DBOUserGroup, DBOUserGroup> {
	private Long id;
	private Date creationDate;
	private Boolean isIndividual = false;
	private String etag;


	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_USER_GROUP_ID, true).withIsBackupId(true),
		new FieldColumn("creationDate", COL_USER_GROUP_CREATION_DATE),
		new FieldColumn("isIndividual", COL_USER_GROUP_IS_INDIVIDUAL), 
		new FieldColumn("etag", COL_USER_GROUP_E_TAG).withIsEtag(true)
		};


	@Override
	public TableMapping<DBOUserGroup> getTableMapping() {
		return new TableMapping<DBOUserGroup>() {
			// Map a result set to this object
			@Override
			public DBOUserGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOUserGroup ug = new DBOUserGroup();
				ug.setId(rs.getLong(COL_USER_GROUP_ID));
				Timestamp ts = rs.getTimestamp(COL_USER_GROUP_CREATION_DATE);
				ug.setCreationDate(ts==null ? null : new Date(ts.getTime()));
				ug.setIsIndividual(rs.getBoolean(COL_USER_GROUP_IS_INDIVIDUAL));
				ug.setEtag(rs.getString(COL_USER_GROUP_E_TAG));
				return ug;
			}

			@Override
			public String getTableName() {
				return TABLE_USER_GROUP;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_USER_GROUP;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOUserGroup> getDBOClass() {
				return DBOUserGroup.class;
			}
		};
	}

	/**
	 * @return the id
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

	/**
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}


	/**
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}


	/**
	 * @return the isIndividual
	 */
	public Boolean getIsIndividual() {
		return isIndividual;
	}


	/**
	 * @param isIndividual the isIndividual to set
	 */
	public void setIsIndividual(Boolean isIndividual) {
		this.isIndividual = isIndividual;
	}
	
	/**
	 * @return the etag
	 */
	public String getEtag() {
		return etag;
	}
	
	/**
	 * @param etag the etag to set
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PRINCIPAL;
	}



	@Override
	public Class<? extends DBOUserGroup> getBackupClass() {
		return DBOUserGroup.class;
	}


	@Override
	public Class<? extends DBOUserGroup> getDatabaseObjectClass() {
		return DBOUserGroup.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBOGroupMembers());
		list.add(new DBOCredential());
		list.add(new DBOTermsOfUseAgreement());
		list.add(new DBOSessionToken());
		return list;
	}

	@Override
	public MigratableTableTranslation<DBOUserGroup, DBOUserGroup> getTranslator() {
		return new BasicMigratableTableTranslation<DBOUserGroup>();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((isIndividual == null) ? 0 : isIndividual.hashCode());
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
		DBOUserGroup other = (DBOUserGroup) obj;
		if (creationDate == null) {
			if (other.creationDate != null)
				return false;
		} else if (!creationDate.equals(other.creationDate))
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
		if (isIndividual == null) {
			if (other.isIndividual != null)
				return false;
		} else if (!isIndividual.equals(other.isIndividual))
			return false;
		return true;
	}

}
