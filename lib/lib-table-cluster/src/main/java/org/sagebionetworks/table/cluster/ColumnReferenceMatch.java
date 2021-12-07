package org.sagebionetworks.table.cluster;

import java.util.Objects;

import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;

public class ColumnReferenceMatch {

	private final TableInfo tableInfo;
	private final ColumnTranslationReference columnTranslationReference;
	
	public ColumnReferenceMatch(TableInfo tableInfo, ColumnTranslationReference columnTranslationReference) {
		super();
		this.tableInfo = tableInfo;
		this.columnTranslationReference = columnTranslationReference;
	}

	/**
	 * @return the tableInfo
	 */
	public TableInfo getTableInfo() {
		return tableInfo;
	}

	/**
	 * @return the columnTranslationReference
	 */
	public ColumnTranslationReference getColumnTranslationReference() {
		return columnTranslationReference;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnTranslationReference, tableInfo);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ColumnReferenceMatch)) {
			return false;
		}
		ColumnReferenceMatch other = (ColumnReferenceMatch) obj;
		return Objects.equals(columnTranslationReference, other.columnTranslationReference)
				&& Objects.equals(tableInfo, other.tableInfo);
	}
	
	
}
