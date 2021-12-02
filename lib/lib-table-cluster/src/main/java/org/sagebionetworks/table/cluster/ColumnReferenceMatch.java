package org.sagebionetworks.table.cluster;

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
	
	
}
