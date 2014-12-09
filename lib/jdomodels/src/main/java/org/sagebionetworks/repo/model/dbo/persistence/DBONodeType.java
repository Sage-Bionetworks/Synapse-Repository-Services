package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE_TYPE;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * A database object for NodeType.
 * @author jmhill
 *
 */
public class DBONodeType implements DatabaseObject<DBONodeType> {
	

	private static FieldColumn[] FIELDS = new FieldColumn[]{
			new FieldColumn("id", COL_NODE_TYPE_ID, true),
			new FieldColumn("name", COL_NODE_TYPE_NAME, true),
	};
	
	@Override
	public TableMapping<DBONodeType> getTableMapping() {
		return new TableMapping<DBONodeType>(){
			// Map a result set to this object
			@Override
			public DBONodeType mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBONodeType result = new DBONodeType();
				result.setId(rs.getShort(COL_NODE_TYPE_ID));
				result.setName(rs.getString(COL_NODE_TYPE_NAME));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_NODE_TYPE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_NODE_TYPE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBONodeType> getDBOClass() {
				return DBONodeType.class;
			}} ;
	}
	
	private Short id;
	private String name;

	public Short getId() {
		return id;
	}

	public void setId(Short id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		DBONodeType other = (DBONodeType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
