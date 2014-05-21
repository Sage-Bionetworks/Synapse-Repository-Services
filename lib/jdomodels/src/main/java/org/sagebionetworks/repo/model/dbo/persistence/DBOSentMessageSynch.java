package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.Date;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * This table is used to keep track of the last change number the that was synchronized between the sent messages and changes.
 * @author John
 *
 */
@Table(name = TABLE_SENT_MESSAGES_SYNCH)
public class DBOSentMessageSynch implements DatabaseObject<DBOSentMessageSynch>{
	
	/**
	 * This is a table with only a single row with this id.
	 */
	public static final Long THE_ID = new Long(1);
	/**
	 * We start with this number.
	 */
	public static final Long DEFAULT_START_CHANGE_NUMBER = new Long(-1);

	private static TableMapping<DBOSentMessageSynch> tableMapping = AutoTableMapping.create(DBOSentMessageSynch.class);
	
	@Field(name=COL_SENT_MESSAGE_SYCH_ID, primary=true)
	private Long id;
	/**
	 * The last change number the that was synchronized between the sent messages and changes.
	 */
	@Field(name=COL_SENT_MESSAGE_SYCH_LAST_CHANGE_NUMBER, nullable=false)
	private Long lastChangeNumber;
	
	@Field(name=COL_SENT_MESSAGE_SYCH_CHANGED_ON, nullable=false)
	private Date updatedOn;
	
	@Override
	public TableMapping<DBOSentMessageSynch> getTableMapping() {
		return tableMapping;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getLastChangeNumber() {
		return lastChangeNumber;
	}

	public void setLastChangeNumber(Long lastChangeNumber) {
		this.lastChangeNumber = lastChangeNumber;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime
				* result
				+ ((lastChangeNumber == null) ? 0 : lastChangeNumber.hashCode());
		result = prime * result
				+ ((updatedOn == null) ? 0 : updatedOn.hashCode());
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
		DBOSentMessageSynch other = (DBOSentMessageSynch) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (lastChangeNumber == null) {
			if (other.lastChangeNumber != null)
				return false;
		} else if (!lastChangeNumber.equals(other.lastChangeNumber))
			return false;
		if (updatedOn == null) {
			if (other.updatedOn != null)
				return false;
		} else if (!updatedOn.equals(other.updatedOn))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOSentMessageSynch [id=" + id + ", lastChangeNumber="
				+ lastChangeNumber + ", updatedOn=" + updatedOn + "]";
	}

}
