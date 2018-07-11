package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_DELETED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_NODE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TRASH_CAN_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_TRASH_CAN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TRASH_CAN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * A trashed entity in the trash can. It keeps track of who deleted the item and when.
 */
public class DBOTrashedEntity implements MigratableDatabaseObject<DBOTrashedEntity, DBOTrashedEntity> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("nodeId", COL_TRASH_CAN_NODE_ID, true).withIsBackupId(true),
		new FieldColumn("nodeName", COL_TRASH_CAN_NODE_NAME),
		new FieldColumn("deletedBy", COL_TRASH_CAN_DELETED_BY),
		new FieldColumn("deletedOn", COL_TRASH_CAN_DELETED_ON),
		new FieldColumn("parentId", COL_TRASH_CAN_PARENT_ID)
	};

	@Override
	public TableMapping<DBOTrashedEntity> getTableMapping() {

		return new TableMapping<DBOTrashedEntity>() {

			@Override
			public DBOTrashedEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTrashedEntity trash = new DBOTrashedEntity();
				trash.setNodeId(rs.getLong(COL_TRASH_CAN_NODE_ID));
				trash.setNodeName(rs.getString(COL_TRASH_CAN_NODE_NAME));
				trash.setDeletedBy(rs.getLong(COL_TRASH_CAN_DELETED_BY));
				trash.setDeletedOn(rs.getTimestamp(COL_TRASH_CAN_DELETED_ON));
				trash.setParentId(rs.getLong(COL_TRASH_CAN_PARENT_ID));
				return trash;
			}

			@Override
			public String getTableName() {
				return TABLE_TRASH_CAN;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_TRASH_CAN;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTrashedEntity> getDBOClass() {
				return DBOTrashedEntity.class;
			}};
	}

	/**
	 * The primary key.
	 */
	public Long getId() {
		return nodeId;
	}

	/**
	 * The primary key.
	 */
	public void setId(Long id) {
		this.nodeId = id;
	}

	/**
	 * The ID of the node that has been deleted into the trash can.
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * The ID of the node that has been deleted into the trash can.
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * The name of the node.
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * The name of the node.
	 */
	public void setNodeName(String name) {
		this.nodeName = name;
	}

	/**
	 * The ID of the user who deleted the entity.
	 */
	public Long getDeletedBy() {
		return deletedBy;
	}

	/**
	 * The ID of the user who deleted the entity.
	 */
	public void setDeletedBy(Long deletedBy) {
		this.deletedBy = deletedBy;
	}

	/**
	 * The date and time when the deletion occurred.
	 */
	public Timestamp getDeletedOn() {
		return deletedOn;
	}

	/**
	 * The date and time when the deletion occurred.
	 */
	public void setDeletedOn(Timestamp deletedOn) {
		this.deletedOn = deletedOn;
	}

	/**
	 * The ID of the original parent before deletion.
	 */
	public Long getParentId() {
		return parentId;
	}

	/**
	 * The ID of the original parent before deletion.
	 */
	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((deletedBy == null) ? 0 : deletedBy.hashCode());
		result = prime * result
				+ ((deletedOn == null) ? 0 : deletedOn.hashCode());
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
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
		DBOTrashedEntity other = (DBOTrashedEntity) obj;
		if (deletedBy == null) {
			if (other.deletedBy != null)
				return false;
		} else if (!deletedBy.equals(other.deletedBy))
			return false;
		if (deletedOn == null) {
			if (other.deletedOn != null)
				return false;
		} else if (!deletedOn.equals(other.deletedOn))
			return false;
		if (nodeName == null) {
			if (other.nodeName != null)
				return false;
		} else if (!nodeName.equals(other.nodeName))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		return true;
	}

	private Long nodeId;
	private String nodeName;
	private Long deletedBy;
	private Timestamp deletedOn;
	private Long parentId;

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TRASH_CAN;
	}

	@Override
	public MigratableTableTranslation<DBOTrashedEntity, DBOTrashedEntity> getTranslator() {
		return new BasicMigratableTableTranslation<DBOTrashedEntity>();
	}

	@Override
	public Class<? extends DBOTrashedEntity> getBackupClass() {
		return DBOTrashedEntity.class;
	}

	@Override
	public Class<? extends DBOTrashedEntity> getDatabaseObjectClass() {
		return DBOTrashedEntity.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}
}
