package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ONWERS_ROOT_WIKI_ID;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.message.ObjectType;

public class DBOWikiOwner implements DatabaseObject<DBOWikiOwner> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("ownerId", COL_WIKI_ONWERS_OWNER_ID, true),
		new FieldColumn("ownerType", COL_WIKI_ONWERS_OBJECT_TYPE, true),
		new FieldColumn("rootWikiId", COL_WIKI_ONWERS_ROOT_WIKI_ID),
	};
	
	private Long ownerId;
	private ObjectType ownerType;
	private Long rootWikiId;

	@Override
	public TableMapping<DBOWikiOwner> getTableMapping() {
		return new TableMapping<DBOWikiOwner>() {

			@Override
			public DBOWikiOwner mapRow(ResultSet rs, int index)
					throws SQLException {
				DBOWikiOwner result = new DBOWikiOwner();
				result.setOwnerId(rs.getLong(COL_WIKI_ONWERS_OWNER_ID));
				result.setOwnerType(rs.getString(COL_WIKI_ONWERS_OBJECT_TYPE));
				result.setRootWikiId(rs.getLong(COL_WIKI_ONWERS_ROOT_WIKI_ID));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_WIKI_OWNERS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_WIKI_ONWERS;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOWikiOwner> getDBOClass() {
				return DBOWikiOwner.class;
			}
		};
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
		DBOWikiOwner other = (DBOWikiOwner) obj;
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

	@Override
	public String toString() {
		return "DBOWikiOwners [ownerId=" + ownerId + ", ownerType=" + ownerType
				+ ", rootWikiId=" + rootWikiId + "]";
	}

}
