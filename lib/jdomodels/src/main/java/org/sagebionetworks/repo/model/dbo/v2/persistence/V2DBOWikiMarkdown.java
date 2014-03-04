package org.sagebionetworks.repo.model.dbo.v2.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_DDL_FILE_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Keeps track of markdown information of wikis
 * 
 * @author hso
 *
 */
public class V2DBOWikiMarkdown implements MigratableDatabaseObject<V2DBOWikiMarkdown, V2DBOWikiMarkdown> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("wikiId", V2_COL_WIKI_MARKDOWN_ID, true).withIsBackupId(true),
		new FieldColumn("fileHandleId", V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID),
		new FieldColumn("markdownVersion", V2_COL_WIKI_MARKDOWN_VERSION_NUM, true),
		new FieldColumn("modifiedOn", V2_COL_WIKI_MARKDOWN_MODIFIED_ON),
		new FieldColumn("modifiedBy", V2_COL_WIKI_MARKDOWN_MODIFIED_BY),
		new FieldColumn("title", V2_COL_WIKI_MARKDOWN_TITLE),
		new FieldColumn("attachmentIdList", V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST),
	};
	
	private Long wikiId;
	private Long fileHandleId;
	private Long markdownVersion;
	private Long modifiedOn;
	private Long modifiedBy;
	private String title;
	private byte[] attachmentIdList;

	@Override
	public TableMapping<V2DBOWikiMarkdown> getTableMapping() {
		return new TableMapping<V2DBOWikiMarkdown>() {

			@Override
			public V2DBOWikiMarkdown mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				V2DBOWikiMarkdown result = new V2DBOWikiMarkdown();
				result.setWikiId(rs.getLong(V2_COL_WIKI_MARKDOWN_ID));
				result.setFileHandleId(rs.getLong(V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID));
				result.setMarkdownVersion(rs.getLong(V2_COL_WIKI_MARKDOWN_VERSION_NUM));
				result.setModifiedOn(rs.getLong(V2_COL_WIKI_MARKDOWN_MODIFIED_ON));
				result.setModifiedBy(rs.getLong(V2_COL_WIKI_MARKDOWN_MODIFIED_BY));
				result.setTitle(rs.getString(V2_COL_WIKI_MARKDOWN_TITLE));
				java.sql.Blob blob = rs.getBlob(V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST);
				if(blob != null){
					result.setAttachmentIdList(blob.getBytes(1, (int) blob.length()));
				}
				return result;
			}

			@Override
			public String getTableName() {
				return V2_TABLE_WIKI_MARKDOWN;
			}

			@Override
			public String getDDLFileName() {
				return V2_DDL_FILE_WIKI_MARKDOWN;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends V2DBOWikiMarkdown> getDBOClass() {
				return V2DBOWikiMarkdown.class;
			}	
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_MARKDOWN;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiMarkdown, V2DBOWikiMarkdown> getTranslator() {
		return new MigratableTableTranslation<V2DBOWikiMarkdown, V2DBOWikiMarkdown>(){

			@Override
			public V2DBOWikiMarkdown createDatabaseObjectFromBackup(
					V2DBOWikiMarkdown backup) {
				return backup;
			}

			@Override
			public V2DBOWikiMarkdown createBackupFromDatabaseObject(
					V2DBOWikiMarkdown dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends V2DBOWikiMarkdown> getBackupClass() {
		return V2DBOWikiMarkdown.class;
	}

	@Override
	public Class<? extends V2DBOWikiMarkdown> getDatabaseObjectClass() {
		return V2DBOWikiMarkdown.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	public Long getWikiId() {
		return wikiId;
	}

	public void setWikiId(Long wikiId) {
		this.wikiId = wikiId;
	}

	public Long getFileHandleId() {
		return fileHandleId;
	}

	public void setFileHandleId(Long fileHandleId) {
		this.fileHandleId = fileHandleId;
	}
	
	public Long getMarkdownVersion() {
		return markdownVersion;
	}
	
	public void setMarkdownVersion(Long markdownVersion) {
		this.markdownVersion = markdownVersion;
	}
	
	public Long getModifiedOn() {
		return modifiedOn;
	}
	
	public void setModifiedOn(Long modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	
	public Long getModifiedBy() {
		return modifiedBy;
	}
	
	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}
	
	public byte[] getAttachmentIdList() {
		return attachmentIdList;
	}
	
	public void setAttachmentIdList(byte[] attachmentIdList) {
		this.attachmentIdList = attachmentIdList;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((markdownVersion == null) ? 0 : markdownVersion.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((attachmentIdList == null) ? 0 : attachmentIdList.hashCode());
		result = prime * result + ((wikiId == null) ? 0 : wikiId.hashCode());
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
		V2DBOWikiMarkdown other = (V2DBOWikiMarkdown) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (markdownVersion == null) {
			if (other.markdownVersion != null)
				return false;
		} else if (!markdownVersion.equals(other.markdownVersion))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (attachmentIdList == null) {
			if (other.attachmentIdList != null)
				return false;
		} else if (!attachmentIdList.equals(other.attachmentIdList))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (wikiId == null) {
			if (other.wikiId != null)
				return false;
		} else if (!wikiId.equals(other.wikiId))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "DBOWikiMarkdown [wikiId=" + wikiId + ", fileHandleId="
				+ fileHandleId + ", markdownVersion=" + markdownVersion + ", modifiedOn=" 
				+ modifiedOn + ", markdownBy=" + modifiedBy + ", title=" 
				+ title + ", attachmentIdList=" + attachmentIdList + "]";
	}
}
