package org.sagebionetworks.repo.model.dbo.v2.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ONWERS_ROOT_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_OWNERS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_OWNERS_ORDER_HINT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_OWNERS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = V2_TABLE_WIKI_OWNERS, constraints = {"UNIQUE INDEX (`" + V2_COL_WIKI_ONWERS_ROOT_WIKI_ID + "`)"})
public class V2DBOWikiOwner implements MigratableDatabaseObject<V2DBOWikiOwner, V2DBOWikiOwnerBackup> {
	
	public static final String MIGRATED_NEW_ETAG = "migrated-new-etag";

	@Field(name = V2_COL_WIKI_ONWERS_OWNER_ID, primary = true, nullable = false)
	private Long ownerId;
	
	@Field(name = V2_COL_WIKI_ONWERS_OBJECT_TYPE, primary = true, nullable = false)
	private ObjectType ownerTypeEnum;
	
	@Field(name = V2_COL_WIKI_ONWERS_ROOT_WIKI_ID, nullable = false, backupId = true)
	@ForeignKey(name = "V2_WIKI_OWNER_FK", table = V2_TABLE_WIKI_PAGE, field = V2_COL_WIKI_ID, cascadeDelete = true)
	private Long rootWikiId;
	
	@Field(name = V2_COL_WIKI_OWNERS_ORDER_HINT, type = "mediumblob", defaultNull = true)
	private byte[] orderHint;
	
	@Field(name = V2_COL_WIKI_OWNERS_ETAG, nullable = false, etag = true)
	private String etag;
	
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
		return ownerTypeEnum.name();
	}
	
	public void setOwnerType(String ownerType) {
		this.ownerTypeEnum = ObjectType.valueOf(ownerType);
	}
	
	public ObjectType getOwnerTypeEnum(){
		return this.ownerTypeEnum;
	}
	
	public void setOwnerTypeEnum(ObjectType owner){
		this.ownerTypeEnum = owner;
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
	public MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwnerBackup> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<V2DBOWikiOwner, V2DBOWikiOwnerBackup>(){
			
			@Override
			public V2DBOWikiOwner createDatabaseObjectFromBackup(
					V2DBOWikiOwnerBackup backup) {
				V2DBOWikiOwner dbo  = new  V2DBOWikiOwner();
				if(backup.getEtag() == null){
					dbo.setEtag(MIGRATED_NEW_ETAG);
				}else{
					dbo.setEtag(backup.getEtag());
				}
				dbo.setOrderHint(backup.getOrderHint());
				dbo.setOwnerId(backup.getOwnerId());
				dbo.setOwnerTypeEnum(backup.getOwnerType());
				dbo.setRootWikiId(backup.getRootWikiId());
				return dbo;
			}
	
			@Override
			public V2DBOWikiOwnerBackup createBackupFromDatabaseObject(V2DBOWikiOwner dbo) {
				V2DBOWikiOwnerBackup backup = new V2DBOWikiOwnerBackup();
				backup.setEtag(dbo.getEtag());
				backup.setOrderHint(dbo.getOrderHint());
				backup.setOwnerId(dbo.getOwnerId());
				backup.setOwnerType(dbo.getOwnerTypeEnum());
				backup.setRootWikiId(dbo.getRootWikiId());
				return backup;
			}};
	}

	@Override
	public Class<? extends V2DBOWikiOwnerBackup> getBackupClass() {
		return V2DBOWikiOwnerBackup.class;
	}

	@Override
	public Class<? extends V2DBOWikiOwner> getDatabaseObjectClass() {
		return V2DBOWikiOwner.class;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result
				+ ((ownerTypeEnum == null) ? 0 : ownerTypeEnum.hashCode());
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
		if (ownerTypeEnum != other.ownerTypeEnum)
			return false;
		if (rootWikiId == null) {
			if (other.rootWikiId != null)
				return false;
		} else if (!rootWikiId.equals(other.rootWikiId))
			return false;
		return true;
	}
	
	
}
