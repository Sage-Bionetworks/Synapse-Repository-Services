package org.sagebionetworks.repo.model.table;

import org.sagebionetworks.repo.model.table.parser.DateParser;
import org.sagebionetworks.repo.model.table.parser.DoubleParser;
import org.sagebionetworks.repo.model.table.parser.LongParser;
import org.sagebionetworks.repo.model.table.parser.StringParser;

/**
 * 
 * Enumeration of the currently supported annotation types.
 *
 */
public enum AnnotationType{
	
	STRING	(new StringParser(), ColumnType.STRING),
	LONG	(new LongParser(), ColumnType.INTEGER),
	DOUBLE	(new DoubleParser(), ColumnType.DOUBLE),
	DATE	(new DateParser(), ColumnType.DATE);
	
	AnnotationType(ValueParser parser, ColumnType columnType){
		this.parser = parser;
		this.columnType = columnType;
	}
	
	ValueParser parser;
	ColumnType columnType;
	
	/**
	 * Parse the given string value into an object of the correct type.
	 * @param value
	 * @return
	 */
	public Object parseValue(String value){
		return parser.parseValueForDatabaseWrite(value);
	}
	
	/**
	 * Get the column type mapped to this annotation type.
	 * @return
	 */
	public ColumnType getColumnType(){
		return columnType;
	}
}