package org.sagebionetworks.table.cluster;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Registry of ColumnTypes that are treated as Lists.
 * Maps List ColumnTypes and their non-list counterparts.
 */
public enum ColumnTypeListMappings {
	STRING(ColumnType.STRING, ColumnType.STRING_LIST),
	DOUBLE(ColumnType.DOUBLE, ColumnType.DOUBLE_LIST),
	INTEGER(ColumnType.INTEGER, ColumnType.INTEGER_LIST),
	BOOLEAN(ColumnType.BOOLEAN, ColumnType.BOOLEAN_LIST),
	DATE(ColumnType.DATE, ColumnType.DATE_LIST);


	ColumnType nonListType;
	ColumnType listType;

	ColumnTypeListMappings(ColumnType nonListType, ColumnType listType){
		this.nonListType = nonListType;
		this.listType = listType;
	}

	public static boolean isList(ColumnType columnType){
		for(ColumnTypeListMappings mapping : values()){
			if(mapping.listType == columnType){
				return true;
			}
		}
		return false;
	}
}
