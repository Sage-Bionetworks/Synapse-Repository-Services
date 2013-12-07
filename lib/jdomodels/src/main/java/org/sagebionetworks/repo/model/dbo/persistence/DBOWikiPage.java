package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_PAGE;

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
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database Object for a Wiki Page.
 * @author John
 *
 */
public class DBOWikiPage implements MigratableDatabaseObject<DBOWikiPage, DBOWikiPage>, ObservableEntity {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_WIKI_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_WIKI_ETAG).withIsEtag(true),
		new FieldColumn("title", COL_WIKI_TITLE),
		new FieldColumn("createdBy", COL_WIKI_CREATED_BY),
		new FieldColumn("createdOn", COL_WIKI_CREATED_ON),
		new FieldColumn("modifiedBy", COL_WIKI_MODIFIED_BY),
		new FieldColumn("modifiedOn", COL_WIKI_MODIFIED_ON),
		new FieldColumn("parentId", COL_WIKI_PARENT_ID).withIsSelfForeignKey(true),
		new FieldColumn("rootId", COL_WIKI_ROOT_ID),
		new FieldColumn("markdown", COL_WIKI_MARKDOWN),
	};
	
	private Long id;
	private String etag;
	private String title;
	private Long createdBy;
	private Long createdOn;
	private Long modifiedBy;
	private Long modifiedOn;
	private Long parentId;
	private Long rootId;
	private byte[] markdown;

	@Override
	public TableMapping<DBOWikiPage> getTableMapping() {
		return new TableMapping<DBOWikiPage>(){
			@Override
			public DBOWikiPage mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOWikiPage wiki = new DBOWikiPage();
				wiki.setId(rs.getLong(COL_WIKI_ID));
				wiki.setEtag(rs.getString(COL_WIKI_ETAG));
				wiki.setTitle(rs.getString(COL_WIKI_TITLE));
				wiki.setCreatedBy(rs.getLong(COL_WIKI_CREATED_BY));
				wiki.setCreatedOn(rs.getLong(COL_WIKI_CREATED_ON));
				wiki.setModifiedBy(rs.getLong(COL_WIKI_MODIFIED_BY));
				wiki.setModifiedOn(rs.getLong(COL_WIKI_MODIFIED_ON));
				wiki.setParentId(rs.getLong(COL_WIKI_PARENT_ID));
				if(rs.wasNull()){
					wiki.setParentId(null);
				}
				java.sql.Blob blob = rs.getBlob(COL_WIKI_MARKDOWN);
				if(blob != null){
					wiki.setMarkdown(blob.getBytes(1, (int) blob.length()));
				}
				wiki.setRootId(rs.getLong(COL_WIKI_ROOT_ID));
				return wiki;
			}

			@Override
			public String getTableName() {
				return TABLE_WIKI_PAGE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_WIKI_PAGE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOWikiPage> getDBOClass() {
				return DBOWikiPage.class;
			}
			
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String getIdString() {
		return id.toString();
	}

	@Override
	public String getParentIdString() {
		return null;
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.WIKI;
	}

	@Override
	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
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

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Long getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public byte[] getMarkdown() {
		return markdown;
	}

	public void setMarkdown(byte[] markdown) {
		this.markdown = markdown;
	}

	public Long getRootId() {
		return rootId;
	}

	public void setRootId(Long rootId) {
		this.rootId = rootId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + Arrays.hashCode(markdown);
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((rootId == null) ? 0 : rootId.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
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
		DBOWikiPage other = (DBOWikiPage) obj;
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
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (!Arrays.equals(markdown, other.markdown))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (rootId == null) {
			if (other.rootId != null)
				return false;
		} else if (!rootId.equals(other.rootId))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOWikiPage [id=" + id + ", etag=" + etag + ", title=" + title
				+ ", createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", parentId=" + parentId + ", markdown="
				+ ((markdown == null) ? "null" : new String(markdown)) + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.WIKI_PAGE;
	}

	@Override
	public MigratableTableTranslation<DBOWikiPage, DBOWikiPage> getTranslator() {
		return new MigratableTableTranslation<DBOWikiPage, DBOWikiPage>(){

			@Override
			public DBOWikiPage createDatabaseObjectFromBackup(DBOWikiPage backup) {
				return backup;
			}

			@Override
			public DBOWikiPage createBackupFromDatabaseObject(DBOWikiPage dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOWikiPage> getBackupClass() {
		return DBOWikiPage.class;
	}

	@Override
	public Class<? extends DBOWikiPage> getDatabaseObjectClass() {
		return DBOWikiPage.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		List<MigratableDatabaseObject> list = new LinkedList<MigratableDatabaseObject>();
		list.add(new DBOWikiAttachment());
		return list;
	}
}
