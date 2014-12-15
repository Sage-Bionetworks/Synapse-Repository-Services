package org.sagebionetworks.repo.model.table;

import java.util.List;

/**
 * Represents a set of raw row of a TableEntity
 * 
 */
public class RawRowSet {
	private final List<String> headers;
	private final String etag;
	private final String tableId;
	private final List<Row> rows;

	public RawRowSet(List<String> headers, String etag, String tableId, List<Row> rows) {
		this.headers = headers;
		this.etag = etag;
		this.tableId = tableId;
		this.rows = rows;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public String getEtag() {
		return etag;
	}

	public String getTableId() {
		return tableId;
	}

	public List<Row> getRows() {
		return rows;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
		result = prime * result + ((headers == null) ? 0 : headers.hashCode());
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
		result = prime * result + ((tableId == null) ? 0 : tableId.hashCode());
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
		RawRowSet other = (RawRowSet) obj;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
			return false;
		if (headers == null) {
			if (other.headers != null)
				return false;
		} else if (!headers.equals(other.headers))
			return false;
		if (rows == null) {
			if (other.rows != null)
				return false;
		} else if (!rows.equals(other.rows))
			return false;
		if (tableId == null) {
			if (other.tableId != null)
				return false;
		} else if (!tableId.equals(other.tableId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RawRowSet [headers=" + headers + ", etag=" + etag + ", tableId=" + tableId + ", rows=" + rows + "]";
}

}