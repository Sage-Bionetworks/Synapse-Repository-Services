package org.sagebionetworks.table.cluster.columntranslation;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;

/**
 * Wraps a @RowMetadataColumnTranslationReference with a custom name.
 *
 */
public class RowMetadataReferenceWrapper implements ColumnTranslationReference {

	private final RowMetadataColumnTranslationReference enumRef;
	private final String columnName;
	
	public RowMetadataReferenceWrapper(String columnName, RowMetadataColumnTranslationReference enumRef) {
		this.columnName = columnName;
		this.enumRef = enumRef;
	}
	@Override
	public ColumnType getColumnType() {
		return enumRef.getColumnType();
	}

	@Override
	public String getUserQueryColumnName() {
		return columnName;
	}

	@Override
	public String getTranslatedColumnName() {
		return columnName;
	}

	@Override
	public Long getMaximumSize() {
		return enumRef.getMaximumSize();
	}

	@Override
	public Long getMaximumListLength() {
		return null;
	}

	@Override
	public FacetType getFacetType() {
		return null;
	}

	@Override
	public String getDefaultValues() {
		return null;
	}
	@Override
	public int hashCode() {
		return Objects.hash(columnName, enumRef);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RowMetadataReferenceWrapper)) {
			return false;
		}
		RowMetadataReferenceWrapper other = (RowMetadataReferenceWrapper) obj;
		return Objects.equals(columnName, other.columnName) && enumRef == other.enumRef;
	}
	@Override
	public String toString() {
		return "RowMetadataReferenceImpl [enumRef=" + enumRef + ", columnName=" + columnName + "]";
	}

	
}
