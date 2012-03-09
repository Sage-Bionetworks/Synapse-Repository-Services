/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_CREATION_DATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_E_TAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * @author brucehoff
 *
 */
public class DBOUserGroup implements DatabaseObject<DBOUserGroup> {
	private Long id;
	private String name;
	private Long eTag = new Long(0);
	private Date creationDate;
	private Boolean isIndividual = false;


	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_USER_GROUP_ID, true),
		new FieldColumn("name", COL_USER_GROUP_NAME),
		new FieldColumn("eTag", COL_USER_GROUP_E_TAG),
		new FieldColumn("creationDate", COL_USER_GROUP_CREATION_DATE),
		new FieldColumn("isIndividual", COL_USER_GROUP_IS_INDIVIDUAL)
		};


	@Override
	public TableMapping<DBOUserGroup> getTableMapping() {
		return new TableMapping<DBOUserGroup>() {
			// Map a result set to this object
			@Override
			public DBOUserGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOUserGroup ug = new DBOUserGroup();
				ug.setId(rs.getLong(COL_USER_GROUP_ID));
				ug.setName(rs.getString(COL_USER_GROUP_NAME));
				ug.seteTag(rs.getLong(COL_USER_GROUP_E_TAG));
				ug.setCreationDate(rs.getTimestamp(COL_USER_GROUP_CREATION_DATE));
				ug.setIsIndividual(rs.getBoolean(COL_USER_GROUP_IS_INDIVIDUAL));
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
	 * @return the name
	 */
	public String getName() {
		return name;
	}


	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * @return the eTag
	 */
	public Long geteTag() {
		return eTag;
	}


	/**
	 * @param eTag the eTag to set
	 */
	public void seteTag(Long eTag) {
		this.eTag = eTag;
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



}
