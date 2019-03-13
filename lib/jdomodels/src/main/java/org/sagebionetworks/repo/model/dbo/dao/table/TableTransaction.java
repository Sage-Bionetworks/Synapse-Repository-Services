package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Date;

/**
 * Simple DTO for TableTransaction information.
 * 
 *
 */
public class TableTransaction {
	
	Long transactionNumber;
	String tableId;
	Long startedBy;
	Date startedOn;
	
	public Long getTransactionNumber() {
		return transactionNumber;
	}
	public TableTransaction withTransactionNumber(Long transactionNumber) {
		this.transactionNumber = transactionNumber;
		return this;
	}
	public String getTableId() {
		return tableId;
	}
	public TableTransaction withTableId(String tableId) {
		this.tableId = tableId;
		return this;
	}
	public Long getStartedBy() {
		return startedBy;
	}
	public TableTransaction withStartedBy(Long startedBy) {
		this.startedBy = startedBy;
		return this;
	}
	public Date getStartedOn() {
		return startedOn;
	}
	public TableTransaction withStartedOn(Date startedOn) {
		this.startedOn = startedOn;
		return this;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((startedBy == null) ? 0 : startedBy.hashCode());
		result = prime * result + ((startedOn == null) ? 0 : startedOn.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
		result = prime * result + ((transactionNumber == null) ? 0 : transactionNumber.hashCode());
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
		TableTransaction other = (TableTransaction) obj;
		if (startedBy == null) {
			if (other.startedBy != null)
				return false;
		} else if (!startedBy.equals(other.startedBy))
			return false;
		if (startedOn == null) {
			if (other.startedOn != null)
				return false;
		} else if (!startedOn.equals(other.startedOn))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		if (transactionNumber == null) {
			if (other.transactionNumber != null)
				return false;
		} else if (!transactionNumber.equals(other.transactionNumber))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "TableTransaction [transactionNumber=" + transactionNumber + ", tableId=" + tableId + ", startedBy="
				+ startedBy + ", startedOn=" + startedOn + "]";
	}

}
