package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.table.cluster.utils.ColumnConstants.CHARACTER_SET_UTF8_COLLATE_UTF8_GENERAL_CI;

import org.apache.commons.lang.StringEscapeUtils;
import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Mapping of ColumnType to database information.
 *
 */
public enum ColumnTypeInfo {
	
	INTEGER		(ColumnType.INTEGER, 		MySqlColumnType.BIGINT,		ValueParsers.LONG_PARSER,		20L),
	FILEHANDLEID(ColumnType.FILEHANDLEID,	MySqlColumnType.BIGINT, 	ValueParsers.LONG_PARSER,		20L),
	DATE		(ColumnType.DATE,			MySqlColumnType.BIGINT,		ValueParsers.DATE_PARSER,		20L),
	ENTITYID	(ColumnType.ENTITYID,		MySqlColumnType.VARCHAR,	ValueParsers.STRING_PARSER,		44L),
	LINK		(ColumnType.LINK,			MySqlColumnType.VARCHAR,	ValueParsers.STRING_PARSER,		null),
	STRING		(ColumnType.STRING,			MySqlColumnType.VARCHAR,	ValueParsers.STRING_PARSER,		null),
	DOUBLE		(ColumnType.DOUBLE,			MySqlColumnType.DOUBLE,		ValueParsers.DOUBLE_PARSER,		null),
	BOOLEAN		(ColumnType.BOOLEAN,		MySqlColumnType.BOOLEAN,	ValueParsers.BOOLEAN_PARSER,	null),
	LARGETEXT	(ColumnType.LARGETEXT,		MySqlColumnType.MEDIUMTEXT,	ValueParsers.STRING_PARSER,		null);
	
	private ColumnType type;
	private MySqlColumnType mySqlType;
	private Long maxSize;
	private ValueParser parser;
	
	private ColumnTypeInfo(ColumnType type, MySqlColumnType mySqlType, ValueParser parser, Long maxSize){
		this.type = type;
		this.mySqlType = mySqlType;
		this.maxSize = maxSize;
		this.parser = parser;
	}
	
	/**
	 * Get the SQL to define a column of this type in MySQL.
	 * @return
	 */
	public String toSql(Long inputSize, String defaultValue){
		StringBuilder builder = new StringBuilder();
		builder.append(mySqlType.name());
		Long size = maxSize;
		if(inputSize == null && requiresInputMaxSize()){
			throw new IllegalArgumentException("Size must be provided for type: "+type);
		}
		// use the input size if given
		if(inputSize != null){
			size = inputSize;
		}
		if(size != null){
			builder.append("(");
			builder.append(size);
			builder.append(")");
		}
		if(isStringType()){
			builder.append(" ");
			builder.append(CHARACTER_SET_UTF8_COLLATE_UTF8_GENERAL_CI);
		}
		// default
		builder.append(" ");
		appendDefaultValue(builder, defaultValue);
		// Add the column type as a comment
		builder.append(" COMMENT '").append(this.type.name()).append("'");
		return builder.toString();
	}
	
	/**
	 * Is the type a string type?
	 * 
	 * @return
	 */
	public boolean isStringType(){
		return (MySqlColumnType.VARCHAR.equals(mySqlType) || MySqlColumnType.MEDIUMTEXT.equals(mySqlType));
	}
	
	/**
	 * Does this type require an input maximum size?
	 * 
	 * @return
	 */
	public boolean requiresInputMaxSize(){
		return (ColumnType.STRING.equals(type) || ColumnType.LINK.equals(type));
	}
	
	/**
	 * Lookup the ColumnTypeInfo for a given type.
	 * @param type
	 * @return
	 */
	public static ColumnTypeInfo getInfoForType(ColumnType type){
		for(ColumnTypeInfo info: ColumnTypeInfo.values()){
			if(info.type.equals(type)){
				return info;
			}
		}
		throw new IllegalArgumentException("Unknown ColumnType: "+type);
	}
	
	/**
	 * Parse the given value into an object that can be inserted into the database.
	 * 
	 * @param value
	 * @return
	 */
	public Object parseValueForDB(String value){
		if(value == null) {
			return null;
		}
		return parser.parseValue(value);
	}
	
	/**
	 * Append the a default value for this type to the passed builder.
	 * 
	 * @param builder
	 * @param defalutValue
	 */
	public void appendDefaultValue(StringBuilder builder, String defaultValue){
		builder.append("DEFAULT ");
		if(defaultValue == null){
			builder.append("NULL");
		}else{
			// Block SQL injections
			defaultValue = StringEscapeUtils.escapeSql(defaultValue);
			// Validate the default can be applied.
			Object objectValue = parseValueForDB(defaultValue);
			if(isStringType()){
				builder.append("'");
			}
			builder.append(objectValue.toString());
			if(isStringType()){
				builder.append("'");
			}
		}
	}

	/**
	 * The ColumnType for this info.
	 * @return
	 */
	public ColumnType getType() {
		return type;
	}

	/**
	 * The MySqlColumnType for this info.
	 * @return
	 */
	public MySqlColumnType getMySqlType() {
		return mySqlType;
	}

	/**
	 * Get default maximum size for this info.
	 * @return
	 */
	public Long getMaxSize() {
		return maxSize;
	}

	/**
	 * The value parser used by this info.
	 * @return
	 */
	public ValueParser getParser() {
		return parser;
	}

}
