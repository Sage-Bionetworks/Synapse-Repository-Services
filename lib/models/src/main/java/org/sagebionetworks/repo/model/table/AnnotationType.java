package org.sagebionetworks.repo.model.table;

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
	
	STRING	(ColumnType.STRING, AnnotationsV2ValueType.STRING),
	LONG	(ColumnType.INTEGER, AnnotationsV2ValueType.LONG),
	DOUBLE	(ColumnType.DOUBLE, AnnotationsV2ValueType.DOUBLE),
	DATE	(ColumnType.DATE, AnnotationsV2ValueType.TIMESTAMP_MS);
	
	AnnotationType(ColumnType columnType, AnnotationsV2ValueType annotationsV2ValueType){
		this.columnType = columnType;
		this.annotationsV2ValueType = annotationsV2ValueType;
	}

	ColumnType columnType;
	AnnotationsV2ValueType annotationsV2ValueType;

	/**
	 * Get the column type mapped to this annotation type.
	 * @return
	 */
	public ColumnType getColumnType(){
		return columnType;
	}

	public AnnotationsV2ValueType getAnnotationsV2ValueType() {
		return annotationsV2ValueType;
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