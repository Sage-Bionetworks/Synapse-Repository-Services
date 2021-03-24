package org.sagebionetworks.repo.model.table;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

/**
 * 
 * Enumeration of the currently supported annotation types.
 *
 */
public enum AnnotationType{
	STRING	(ColumnType.STRING, AnnotationsValueType.STRING),
	LONG	(ColumnType.INTEGER, AnnotationsValueType.LONG),
	DOUBLE	(ColumnType.DOUBLE, AnnotationsValueType.DOUBLE),
	DATE	(ColumnType.DATE, AnnotationsValueType.TIMESTAMP_MS),
	BOOLEAN	(ColumnType.BOOLEAN, AnnotationsValueType.BOOLEAN);
	
	AnnotationType(ColumnType columnType, AnnotationsValueType annotationsV2ValueType){
		this.columnType = columnType;
		this.annotationsV2ValueType = annotationsV2ValueType;
	}

	ColumnType columnType;
	AnnotationsValueType annotationsV2ValueType;

	/**
	 * Get the column type mapped to this annotation type.
	 * @return
	 */
	public ColumnType getColumnType(){
		return columnType;
	}

	public AnnotationsValueType getAnnotationsV2ValueType() {
		return annotationsV2ValueType;
	}

	public static AnnotationType forAnnotationV2Type(AnnotationsValueType v2ValueType){
		for(AnnotationType annotationType: values()){
			if (annotationType.annotationsV2ValueType == v2ValueType){
				return annotationType;
			}
		}
		throw new IllegalArgumentException("unexpected AnnotationsV2ValueType");
	}
}