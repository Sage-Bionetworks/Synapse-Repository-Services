package org.sagebionetworks.table.cluster.columntranslation;

import java.util.Arrays;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableConstants;

/**
 * ColumnTranslationReference for row metadata columns. For these, the
 * userQueryColumnName and translatedColumnName are the same
 */
public enum RowMetadataColumnTranslationReference {
	ROW_ID(TableConstants.ROW_ID, ColumnType.INTEGER, null),
	ROW_VERSION(TableConstants.ROW_VERSION, ColumnType.INTEGER, null),
	ROW_ETAG(TableConstants.ROW_ETAG, ColumnType.STRING, 36L),
	ROW_BENEFACTOR(TableConstants.ROW_BENEFACTOR, ColumnType.INTEGER, null);

	// the translated name and queried name are the same
	private final String columnName;
	private final ColumnType columnType;
	private final Long maximumSize;

	RowMetadataColumnTranslationReference(final String columnName, final ColumnType columnType, Long maximumSize) {
		this.columnName = columnName.toUpperCase();
		this.columnType = columnType;
		this.maximumSize = maximumSize;
	}
	
	public ColumnTranslationReference getColumnTranslationReference() {
		return new RowMetadataReferenceWrapper(this.columnName, this);
	}

	public ColumnType getColumnType() {
		return columnType;
	}

//	public String getUserQueryColumnName() {
//		return columnName;
//	}
//
//	public String getTranslatedColumnName() {
//		return columnName;
//	}

	public Long getMaximumSize() {
		return maximumSize;
	}
	
	/**
	 * Attempt to match the given right-hand-side with one of the columns of this
	 * enumeration.
	 * 
	 * @param rhs
	 * @return
	 */
	public static Optional<ColumnTranslationReference> lookupColumnReference(String rhs) {
		if(rhs.toUpperCase().startsWith(TableConstants.ROW_BENEFACTOR)) {
			return Optional.of(new RowMetadataReferenceWrapper(rhs.toUpperCase(), ROW_BENEFACTOR));
		}
		return Arrays.stream(RowMetadataColumnTranslationReference.values())
				.filter(r -> rhs.equalsIgnoreCase(r.columnName)).findFirst().map(r -> r.getColumnTranslationReference());
	}

}
