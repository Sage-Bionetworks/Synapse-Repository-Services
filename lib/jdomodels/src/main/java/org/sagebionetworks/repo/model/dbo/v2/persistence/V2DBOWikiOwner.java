package org.sagebionetworks.repo.model.dbo.v2.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STAT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_DDL_FILE_WIKI_ONWERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOProjectStat;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = V2_TABLE_WIKI_OWNERS, constraints = {"UNIQUE INDEX (`" + V2_COL_WIKI_ONWERS_ROOT_WIKI_ID + "`)"})
public class V2DBOWikiOwner implements MigratableDatabaseObject<V2DBOWikiOwner, V2DBOWikiOwner> {
	
	@Field(name = V2_COL_WIKI_ONWERS_OWNER_ID, nullable = false)
	private Long ownerId;
	
	@Field(name = V2_COL_WIKI_ONWERS_OBJECT_TYPE, nullable = false)
	private ObjectType ownerType;
	
	@Field(name = V2_COL_WIKI_ONWERS_ROOT_WIKI_ID, nullable = false)
	@ForeignKey(name = "V2_WIKI_OWNER_FK", table = V2_TABLE_WIKI_PAGE, field = V2_COL_WIKI_ID, cascadeDelete = true)
	private Long rootWikiId;
	
	private static TableMapping<V2DBOWikiOwner> tableMapping = AutoTableMapping.create(V2DBOWikiOwner.class);
	
	@Override
	public TableMapping<V2DBOWikiOwner> getTableMapping() {
		return tableMapping;
	}
	
