package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_WIKI_PAGE;
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
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ObservableEntity;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
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
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", V2_COL_WIKI_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("etag", V2_COL_WIKI_ETAG).withIsEtag(true),
			new FieldColumn("title", V2_COL_WIKI_TITLE),
			new FieldColumn("createdBy", V2_COL_WIKI_CREATED_BY),
			new FieldColumn("createdOn", V2_COL_WIKI_CREATED_ON),
			new FieldColumn("modifiedBy", V2_COL_WIKI_MODIFIED_BY),
			new FieldColumn("modifiedOn", V2_COL_WIKI_MODIFIED_ON),
			new FieldColumn("parentId", V2_COL_WIKI_PARENT_ID),
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
		return new TableMapping<V2DBOWikiPage>() {
			
			@Override
			public V2DBOWikiPage mapRow(ResultSet rs, int rowNum) throws SQLException {
				V2DBOWikiPage dbo = new V2DBOWikiPage();
				dbo.setId(rs.getLong(V2_COL_WIKI_ID));
				dbo.setEtag(rs.getString(V2_COL_WIKI_ETAG));
				dbo.setTitle(rs.getString(V2_COL_WIKI_TITLE));
				dbo.setCreatedBy(rs.getLong(V2_COL_WIKI_CREATED_BY));
				dbo.setCreatedOn(rs.getLong(V2_COL_WIKI_CREATED_ON));
				dbo.setModifiedBy(rs.getLong(V2_COL_WIKI_MODIFIED_BY));
				dbo.setModifiedOn(rs.getLong(V2_COL_WIKI_MODIFIED_ON));
				dbo.setParentId(rs.getLong(V2_COL_WIKI_PARENT_ID));
				if(rs.wasNull()) {
					dbo.setParentId(null);
				}
				dbo.setRootId(rs.getLong(V2_COL_WIKI_ROOT_ID));
				dbo.setMarkdownVersion(rs.getLong(V2_COL_WIKI_MARKDOWN_VERSION));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return V2_TABLE_WIKI_PAGE;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_WIKI_PAGE;
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
	public String getIdString() {
		return id.toString();
	}

	@Override
	public ObjectType getObjectType() {
		return ObjectType.WIKI;
	}

	@Override
	public String getEtag() {
		return etag;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_PAGE;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiPage, V2DBOWikiPage> getTranslator() {
		return new BasicMigratableTableTranslation<V2DBOWikiPage>();
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		List<MigratableDatabaseObject<?,?>> list = new LinkedList<MigratableDatabaseObject<?,?>>();
		list.add(new V2DBOWikiAttachmentReservation());
		list.add(new V2DBOWikiMarkdown());
		return list;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, etag, id, markdownVersion, modifiedBy, modifiedOn, parentId, rootId,
				title);
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
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(markdownVersion, other.markdownVersion)
				&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(parentId, other.parentId) && Objects.equals(rootId, other.rootId)
				&& Objects.equals(title, other.title);
	}

	@Override
	public String toString() {
		return "V2DBOWikiPage [id=" + id + ", etag=" + etag + ", title=" + title + ", createdBy=" + createdBy
				+ ", createdOn=" + createdOn + ", modifiedBy=" + modifiedBy + ", modifiedOn=" + modifiedOn
				+ ", parentId=" + parentId + ", rootId=" + rootId + ", markdownVersion=" + markdownVersion + "]";
	}
	
}
