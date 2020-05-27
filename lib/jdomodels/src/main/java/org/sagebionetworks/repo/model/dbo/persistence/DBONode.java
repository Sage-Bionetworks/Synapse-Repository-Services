package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_MAX_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.springframework.jdbc.core.RowMapper;

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
			new FieldColumn("currentRevNumber", COL_NODE_CURRENT_REV),
			new FieldColumn("maxRevNumber", COL_NODE_MAX_REV),
			new FieldColumn("eTag", COL_NODE_ETAG).withIsEtag(true),
			new FieldColumn("createdBy", COL_NODE_CREATED_BY),
			new FieldColumn("createdOn", COL_NODE_CREATED_ON),
			new FieldColumn("type", COL_NODE_TYPE),
			new FieldColumn("alias", COL_NODE_ALIAS),
			};
	
	private static final RowMapper<DBONode> ROW_MAPPER = new DBONodeMapper();
	
	private static final TableMapping<DBONode> TABLE_MAPPING = new TableMapping<DBONode>() {
		// Map a result set to this object
		@Override
		public DBONode mapRow(ResultSet rs, int rowNum)	throws SQLException {
			return ROW_MAPPER.mapRow(rs, rowNum);
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
	
	private static final MigratableTableTranslation<DBONode, DBONode> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<DBONode>();
	
	private static final List<MigratableDatabaseObject<?,?>> SECONDARY_TYPES = Collections.singletonList(new DBORevision());
	
	private Long id;
	private Long parentId;
	private String name;
	private Long currentRevNumber;
	private Long maxRevNumber;
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
	public Long getMaxRevNumber() {
		return maxRevNumber;
	}
	public void setMaxRevNumber(Long maxRevNumber) {
		this.maxRevNumber = maxRevNumber;
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
	public TableMapping<DBONode> getTableMapping() {
		return TABLE_MAPPING;
	}
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.NODE;
	}
	@Override
	public MigratableTableTranslation<DBONode, DBONode> getTranslator() {
		return MIGRATION_TRANSLATOR;
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
		return SECONDARY_TYPES;
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
	public String getEtag() {
		return eTag;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + Objects.hash(alias, createdBy, createdOn, currentRevNumber, eTag, id, maxRevNumber, name, parentId, type);
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
		return Objects.equals(alias, other.alias) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(currentRevNumber, other.currentRevNumber)
				&& Arrays.equals(description, other.description) && Objects.equals(eTag, other.eTag) && Objects.equals(id, other.id)
				&& Objects.equals(maxRevNumber, other.maxRevNumber) && Objects.equals(name, other.name)
				&& Objects.equals(parentId, other.parentId) && Objects.equals(type, other.type);
	}
	
	@Override
	public String toString() {
		return "DBONode [id=" + id + ", parentId=" + parentId + ", name="
				+ name + ", currentRevNumber=" + currentRevNumber + ", maxRevNumber=" + maxRevNumber
				+ ", description=" + Arrays.toString(description) + ", eTag="
				+ eTag + ", createdBy=" + createdBy + ", createdOn="
				+ createdOn + ", type=" + type + ", alias=" + alias + "]";
	}

}
