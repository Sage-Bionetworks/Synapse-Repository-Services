package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_WIKI_MARKDOWN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_TITLE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_MARKDOWN_VERSION_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_MARKDOWN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
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
public class V2DBOWikiMarkdown implements MigratableDatabaseObject<V2DBOWikiMarkdown, V2DBOWikiMarkdown> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("wikiId", V2_COL_WIKI_MARKDOWN_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("fileHandleId", V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID).withHasFileHandleRef(true),
			new FieldColumn("markdownVersion", V2_COL_WIKI_MARKDOWN_VERSION_NUM).withIsPrimaryKey(true),
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
			public V2DBOWikiMarkdown mapRow(ResultSet rs, int rowNum) throws SQLException {
				V2DBOWikiMarkdown dbo = new V2DBOWikiMarkdown();
				dbo.setWikiId(rs.getLong(V2_COL_WIKI_MARKDOWN_ID));
				dbo.setFileHandleId(rs.getLong(V2_COL_WIKI_MARKDOWN_FILE_HANDLE_ID));
				dbo.setMarkdownVersion(rs.getLong(V2_COL_WIKI_MARKDOWN_VERSION_NUM));
				dbo.setModifiedOn(rs.getLong(V2_COL_WIKI_MARKDOWN_MODIFIED_ON));
				dbo.setModifiedBy(rs.getLong(V2_COL_WIKI_MARKDOWN_MODIFIED_BY));
				dbo.setTitle(rs.getString(V2_COL_WIKI_MARKDOWN_TITLE));
				dbo.setAttachmentIdList(rs.getBytes(V2_COL_WIKI_MARKDOWN_ATTACHMENT_ID_LIST));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return V2_TABLE_WIKI_MARKDOWN;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_WIKI_MARKDOWN;
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
		result = prime * result + Arrays.hashCode(attachmentIdList);
		result = prime * result + Objects.hash(fileHandleId, markdownVersion, modifiedBy, modifiedOn, title, wikiId);
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
		return Arrays.equals(attachmentIdList, other.attachmentIdList)
				&& Objects.equals(fileHandleId, other.fileHandleId)
				&& Objects.equals(markdownVersion, other.markdownVersion)
				&& Objects.equals(modifiedBy, other.modifiedBy) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(title, other.title) && Objects.equals(wikiId, other.wikiId);
	}
	
	@Override
	public String toString() {
		return "V2DBOWikiMarkdown [wikiId=" + wikiId + ", fileHandleId=" + fileHandleId + ", markdownVersion="
				+ markdownVersion + ", modifiedOn=" + modifiedOn + ", modifiedBy=" + modifiedBy + ", title=" + title
				+ ", attachmentIdList=" + Arrays.toString(attachmentIdList) + "]";
	}
}
