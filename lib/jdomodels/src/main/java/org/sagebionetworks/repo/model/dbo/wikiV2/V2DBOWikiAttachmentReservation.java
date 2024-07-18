package org.sagebionetworks.repo.model.dbo.wikiV2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_WIKI_ATTATCHMENT_RESERVATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.V2_TABLE_WIKI_ATTACHMENT_RESERVATION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class V2DBOWikiAttachmentReservation implements MigratableDatabaseObject<V2DBOWikiAttachmentReservation, V2DBOWikiAttachmentReservation> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("wikiId", V2_COL_WIKI_ATTACHMENT_RESERVATION_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("fileHandleId", V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID).withIsPrimaryKey(true),
			new FieldColumn("timeStamp", V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP)
	};

	private Long wikiId;
	private Long fileHandleId;
	private Timestamp timeStamp;
	
	@Override
	public TableMapping<V2DBOWikiAttachmentReservation> getTableMapping() {
		return new TableMapping<V2DBOWikiAttachmentReservation>() {
			
			@Override
			public V2DBOWikiAttachmentReservation mapRow(ResultSet rs, int rowNum) throws SQLException {
				V2DBOWikiAttachmentReservation dbo  = new V2DBOWikiAttachmentReservation();
				dbo.setWikiId(rs.getLong(V2_COL_WIKI_ATTACHMENT_RESERVATION_ID));
				dbo.setFileHandleId(rs.getLong(V2_COL_WIKI_ATTACHMENT_RESERVATION_FILE_HANDLE_ID));
				dbo.setTimeStamp(rs.getTimestamp(V2_COL_WIKI_ATTACHMENT_RESERVATION_TIMESTAMP));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return V2_TABLE_WIKI_ATTACHMENT_RESERVATION;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_WIKI_ATTATCHMENT_RESERVATION;
			}
			
			@Override
			public Class<? extends V2DBOWikiAttachmentReservation> getDBOClass() {
				return V2DBOWikiAttachmentReservation.class;
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

	public Timestamp getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Timestamp timeStamp) {
		this.timeStamp = timeStamp;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileHandleId, timeStamp, wikiId);
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
		return Objects.equals(fileHandleId, other.fileHandleId) && Objects.equals(timeStamp, other.timeStamp)
				&& Objects.equals(wikiId, other.wikiId);
	}

	@Override
	public String toString() {
		return "V2DBOWikiAttachmentReservation [wikiId=" + wikiId + ", fileHandleId=" + fileHandleId + ", timeStamp="
				+ timeStamp + "]";
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
