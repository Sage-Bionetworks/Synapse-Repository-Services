package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_PAGE;

import java.sql.Timestamp;
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
@Table(name = V2_TABLE_WIKI_ATTACHMENT_RESERVATION, constraints={"UNIQUE KEY `V2_WIKI_UNIQUE_FILE_HANDLE_ID` (`"+V2_COL_WIKI_ATTACHMENT_RESERVATION_ID+"`, `" + V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID + "`)"})
public class V2DBOWikiAttachmentReservation implements MigratableDatabaseObject<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> {

	@Field(name = V2_COL_WIKI_ATTACHMENT_RESERVATION_ID, primary = true, backupId = true, nullable = false)
	@ForeignKey(name = "V2_WIKI_ATTACH_RESERVE_FK", table = V2_TABLE_WIKI_PAGE, field = V2_COL_WIKI_ID, cascadeDelete = true)
	private Long wikiId;
	
	@Field(name = V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID, primary = true, nullable = false, hasFileHandleRef = true)
	@ForeignKey(name = "V2_WIKI_FILE_HAND_RESERVE_FK", table = TABLE_FILES, field = COL_FILES_ID, cascadeDelete = false)
	private Long fileHandleId;
	
	@Field(name = V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP, nullable = false)
	private Timestamp timeStamp;
	
	private static TableMapping<V2DBOWikiAttachmentReservation> tableMapping = AutoTableMapping.create(V2DBOWikiAttachmentReservation.class);
	
	@Override
	public TableMapping<V2DBOWikiAttachmentReservation> getTableMapping() {
		return tableMapping;
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

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result
				+ ((timeStamp == null) ? 0 : timeStamp.hashCode());
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
		V2DBOWikiAttachmentReservation other = (V2DBOWikiAttachmentReservation) obj;
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (wikiId == null) {
			if (other.wikiId != null)
				return false;
		} else if (!wikiId.equals(other.wikiId))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOWikiAttachmentReservation [wikiId=" + wikiId + ", fileHandleId="
				+ fileHandleId + ", timeStamp=" + timeStamp + "]";
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.V2_WIKI_ATTACHMENT_RESERVATION;
	}

	@Override
	public MigratableTableTranslation<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> getTranslator() {
		return new BasicMigratableTableTranslation<V2DBOWikiAttachmentReservation>();
	}

	@Override
	public Class<? extends V2DBOWikiAttachmentReservation> getBackupClass() {
		return V2DBOWikiAttachmentReservation.class;
	}

	@Override
	public Class<? extends V2DBOWikiAttachmentReservation> getDatabaseObjectClass() {
		return V2DBOWikiAttachmentReservation.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

}
