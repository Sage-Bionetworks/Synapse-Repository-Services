package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FAVORITE_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_FAVORITE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FAVORITE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * @author dburdick
 *
 */
public class DBOFavorite implements MigratableDatabaseObject<DBOFavorite, DBOFavorite>, ObservableEntity {
	
	public static final String FIELD_COLUMN_ID_PRINCIPAL_ID = "principalId";
	public static final String FIELD_COLUMN_ID_NODE_ID = "nodeId";
	
	private Long principalId;
	private Long nodeId;
	private Long createdOn;
	private Long id;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn(FIELD_COLUMN_ID_PRINCIPAL_ID, COL_FAVORITE_PRINCIPAL_ID, true),
		new FieldColumn(FIELD_COLUMN_ID_NODE_ID, COL_FAVORITE_NODE_ID, true),
		new FieldColumn("createdOn", COL_FAVORITE_CREATED_ON),
		new FieldColumn("id", COL_FAVORITE_ID).withIsBackupId(true),
		};

	@Override
	public TableMapping<DBOFavorite> getTableMapping() {
		return new TableMapping<DBOFavorite>() {
			// Map a result set to this object
			@Override
			public DBOFavorite mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOFavorite dbo = new DBOFavorite();
				dbo.setPrincipalId(rs.getLong(COL_FAVORITE_PRINCIPAL_ID));
				dbo.setNodeId(rs.getLong(COL_FAVORITE_NODE_ID));
				dbo.setCreatedOn(rs.getLong(COL_FAVORITE_CREATED_ON));
				dbo.setId(rs.getLong(COL_FAVORITE_ID));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_FAVORITE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_FAVORITE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOFavorite> getDBOClass() {
				return DBOFavorite.class;
			}
		};
	}


	@Override
	public ObjectType getObjectType() {
		return ObjectType.FAVORITE;
	}

	@Override
	public String getIdString() {
		return principalId +"-"+ nodeId;
	}

	@Override
	public String getParentIdString() {
		return null;
	}

	@Override
	public String getEtag() {
		return null;
	}

	/*
	 * Auto generated methods
	 */

	public Long getPrincipalId() {
		return principalId;
	}


	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}


	public Long getNodeId() {
		return nodeId;
	}


	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}


	public Long getCreatedOn() {
		return createdOn;
	}


	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}


	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
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
		DBOFavorite other = (DBOFavorite) obj;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOFavorite [principalId=" + principalId + ", nodeId=" + nodeId
				+ ", createdOn=" + createdOn + ", id=" + id + "]";
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.FAVORITE;
	}


	@Override
	public MigratableTableTranslation<DBOFavorite, DBOFavorite> getTranslator() {
		return new BasicMigratableTableTranslation<DBOFavorite>();
	}


	@Override
	public Class<? extends DBOFavorite> getBackupClass() {
		return DBOFavorite.class;
	}


	@Override
	public Class<? extends DBOFavorite> getDatabaseObjectClass() {
		return DBOFavorite.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

}
