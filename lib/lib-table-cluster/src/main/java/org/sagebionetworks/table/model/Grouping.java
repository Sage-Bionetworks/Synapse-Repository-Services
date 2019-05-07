package org.sagebionetworks.table.model;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * A grouping of all rows that have values that exist for the same columns of a
 * change set.
 *
 */
public class Grouping {
	
	List<ColumnModel> columns;
	List<SparseRow> rows;
	
	public Grouping(List<ColumnModel> columns, List<SparseRow> rows) {
		super();
		this.columns = columns;
		this.rows = rows;
	}

	/**
	 * Get the ColumnModels that all rows within this grouping have valid values
	 * for.
	 * 
	 * @return
	 */
	public List<ColumnModel> getColumnsWithValues(){
		return columns;
	}

	/**
	 * Get all of the rows within this grouping.
	 * 
	 * @return
	 */
	public List<SparseRow> getRows(){
		return rows;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
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
		Grouping other = (Grouping) obj;
		if (columns == null) {
			if (other.columns != null)
				return false;
		} else if (!columns.equals(other.columns))
			return false;
		if (rows == null) {
			if (other.rows != null)
				return false;
		} else if (!rows.equals(other.rows))
			return false;
		return true;
	}

}
