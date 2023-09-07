package org.sagebionetworks.table.cluster;

import static org.sagebionetworks.repo.model.table.TableConstants.RESERVED_COLUMNS_NAMES;

import java.util.Comparator;
import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Information about a column in the database.
 *
 */
public class DatabaseColumnInfo {
	
	private String columnName;
	private boolean hasIndex;
	private MySqlColumnType type;
	private Integer maxSize;
	private Long cardinality;
	private String indexName;
	private ColumnType columnType;
	private String constraintName;
	
	public String getColumnName() {
		return columnName;
	}
	public DatabaseColumnInfo setColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}
	public boolean hasIndex() {
		return hasIndex;
	}
	public DatabaseColumnInfo setHasIndex(boolean hasIndex) {
		this.hasIndex = hasIndex;
		return this;
	}
	public Long getCardinality() {
		return cardinality;
	}
	public DatabaseColumnInfo setCardinality(Long cardinality) {
		this.cardinality = cardinality;
		return this;
	}
	public String getIndexName() {
		return indexName;
	}
	public DatabaseColumnInfo setIndexName(String indexName) {
		this.indexName = indexName;
		return this;
	}
	public MySqlColumnType getType() {
		return type;
	}
	public DatabaseColumnInfo setType(MySqlColumnType type) {
		this.type = type;
		return this;
	}
	public Integer getMaxSize() {
		return maxSize;
	}
	public DatabaseColumnInfo setMaxSize(Integer maxSize) {
		this.maxSize = maxSize;
		return this;
	}
	public ColumnType getColumnType() {
		return columnType;
	}
	public DatabaseColumnInfo setColumnType(ColumnType columnType) {
		this.columnType = columnType;
		return this;
	}
	public String getConstraintName() {
		return constraintName;
	}
	public DatabaseColumnInfo setConstraintName(String constraintName) {
		this.constraintName = constraintName;
		return this;
	}
	/**
	 * Is this column for for metadata such as:
	 *  ROW_ID, ROW_VERSION, ROW_ETAG, or ROW_BENEFACTOR
	 * 
	 * @return
	 */
	public boolean isMetadata() {
		return RESERVED_COLUMNS_NAMES.contains(this.columnName);
	}
	
	public boolean isCreateIndex() {
		if (isMetadata()) {
			return false;
		}
		if (ColumnTypeListMappings.isList(columnType)) {
			ColumnType nonListType = ColumnTypeListMappings.nonListType(columnType);
			return ColumnTypeInfo.getInfoForType(nonListType).getMySqlType().isCreateIndex();
		}
		return type.isCreateIndex();
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
		if (MySqlColumnType.MEDIUMTEXT.equals(type) || MySqlColumnType.TEXT.equals(type)){
			indexSize = ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH;
		}else{
			indexSize = maxSize;
		}
		boolean isListType = ColumnTypeListMappings.isList(columnType);
		
		StringBuilder builder = new StringBuilder();
		builder.append(indexName);
		builder.append(" (");
		if (isListType) {
			ColumnTypeInfo nonListType = ColumnTypeInfo.getInfoForType(ColumnTypeListMappings.nonListType(columnType));
			
			builder.append("(CAST(")
				.append(columnName)
				.append(" AS ")
				.append(nonListType.getMySqlType().getMySqlCastType());
			
			if (nonListType.isStringType()) {
				builder.append("(");
				builder.append(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH);
				builder.append(")");
			}
			
			builder.append(" ARRAY))");			
		} else {
			builder.append(columnName);
			if(indexSize != null && indexSize >= ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH){
				builder.append("(");
				builder.append(ColumnConstants.MAX_MYSQL_VARCHAR_INDEX_LENGTH);
				builder.append(")");
			}
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
		return Objects.hash(cardinality, columnName, columnType, constraintName, hasIndex, indexName, maxSize, type);
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
		return Objects.equals(cardinality, other.cardinality) && Objects.equals(columnName, other.columnName)
				&& columnType == other.columnType && Objects.equals(constraintName, other.constraintName)
				&& hasIndex == other.hasIndex && Objects.equals(indexName, other.indexName)
				&& Objects.equals(maxSize, other.maxSize) && type == other.type;
	}
	@Override
	public String toString() {
		return "DatabaseColumnInfo [columnName=" + columnName + ", hasIndex=" + hasIndex + ", type=" + type
				+ ", maxSize=" + maxSize + ", cardinality=" + cardinality + ", indexName=" + indexName + ", columnType="
				+ columnType + ", constraintName=" + constraintName + "]";
	}


}
