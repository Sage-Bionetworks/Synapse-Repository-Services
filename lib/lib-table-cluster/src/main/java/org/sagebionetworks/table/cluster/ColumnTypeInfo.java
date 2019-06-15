package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.table.cluster.utils.ColumnConstants.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ValueParser;
import org.sagebionetworks.repo.model.table.parser.BooleanParser;
import org.sagebionetworks.repo.model.table.parser.DateToLongParser;
import org.sagebionetworks.repo.model.table.parser.DoubleParser;
import org.sagebionetworks.repo.model.table.parser.EntityIdParser;
import org.sagebionetworks.repo.model.table.parser.LongParser;
import org.sagebionetworks.repo.model.table.parser.StringParser;

/**
 * Mapping of ColumnType to database information.
 *
 */
public enum ColumnTypeInfo {
	
	INTEGER		(ColumnType.INTEGER, 		MySqlColumnType.BIGINT,		new LongParser(),			20L),
	FILEHANDLEID(ColumnType.FILEHANDLEID,	MySqlColumnType.BIGINT, 	new LongParser(),			20L),
	DATE		(ColumnType.DATE,			MySqlColumnType.BIGINT,		new DateToLongParser(),		20L),
	ENTITYID	(ColumnType.ENTITYID,		MySqlColumnType.BIGINT,		new EntityIdParser(),		20L),
	LINK		(ColumnType.LINK,			MySqlColumnType.VARCHAR,	new StringParser(),			null),
	STRING		(ColumnType.STRING,			MySqlColumnType.VARCHAR,	new StringParser(),			null),
	DOUBLE		(ColumnType.DOUBLE,			MySqlColumnType.DOUBLE,		new DoubleParser(),			null),
	BOOLEAN		(ColumnType.BOOLEAN,		MySqlColumnType.BOOLEAN,	new BooleanParser(),		null),
	LARGETEXT	(ColumnType.LARGETEXT,		MySqlColumnType.MEDIUMTEXT,	new StringParser(),			null),
	USERID		(ColumnType.USERID,			MySqlColumnType.BIGINT, 	new LongParser(),			20L);
	
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
	 * @param inputSize
	 * @param defaultValue
	 * @param useDepricatedUtf8ThreeBytes Should only be set to true for the few old
	 * tables that are too large to build with the correct 4 byte UTF-8.
	 * @return
	 */
	public String toSql(Long inputSize, String defaultValue, boolean useDepricatedUtf8ThreeBytes){
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
		if(size != null && mySqlType.hasSize()){
			builder.append("(");
			builder.append(size);
			builder.append(")");
		}
		if(isStringType()){
			builder.append(" ");
			if(useDepricatedUtf8ThreeBytes) {
				// This is a special case to support old large tables with UTF-8 3 bytes.
				builder.append(DEPREICATED_THREE_BYTE_UTF8);
			}else {
				builder.append(CHARACTER_SET_UTF8_COLLATE_UTF8_GENERAL_CI);
			}
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
	public Object parseValueForDatabaseWrite(String value){
		if(value == null) {
			return null;
		}
		return parser.parseValueForDatabaseWrite(value);
	}
	
	/**
	 * Parser the value for a database read.
	 * 
	 * @param value
	 * @return
	 */
	public String parseValueForDatabaseRead(String value) {
		if(value == null){
			return null;
		}
		return parser.parseValueForDatabaseRead(value);
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
			// escape single quotes
			// NOTE: This originally used StringEscapeUtils.escapeSql() which only ever escaped single quotes and has been removed in later versions.
			// https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/StringEscapeUtils.html#escapeSql(java.lang.String)
			// https://stackoverflow.com/questions/32096614/migrating-stringescapeutils-escapesql-from-commons-lang
			defaultValue = StringUtils.replace(defaultValue, "'", "''");
			// Validate the default can be applied.
			Object objectValue = parseValueForDatabaseWrite(defaultValue);
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
