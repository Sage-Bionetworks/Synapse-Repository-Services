package org.sagebionetworks.audit.dao;

import java.util.List;

import org.sagebionetworks.repo.model.audit.ObjectRecord;

/**
 * Batch of ObjectRecords.
 * 
 */
public class ObjectRecordBatch {

	String type;
	List<ObjectRecord> records;

	/**
	 * 
	 * @param records
	 *            The object records to be written.
	 * @param type
	 *            The type of the synapse object that is going to be written
	 */
	public ObjectRecordBatch(List<ObjectRecord> records, String type) {
		super();
		this.type = type;
		this.records = records;
	}

	/**
	 * The type of the synapse object that is going to be written.
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * The object records to be written.
	 * 
	 * @return
	 */
	public List<ObjectRecord> getRecords() {
		return records;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((records == null) ? 0 : records.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		ObjectRecordBatch other = (ObjectRecordBatch) obj;
		if (records == null) {
			if (other.records != null)
				return false;
		} else if (!records.equals(other.records))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
