package org.sagebionetworks.table.cluster;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Registry of ColumnTypes that are treated as Lists.
 * Maps List ColumnTypes and their non-list counterparts.
 */
public enum ColumnTypeListMappings {
	STRING(ColumnType.STRING, ColumnType.STRING_LIST),
	INTEGER(ColumnType.INTEGER, ColumnType.INTEGER_LIST),
	BOOLEAN(ColumnType.BOOLEAN, ColumnType.BOOLEAN_LIST),
	DATE(ColumnType.DATE, ColumnType.DATE_LIST);

	private static final Map<ColumnType, ColumnTypeListMappings> LIST_TO_NON_LIST = Arrays.stream(values())
			.collect(Collectors.toMap(
						ColumnTypeListMappings::getListType, //key
						Function.identity(), //value
						(key, val) -> {throw new IllegalArgumentException("duplicate key " + key);}	, //duplicate key handler
						() -> new EnumMap<>(ColumnType.class) //map implementation supplier
					));

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
		ColumnTypeListMappings nonListType = LIST_TO_NON_LIST.get(listType);
		if (nonListType == null) {
			throw new IllegalArgumentException(listType + " is not a List ColumnType");
		}
		return nonListType;
	}

	public static ColumnType nonListType(ColumnType listType){
		return forListType(listType).getNonListType();
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
