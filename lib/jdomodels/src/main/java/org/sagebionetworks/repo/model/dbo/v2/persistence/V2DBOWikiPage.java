package org.sagebionetworks.repo.model.dbo.v2.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ROOT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_DDL_FILE_WIKI_PAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;

import java.sql.ResultSet;
import java.sql.SQLException;
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
 * (Derived from DBOWikiPage of org.sagebionetworks.repo.model.dbo.persistence)
 * 
 * @author hso
 *
 */
public class V2DBOWikiPage implements MigratableDatabaseObject<V2DBOWikiPage, V2DBOWikiPage>, ObservableEntity {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", V2_COL_WIKI_ID, true).withIsBackupId(true),
		new FieldColumn("etag", V2_COL_WIKI_ETAG).withIsEtag(true),
		new FieldColumn("title", V2_COL_WIKI_TITLE),
		new FieldColumn("createdBy", V2_COL_WIKI_CREATED_BY),
		new FieldColumn("createdOn", V2_COL_WIKI_CREATED_ON),
		new FieldColumn("modifiedBy", V2_COL_WIKI_MODIFIED_BY),
		new FieldColumn("modifiedOn", V2_COL_WIKI_MODIFIED_ON),
		new FieldColumn("parentId", V2_COL_WIKI_PARENT_ID).withIsSelfForeignKey(true),
		new FieldColumn("rootId", V2_COL_WIKI_ROOT_ID),
		new FieldColumn("markdownVersion", V2_COL_WIKI_MARKDOWN_VERSION),
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
	private Long markdownVersion;

	@Override
	public TableMapping<V2DBOWikiPage> getTableMapping() {
		return new TableMapping<V2DBOWikiPage>(){
			@Override
			public V2DBOWikiPage mapRow(ResultSet rs, int rowNum) throws SQLException {
				V2DBOWikiPage wiki = new V2DBOWikiPage();
				wiki.setId(rs.getLong(V2_COL_WIKI_ID));
				wiki.setEtag(rs.getString(V2_COL_WIKI_ETAG));
				wiki.setTitle(rs.getString(V2_COL_WIKI_TITLE));
				wiki.setCreatedBy(rs.getLong(V2_COL_WIKI_CREATED_BY));
				wiki.setCreatedOn(rs.getLong(V2_COL_WIKI_CREATED_ON));
				wiki.setModifiedBy(rs.getLong(V2_COL_WIKI_MODIFIED_BY));
				wiki.setModifiedOn(rs.getLong(V2_COL_WIKI_MODIFIED_ON));
				wiki.setParentId(rs.getLong(V2_COL_WIKI_PARENT_ID));
				if(rs.wasNull()){
					wiki.setParentId(null);
				}
				wiki.setMarkdownVersion(rs.getLong(V2_COL_WIKI_MARKDOWN_VERSION));
				wiki.setRootId(rs.getLong(V2_COL_WIKI_ROOT_ID));
				return wiki;
			}

			@Override
			public String getTableName() {
				return V2_TABLE_WIKI_PAGE;
			}

			@Override
			public String getDDLFileName() {
				return V2_DDL_FILE_WIKI_PAGE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends V2DBOWikiPage> getDBOClass() {
				return V2DBOWikiPage.class;
			}
			
		};
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Long getMarkdownVersion() {
		return markdownVersion;
	}

	public void setMarkdownVersion(Long markdownVersion) {
		this.markdownVersion = markdownVersion;
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
		result = prime * result + ((markdownVersion == null) ? 0 : markdownVersion.hashCode());
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
		V2DBOWikiPage other = (V2DBOWikiPage) obj;
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
		if (markdownVersion == null) {
			if (other.markdownVersion != null)
				return false;
		} else if (!markdownVersion.equals(other.markdownVersion))
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
				+ ", parentId=" + parentId + ", markdownVersion=" + markdownVersion + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_PAGE;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiPage, V2DBOWikiPage> getTranslator() {
		return new MigratableTableTranslation<V2DBOWikiPage, V2DBOWikiPage>(){

			@Override
			public V2DBOWikiPage createDatabaseObjectFromBackup(V2DBOWikiPage backup) {
				return backup;
			}

			@Override
			public V2DBOWikiPage createBackupFromDatabaseObject(V2DBOWikiPage dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends V2DBOWikiPage> getBackupClass() {
		return V2DBOWikiPage.class;
	}

	@Override
	public Class<? extends V2DBOWikiPage> getDatabaseObjectClass() {
		return V2DBOWikiPage.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		List<MigratableDatabaseObject> list = new LinkedList<MigratableDatabaseObject>();
		list.add(new V2DBOWikiAttachmentReservation());
		list.add(new V2DBOWikiMarkdown());
		return list;
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
}
