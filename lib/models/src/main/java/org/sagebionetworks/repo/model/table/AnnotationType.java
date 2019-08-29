package org.sagebionetworks.repo.model.table;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Value;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;
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
	
	STRING	(new StringParser(), ColumnType.STRING, AnnotationsV2ValueType.STRING),
	LONG	(new LongParser(), ColumnType.INTEGER, AnnotationsV2ValueType.LONG),
	DOUBLE	(new DoubleParser(), ColumnType.DOUBLE, AnnotationsV2ValueType.DOUBLE),
	DATE	(new DateParser(), ColumnType.DATE, AnnotationsV2ValueType.TIMESTAMP_MS);
	
	AnnotationType(ValueParser parser, ColumnType columnType, AnnotationsV2ValueType annotationsV2ValueType){
		this.parser = parser;
		this.columnType = columnType;
		this.annotationsV2ValueType = annotationsV2ValueType;
	}
	
	ValueParser parser;
	ColumnType columnType;
	AnnotationsV2ValueType annotationsV2ValueType;
	
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

	public static AnnotationType forAnnotationV2Type(AnnotationsV2ValueType v2ValueType){
		for(AnnotationType annotationType: values()){
			if (annotationType.annotationsV2ValueType == v2ValueType){
				return annotationType;
			}
		}
		throw new IllegalArgumentException("unexpected AnnotationsV2ValueType");
	}
}