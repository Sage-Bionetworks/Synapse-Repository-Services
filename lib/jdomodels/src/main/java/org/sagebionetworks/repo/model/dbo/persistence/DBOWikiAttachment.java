package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_FILE_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WIKI_ATTACHMENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_WIKI_ATTATCHMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WIKI_ATTACHMENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Keeps track of wiki attachments.
 * 
 * @author jmhill
 *
 */
public class DBOWikiAttachment implements MigratableDatabaseObject<DBOWikiAttachment, DBOWikiAttachment>  {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("wikiId", COL_WIKI_ATTACHMENT_ID, true).withIsBackupId(true),
		new FieldColumn("fileHandleId", COL_WIKI_ATTACHMENT_FILE_HANDLE_ID, true),
		new FieldColumn("fileName", COL_WIKI_ATTACHMENT_FILE_NAME),
	};

	private Long wikiId;
	private Long fileHandleId;
	private String fileName;
	
	@Override
	public TableMapping<DBOWikiAttachment> getTableMapping() {
		return new TableMapping<DBOWikiAttachment>(){

			@Override
			public DBOWikiAttachment mapRow(ResultSet rs, int rowNum)throws SQLException {
				DBOWikiAttachment result = new DBOWikiAttachment();
				result.setWikiId(rs.getLong(COL_WIKI_ATTACHMENT_ID));
				result.setFileHandleId(rs.getLong(COL_WIKI_ATTACHMENT_FILE_HANDLE_ID));
				result.setFileName(rs.getString(COL_WIKI_ATTACHMENT_FILE_NAME));
				return result;
			}

			@Override
			public String getTableName() {
				return TABLE_WIKI_ATTACHMENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_WIKI_ATTATCHMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOWikiAttachment> getDBOClass() {
				return DBOWikiAttachment.class;
			}
			
		};
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

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
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
		DBOWikiAttachment other = (DBOWikiAttachment) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
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
		return "DBOWikiAttachment [wikiId=" + wikiId + ", fileHandleId="
				+ fileHandleId + ", fileName=" + fileName + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.WIKI_ATTACHMENT;
	}

	@Override
	public MigratableTableTranslation<DBOWikiAttachment, DBOWikiAttachment> getTranslator() {
		return new MigratableTableTranslation<DBOWikiAttachment, DBOWikiAttachment>(){

			@Override
			public DBOWikiAttachment createDatabaseObjectFromBackup(
					DBOWikiAttachment backup) {
				return backup;
			}

			@Override
			public DBOWikiAttachment createBackupFromDatabaseObject(
					DBOWikiAttachment dbo) {
				return dbo;
			}};
	}

	@Override
	public Class<? extends DBOWikiAttachment> getBackupClass() {
		return DBOWikiAttachment.class;
	}

	@Override
	public Class<? extends DBOWikiAttachment> getDatabaseObjectClass() {
		return DBOWikiAttachment.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}


}