	public Long getOwnerId() {
		return ownerId;
	}
	
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}
	
	public String getOwnerType() {
		return ownerType.name();
	}
	
	public void setOwnerType(String ownerType) {
		this.ownerType = ObjectType.valueOf(ownerType);
	}
	
	public ObjectType getOwnerTypeEnum(){
		return this.ownerType;
	}
	
	public void setOwnerTypeEnum(ObjectType owner){
		this.ownerType = owner;
	}
	public Long getRootWikiId() {
		return rootWikiId;
	}
	
	public void setRootWikiId(Long rootWikiId) {
		this.rootWikiId = rootWikiId;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_OWNERS;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwner> getTranslator() {
		// TODO: (This??) We do not currently have a backup for this object.
		return new MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwner>(){
			
			@Override
			public V2DBOWikiOwner createDatabaseObjectFromBackup(
					V2DBOWikiOwner backup) {
				return backup;
			}
	
			@Override
			public V2DBOWikiOwner createBackupFromDatabaseObject(V2DBOWikiOwner dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends V2DBOWikiOwner> getBackupClass() {
		return V2DBOWikiOwner.class;	// TODO: Is there a backup class?
	}

	@Override
	public Class<? extends V2DBOWikiOwner> getDatabaseObjectClass() {
		return V2DBOWikiOwner.class;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;	// TODO: This?
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result
				+ ((ownerType == null) ? 0 : ownerType.hashCode());
		result = prime * result
				+ ((rootWikiId == null) ? 0 : rootWikiId.hashCode());
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
		V2DBOWikiOwner other = (V2DBOWikiOwner) obj;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (ownerType != other.ownerType)
			return false;
		if (rootWikiId == null) {
			if (other.rootWikiId != null)
				return false;
		} else if (!rootWikiId.equals(other.rootWikiId))
			return false;
		return true;
	}
	
	
}

///**
// * Keeps track of the owner of wikis
// * (Derived from DBOWikiOwner of org.sagebionetworks.repo.model.dbo.persistence)
// * 
// * @author hso
// *
// */
//public class V2DBOWikiOwner implements MigratableDatabaseObject<V2DBOWikiOwner, V2DBOWikiOwner> {
//	
//	private static final FieldColumn[] FIELDS = new FieldColumn[] {
//		new FieldColumn("ownerId", V2_COL_WIKI_ONWERS_OWNER_ID, true),
//		new FieldColumn("ownerType", V2_COL_WIKI_ONWERS_OBJECT_TYPE, true),
//		new FieldColumn("rootWikiId", V2_COL_WIKI_ONWERS_ROOT_WIKI_ID).withIsBackupId(true),
//	};
//	
//	private Long ownerId;
//	private ObjectType ownerType;
//	private Long rootWikiId;
//
//	@Override
//	public TableMapping<V2DBOWikiOwner> getTableMapping() {
//		return new TableMapping<V2DBOWikiOwner>() {
//
//			@Override
//			public V2DBOWikiOwner mapRow(ResultSet rs, int index)
//					throws SQLException {
//				V2DBOWikiOwner result = new V2DBOWikiOwner();
//				result.setOwnerId(rs.getLong(V2_COL_WIKI_ONWERS_OWNER_ID));
//				result.setOwnerType(rs.getString(V2_COL_WIKI_ONWERS_OBJECT_TYPE));
//				result.setRootWikiId(rs.getLong(V2_COL_WIKI_ONWERS_ROOT_WIKI_ID));
//				return result;
//			}
//
//			@Override
//			public String getTableName() {
//				return V2_TABLE_WIKI_OWNERS;
//			}
//
//			@Override
//			public String getDDLFileName() {
//				return V2_DDL_FILE_WIKI_ONWERS;
//			}
//
//			@Override
//			public FieldColumn[] getFieldColumns() {
//				return FIELDS;
//			}
//
//			@Override
//			public Class<? extends V2DBOWikiOwner> getDBOClass() {
//				return V2DBOWikiOwner.class;
//			}
//		};
//	}
//
//	public Long getOwnerId() {
//		return ownerId;
//	}
//
//	public void setOwnerId(Long ownerId) {
//		this.ownerId = ownerId;
//	}
//
//	public String getOwnerType() {
//		return ownerType.name();
//	}
//
//	public void setOwnerType(String ownerType) {
//		this.ownerType = ObjectType.valueOf(ownerType);
//	}
//	
//	public ObjectType getOwnerTypeEnum(){
//		return this.ownerType;
//	}
//
//	public void setOwnerTypeEnum(ObjectType owner){
//		this.ownerType = owner;
//	}
//	public Long getRootWikiId() {
//		return rootWikiId;
//	}
//
//	public void setRootWikiId(Long rootWikiId) {
//		this.rootWikiId = rootWikiId;
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
//		result = prime * result
//				+ ((ownerType == null) ? 0 : ownerType.hashCode());
//		result = prime * result
//				+ ((rootWikiId == null) ? 0 : rootWikiId.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj)
//			return true;
//		if (obj == null)
//			return false;
//		if (getClass() != obj.getClass())
//			return false;
//		V2DBOWikiOwner other = (V2DBOWikiOwner) obj;
//		if (ownerId == null) {
//			if (other.ownerId != null)
//				return false;
//		} else if (!ownerId.equals(other.ownerId))
//			return false;
//		if (ownerType != other.ownerType)
//			return false;
//		if (rootWikiId == null) {
//			if (other.rootWikiId != null)
//				return false;
//		} else if (!rootWikiId.equals(other.rootWikiId))
//			return false;
//		return true;
//	}
//
//	@Override
//	public String toString() {
//		return "DBOWikiOwners [ownerId=" + ownerId + ", ownerType=" + ownerType
//				+ ", rootWikiId=" + rootWikiId + "]";
//	}
//
//	@Override
//	public MigrationType getMigratableTableType() {
//		return MigrationType.V2_WIKI_OWNERS;
//	}
//
//	@Override
//	public MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwner> getTranslator() {
//		return new MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwner>(){
//
//			@Override
//			public V2DBOWikiOwner createDatabaseObjectFromBackup(
//					V2DBOWikiOwner backup) {
//				return backup;
//			}
//
//			@Override
//			public V2DBOWikiOwner createBackupFromDatabaseObject(V2DBOWikiOwner dbo) {
//				return dbo;
//			}};
//	}
//
//	@Override
//	public Class<? extends V2DBOWikiOwner> getBackupClass() {
//		// TODO Auto-generated method stub
//		return V2DBOWikiOwner.class;
//	}
//
//	@Override
//	public Class<? extends V2DBOWikiOwner> getDatabaseObjectClass() {
//		return V2DBOWikiOwner.class;
//	}
//
//	@Override
//	public List<MigratableDatabaseObject> getSecondaryTypes() {
//		return null;
//	}
//
//}
