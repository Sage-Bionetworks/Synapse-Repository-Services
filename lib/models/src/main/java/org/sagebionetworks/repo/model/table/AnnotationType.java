package org.sagebionetworks.repo.model.table;

/**
 * 
 * Enumeration of the currently supported annotation types.
 *
 */
public enum AnnotationType{
	
	STRING	(ValueParsers.STRING_PARSER),
	LONG	(ValueParsers.LONG_PARSER),
	DOUBLE	(ValueParsers.DOUBLE_PARSER),
	DATE	(ValueParsers.DATE_PARSER);
	
	AnnotationType(ValueParser parser){
		this.parser = parser;
	}
	
	ValueParser parser;
	
	/**
	 * Parse the given string value into an object of the correct type.
	 * @param value
	 * @return
	 */
	public Object parseValue(String value){
		return parser.parseValue(value);
	}
}