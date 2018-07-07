package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * The database object for a node.
 * 
 * @author jmhill
 *
 */
public class DBONode implements MigratableDatabaseObject<DBONode, DBONode>, ObservableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_NODE_ID, true).withIsBackupId(true),
			new FieldColumn("parentId", COL_NODE_PARENT_ID).withIsSelfForeignKey(true),
			new FieldColumn("name", COL_NODE_NAME),
			new FieldColumn("currentRevNumber", COL_CURRENT_REV),
			new FieldColumn("eTag", COL_NODE_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_NODE_CREATED_BY),
			new FieldColumn("createdOn", COL_NODE_CREATED_ON),
			new FieldColumn("type", COL_NODE_TYPE),
			new FieldColumn("alias", COL_NODE_ALIAS),
			};

	@Override
	public TableMapping<DBONode> getTableMapping() {
		return new TableMapping<DBONode>() {
			// Map a result set to this object
			@Override
			public DBONode mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBONodeMapper mapper = new DBONodeMapper();
				return mapper.mapRow(rs, rowNum);
			}

			@Override
			public String getTableName() {
				return TABLE_NODE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_NODE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBONode> getDBOClass() {
				return DBONode.class;
			}
		};
	}
	
	private Long id;
	private Long parentId;
	private String name;
	private Long currentRevNumber;
	private byte[] description;
	private String eTag;
	private Long createdBy;
	private Long createdOn;
	private String type;
	private String alias;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getParentId() {
		return parentId;
	}
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getCurrentRevNumber() {
		return currentRevNumber;
	}
	public void setCurrentRevNumber(Long currentRevNumber) {
		this.currentRevNumber = currentRevNumber;
	}
	public byte[] getDescription() {
		return description;
	}
	public void setDescription(byte[] description) {
		this.description = description;
	}
	public Long getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}
	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE;
	}
	@Override
	public MigratableTableTranslation<DBONode, DBONode> getTranslator() {
		return new BasicMigratableTableTranslation<DBONode>();
	}
	@Override
	public Class<? extends DBONode> getBackupClass() {
		return DBONode.class;
	}
	@Override
	public Class<? extends DBONode> getDatabaseObjectClass() {
		return DBONode.class;
	}
	
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new DBORevision());
		return list;
	}
	
	@Override
	public ObjectType getObjectType() {
		return ObjectType.ENTITY;
	}
	@Override
	public String getIdString() {
		return KeyFactory.keyToString(id);
	}
	@Override
	public String getParentIdString() {
		return KeyFactory.keyToString(parentId);
	}
	@Override
	public String getEtag() {
		return eTag;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime
				* result
				+ ((currentRevNumber == null) ? 0 : currentRevNumber.hashCode());
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		DBONode other = (DBONode) obj;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
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
		if (currentRevNumber == null) {
			if (other.currentRevNumber != null)
				return false;
		} else if (!currentRevNumber.equals(other.currentRevNumber))
			return false;
		if (!Arrays.equals(description, other.description))
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DBONode [id=" + id + ", parentId=" + parentId + ", name="
				+ name + ", currentRevNumber=" + currentRevNumber
				+ ", description=" + Arrays.toString(description) + ", eTag="
				+ eTag + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", type=" + type + ", alias=" + alias + "]";
	}

}
