package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Keeps track of markdown information of wikis
 * 
 * @author hso
 *
 */
@Table(name = V2_TABLE_WIKI_MARKDOWN, constraints = {"UNIQUE KEY `V2_WIKI_UNIQUE_MARKDOWN_VERSION` (`" + V2_COL_WIKI_MARKDOWN_ID + "`,`" + V2_COL_WIKI_MARKDOWN_VERSION_NUM + "`)"})
public class V2DBOWikiMarkdown implements MigratableDatabaseObject<V2DBOWikiMarkdown, V2DBOWikiMarkdown> {
	
	@Field(name = V2_COL_WIKI_MARKDOWN_ID, backupId = true, primary = true, nullable = false)
	@ForeignKey(name = "V2_WIKI_MARKDOWN_FK", table = V2_TABLE_WIKI_PAGE, field = V2_COL_WIKI_ID, cascadeDelete = true)
	private Long wikiId;
	
	@Field(name = V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID, nullable = false, hasFileHandleRef = true)
	@ForeignKey(name = "V2_WIKI_MARKDOWN_FILE_HAND_FK", table = TABLE_FILES, field = COL_FILES_ID, cascadeDelete = false)
	private Long fileHandleId;
	
	@Field(name = V2_COL_WIKI_MARKDOWN_VERSION_NUM, primary = true, nullable = false)
	private Long markdownVersion;
	
	@Field(name = V2_COL_WIKI_MARKDOWN_MODIFIED_ON, nullable = false)
	private Long modifiedOn;
	
	@Field(name = V2_COL_WIKI_MARKDOWN_MODIFIED_BY, nullable = false)
	private Long modifiedBy;
	
	@Field(name = V2_COL_WIKI_MARKDOWN_TITLE, varchar = 256)
	private String title;
	
	@Field(name = V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST, type = "mediumblob", defaultNull = true)
	private byte[] attachmentIdList;

	private static TableMapping<V2DBOWikiMarkdown> tableMapping = AutoTableMapping.create(V2DBOWikiMarkdown.class);
	
	@Override
	public TableMapping<V2DBOWikiMarkdown> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_MARKDOWN;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiMarkdown, V2DBOWikiMarkdown> getTranslator() {
		return new BasicMigratableTableTranslation<V2DBOWikiMarkdown>();
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
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
