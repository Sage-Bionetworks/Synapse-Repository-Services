package org.sagebionetworks.table.query.util;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Registry of ColumnTypes that are treated as Lists.
 * Maps List ColumnTypes and their non-list counterparts.
 */
public enum ColumnTypeListMappings {
	STRING(ColumnType.STRING, ColumnType.STRING_LIST),
	INTEGER(ColumnType.INTEGER, ColumnType.INTEGER_LIST),
	BOOLEAN(ColumnType.BOOLEAN, ColumnType.BOOLEAN_LIST),
	DATE(ColumnType.DATE, ColumnType.DATE_LIST),
	ENTITYID(ColumnType.ENTITYID, ColumnType.ENTITYID_LIST),
	USERID(ColumnType.USERID, ColumnType.USERID_LIST);

	ColumnType nonListType;
	ColumnType listType;

	ColumnTypeListMappings(ColumnType nonListType, ColumnType listType){
		this.nonListType = nonListType;
		this.listType = listType;
	}

	public ColumnType getNonListType() {
		return nonListType;
	}

	public ColumnType getListType() {
		return listType;
	}

	public static ColumnTypeListMappings forListType(ColumnType listType){
		for(ColumnTypeListMappings mappings : values()){
			if(mappings.getListType() == listType){
				return mappings;
			}
		}
		throw new IllegalArgumentException(listType + " is not a list ColumnType");
	}

	public static ColumnTypeListMappings forNonListType(ColumnType nonListType){
		for(ColumnTypeListMappings mappings : values()){
			if(mappings.getNonListType() == nonListType){
				return mappings;
			}
		}
		throw new IllegalArgumentException(nonListType + " is not a ColumnType that has a list type associated with it");
	}

	public static ColumnType nonListType(ColumnType listType){
		return forListType(listType).getNonListType();
	}

	public static ColumnType listType(ColumnType nonListType){
		return forNonListType(nonListType).getListType();
	}

	public static boolean isList(ColumnType columnType){
		try{
			forListType(columnType);
			return true;
		} catch (IllegalArgumentException e){
			return false;
		}
	}
}
