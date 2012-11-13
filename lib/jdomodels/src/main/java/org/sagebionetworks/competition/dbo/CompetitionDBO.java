package org.sagebionetworks.competition.dbo;

import static org.sagebionetworks.competition.query.jdo.SQLConstants.*;
import static org.sagebionetworks.competition.dbo.DBOConstants.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.model.TaggableEntity;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * The database object for a Synapse Competition
 * 
 * @author bkng
 */
public class CompetitionDBO implements DatabaseObject<CompetitionDBO>, TaggableEntity {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(PARAM_COMPETITION_ID, COL_COMPETITION_ID, true),
			new FieldColumn(PARAM_COMPETITION_ETAG, COL_COMPETITION_ETAG),
			new FieldColumn(PARAM_COMPETITION_NAME, COL_COMPETITION_NAME),
			new FieldColumn(PARAM_COMPETITION_DESCRIPTION, COL_COMPETITION_DESCRIPTION),
			new FieldColumn(PARAM_COMPETITION_OWNER_ID, COL_COMPETITION_OWNER_ID),
			new FieldColumn(PARAM_COMPETITION_CREATED_ON, COL_COMPETITION_CREATED_ON),
			new FieldColumn(PARAM_COMPETITION_CONTENT_SOURCE, COL_COMPETITION_CONTENT_SOURCE),
			new FieldColumn(PARAM_COMPETITION_STATUS, COL_COMPETITION_STATUS)
			};

	public TableMapping<CompetitionDBO> getTableMapping() {
		return new TableMapping<CompetitionDBO>() {
			// Map a result set to this object
			public CompetitionDBO mapRow(ResultSet rs, int rowNum)	throws SQLException {
				CompetitionDBO comp = new CompetitionDBO();
				comp.setId(rs.getLong(COL_COMPETITION_ID));
				comp.seteTag(rs.getString(COL_COMPETITION_ETAG));
				comp.setName(rs.getString(COL_COMPETITION_NAME));
				java.sql.Blob blob = rs.getBlob(COL_COMPETITION_DESCRIPTION);
				if(blob != null){
					comp.setDescription(blob.getBytes(1, (int) blob.length()));
				}
				comp.setOwnerId(rs.getLong(COL_COMPETITION_OWNER_ID));
				comp.setCreatedOn(rs.getLong(COL_COMPETITION_CREATED_ON));
				comp.setContentSource(rs.getString(COL_COMPETITION_CONTENT_SOURCE));
				comp.setStatus(rs.getInt(COL_COMPETITION_STATUS));
				return comp;
			}

			public String getTableName() {
				return TABLE_COMPETITION;
			}

			public String getDDLFileName() {
				return DDL_FILE_COMPETITION;
			}

			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			public Class<? extends CompetitionDBO> getDBOClass() {
				return CompetitionDBO.class;
			}
		};
	}
	
	private Long id;
	private String eTag;
	private String name;
	private byte[] description;
	private Long ownerId;
	private Long createdOn;
	private String contentSource;
	private int status;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String geteTag() {
		return eTag;
	}
	public void seteTag(String eTag) {
		this.eTag = eTag;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public byte[] getDescription() {
		return description;
	}
	public void setDescription(byte[] description) {
		this.description = description;
	}

	public Long getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public String getContentSource() {
		return contentSource;
	}
	public void setContentSource(String contentSource) {
		this.contentSource = contentSource;
	}

	public int getStatus() {
		return status;
	}
	private void setStatus(int status) {
		this.status = status;
	}
	
	public CompetitionStatus getStatusEnum() {
		return CompetitionStatus.values()[status];
	}
	public void setStatusEnum(CompetitionStatus cs) {
		if (cs == null)	throw new IllegalArgumentException("Competition status cannot be null");
		setStatus(cs.ordinal());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contentSource == null) ? 0 : contentSource.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + Arrays.hashCode(description);
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
		result = prime * result + status;
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
		CompetitionDBO other = (CompetitionDBO) obj;
		if (contentSource == null) {
			if (other.contentSource != null)
				return false;
		} else if (!contentSource.equals(other.contentSource))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (!Arrays.equals(description, other.description))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (ownerId == null) {
			if (other.ownerId != null)
				return false;
		} else if (!ownerId.equals(other.ownerId))
			return false;
		if (status != other.status)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOCompetition [id=" + id + ", name=" + name + ", eTag=" + eTag 
				+ ", description=" + description + ", ownerId=" + ownerId 
				+ ", createdOn=" + createdOn + ", contentSource=" 
				+ contentSource + ", status=" + CompetitionStatus.values()[status].toString() + "]";
	}

}
