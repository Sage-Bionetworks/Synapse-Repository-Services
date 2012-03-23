package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

/**
 * An alias for a node type.
 * @author John
 *
 */
public class DBONodeTypeAlias implements DatabaseObject<DBONodeTypeAlias>{
	
	private static FieldColumn[] FIELDS = new FieldColumn[]{
		new FieldColumn("typeOwner", COL_OWNER_TYPE, true),
		new FieldColumn("alias", COL_NODE_TYPE_ALIAS, true),
};

	@Override
	public TableMapping<DBONodeTypeAlias> getTableMapping() {
		
		return new TableMapping<DBONodeTypeAlias>(){

			@Override
			public DBONodeTypeAlias mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				DBONodeTypeAlias result = new DBONodeTypeAlias();
				result.setTypeOwner(rs.getShort(COL_OWNER_TYPE));
				result.setAlias(rs.getString(COL_NODE_TYPE_ALIAS));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_NODE_TYPE_ALIAS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_NODE_TYPE_ALIAS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBONodeTypeAlias> getDBOClass() {
				return DBONodeTypeAlias.class;
			}};
	}
	
	private Short typeOwner;
	private String alias;

	public Short getTypeOwner() {
		return typeOwner;
	}
	public void setTypeOwner(Short typeOwner) {
		this.typeOwner = typeOwner;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result
				+ ((typeOwner == null) ? 0 : typeOwner.hashCode());
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
		DBONodeTypeAlias other = (DBONodeTypeAlias) obj;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		if (typeOwner == null) {
			if (other.typeOwner != null)
				return false;
		} else if (!typeOwner.equals(other.typeOwner))
			return false;
		return true;
	}

}
