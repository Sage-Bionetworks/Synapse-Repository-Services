package org.sagebionetworks.repo.model.table;

/**
 * 
 * Enumeration of the currently supported annotation types.
 *
 */
public enum AnnotationType{
	
	STRING	(ValueParsers.STRING_PARSER, ColumnType.STRING),
	LONG	(ValueParsers.LONG_PARSER, ColumnType.INTEGER),
	DOUBLE	(ValueParsers.DOUBLE_PARSER, ColumnType.DOUBLE),
	DATE	(ValueParsers.DATE_PARSER, ColumnType.DATE);
	
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
		return parser.parseValue(value);
	}
	
	/**
	 * Get the column type mapped to this annotation type.
	 * @return
	 */
	public ColumnType getColumnType(){
		return columnType;
	}
}