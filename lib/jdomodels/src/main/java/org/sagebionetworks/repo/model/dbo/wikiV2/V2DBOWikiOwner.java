package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_OWNERS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_OWNERS_ORDER_HINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_OWNERS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class V2DBOWikiOwner implements MigratableDatabaseObject<V2DBOWikiOwner, V2DBOWikiOwner> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("ownerId", V2_COL_WIKI_ONWERS_OWNER_ID).withIsPrimaryKey(true),
			new FieldColumn("ownerType", V2_COL_WIKI_ONWERS_OBJECT_TYPE).withIsPrimaryKey(true),
			new FieldColumn("rootWikiId", V2_COL_WIKI_ONWERS_ROOT_WIKI_ID).withIsBackupId(true),
			new FieldColumn("orderHint", V2_COL_WIKI_OWNERS_ORDER_HINT),
			new FieldColumn("etag", V2_COL_WIKI_OWNERS_ETAG).withIsEtag(true),
	};

	private Long ownerId;
	private String ownerType;
	private Long rootWikiId;
	private byte[] orderHint;
	private String etag;
	
	@Override
	public TableMapping<V2DBOWikiOwner> getTableMapping() {
		return new TableMapping<V2DBOWikiOwner>() {
			
			@Override
			public V2DBOWikiOwner mapRow(ResultSet rs, int rowNum) throws SQLException {
				V2DBOWikiOwner dbo = new V2DBOWikiOwner();
				dbo.setOwnerId(rs.getLong(V2_COL_WIKI_ONWERS_OWNER_ID));
				dbo.setOwnerType(rs.getString(V2_COL_WIKI_ONWERS_OBJECT_TYPE));
				dbo.setRootWikiId(rs.getLong(V2_COL_WIKI_ONWERS_ROOT_WIKI_ID));
				dbo.setOrderHint(rs.getBytes(V2_COL_WIKI_OWNERS_ORDER_HINT));
				dbo.setEtag(rs.getString(V2_COL_WIKI_OWNERS_ETAG));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return V2_TABLE_WIKI_OWNERS;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_WIKI_OWNERS;
			}
			
			@Override
			public Class<? extends V2DBOWikiOwner> getDBOClass() {
				return V2DBOWikiOwner.class;
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
		return ownerType;
	}
	
	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}
	
	public Long getRootWikiId() {
		return rootWikiId;
	}
	
	public void setRootWikiId(Long rootWikiId) {
		this.rootWikiId = rootWikiId;
	}
	
	public void setOrderHint(byte[] orderHint) {
		this.orderHint = orderHint;
	}
	
	public byte[] getOrderHint() {
		return orderHint;
	}
	
	public void setEtag(String etag) {
		this.etag = etag;
	}
	
	public String getEtag() {
		return etag;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_OWNERS;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwner> getTranslator() {
		return new BasicMigratableTableTranslation<V2DBOWikiOwner>();
	}

	@Override
	public Class<? extends V2DBOWikiOwner> getBackupClass() {
		return V2DBOWikiOwner.class;
	}

	@Override
	public Class<? extends V2DBOWikiOwner> getDatabaseObjectClass() {
		return V2DBOWikiOwner.class;
	}
	
	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
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
