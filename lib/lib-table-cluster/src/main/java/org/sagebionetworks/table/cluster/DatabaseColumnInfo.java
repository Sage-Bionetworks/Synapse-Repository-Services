package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.ROW_BENEFACTOR;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.util.Comparator;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.utils.ColumnConstants;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Information about a column in the database.
 *
 */
public class DatabaseColumnInfo {
	
	String columnName;
	boolean hasIndex;
	MySqlColumnType type;
	Integer maxSize;
	Long cardinality;
	String indexName;
	ColumnType columnType;
	
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public boolean hasIndex() {
		return hasIndex;
	}
	public void setHasIndex(boolean hasIndex) {
		this.hasIndex = hasIndex;
	}
	public Long getCardinality() {
		return cardinality;
	}
	public void setCardinality(Long cardinality) {
		this.cardinality = cardinality;
	}
	public String getIndexName() {
		return indexName;
	}
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
	public MySqlColumnType getType() {
		return type;
	}
	public void setType(MySqlColumnType type) {
		this.type = type;
	}
	public Integer getMaxSize() {
		return maxSize;
	}
	public void setMaxSize(Integer maxSize) {
		this.maxSize = maxSize;
	}
	public ColumnType getColumnType() {
		return columnType;
	}
	public void setColumnType(ColumnType columnType) {
		this.columnType = columnType;
	}
	/**
	 * Is this column for for metadata such as:
	 *  ROW_ID, ROW_VERSION, ROW_ETAG, or ROW_BENEFACTOR
	 * 
	 * @return
	 */
	public boolean isMetadata(){
		return ROW_ID.equals(this.columnName)
				|| ROW_VERSION.equals(this.columnName)
				|| ROW_ETAG.equals(this.columnName)
				|| ROW_BENEFACTOR.equals(this.columnName);
	}
	
	
	/**
	 * Create the index definition for this column.
	 * 
	 * @return
	 */
	public String createIndexDefinition(){
		ValidateArgument.required(indexName, "indexName");
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(type, "type");
		Integer indexSize = null;
		if(MySqlColumnType.MEDIUMTEXT.equals(type)){
			indexSize = ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH;
		}else{
			indexSize = maxSize;
		}
		StringBuilder builder = new StringBuilder();
		builder.append(indexName);
		builder.append(" (");
		builder.append(columnName);
		if(indexSize != null && indexSize >= ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH){
			builder.append("(");
			builder.append(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH);
			builder.append(")");
		}
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Comparator based on DatabaseColumnInfo.cardinality;
	 */
	public static Comparator<DatabaseColumnInfo> CARDINALITY_COMPARATOR = new Comparator<DatabaseColumnInfo>() {
		@Override
		public int compare(DatabaseColumnInfo one, DatabaseColumnInfo two) {
			ValidateArgument.required(one, "DatabaseColumnInfo");
			ValidateArgument.required(one.getCardinality(), "DatabaseColumnInfo.getCardinality()");
			ValidateArgument.required(two, "DatabaseColumnInfo");
			ValidateArgument.required(two.getCardinality(), "DatabaseColumnInfo.getCardinality()");
			return Long.compare(one.cardinality, two.cardinality);
		}
	};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cardinality == null) ? 0 : cardinality.hashCode());
		result = prime * result
				+ ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result
				+ ((columnType == null) ? 0 : columnType.hashCode());
		result = prime * result + (hasIndex ? 1231 : 1237);
		result = prime * result
				+ ((indexName == null) ? 0 : indexName.hashCode());
		result = prime * result + ((maxSize == null) ? 0 : maxSize.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatabaseColumnInfo other = (DatabaseColumnInfo) obj;
		if (cardinality == null) {
			if (other.cardinality != null)
				return false;
		} else if (!cardinality.equals(other.cardinality))
			return false;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (columnType != other.columnType)
			return false;
		if (hasIndex != other.hasIndex)
			return false;
		if (indexName == null) {
			if (other.indexName != null)
				return false;
		} else if (!indexName.equals(other.indexName))
			return false;
		if (maxSize == null) {
			if (other.maxSize != null)
				return false;
		} else if (!maxSize.equals(other.maxSize))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "DatabaseColumnInfo [columnName=" + columnName + ", hasIndex="
				+ hasIndex + ", type=" + type + ", maxSize=" + maxSize
				+ ", cardinality=" + cardinality + ", indexName=" + indexName
				+ ", columnType=" + columnType + "]";
	}

	
}
