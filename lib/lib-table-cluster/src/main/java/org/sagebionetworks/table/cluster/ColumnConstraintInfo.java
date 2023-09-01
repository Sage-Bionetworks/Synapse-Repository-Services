package org.sagebionetworks.table.cluster;

import java.util.Objects;
import java.util.Optional;

/**
 * Information about a CHECK constraint on a column.
 *
 */
public class ColumnConstraintInfo {
	
	private final String tableName;
	private final String constraintName;
	private final String columnName;
	

	public ColumnConstraintInfo(String tableName, String columnName) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.constraintName = String.format("%sCHK%s", tableName, columnName);
	}


	/**
	 * Parse {@link ColumnConstraintInfo} from the provide constraint name.
	 * @param constraintName
	 * @return {@link Optional#empty()} when the provided constraint does not parse.
	 */
	public static Optional<ColumnConstraintInfo> parseConstraintName(String constraintName){
		if(constraintName != null) {
			String[] split = constraintName.split("CHK");
			if(split.length == 2) {
				String tableName = split[0];
				String columnName = split[1];
				return Optional.of(new ColumnConstraintInfo(tableName, columnName));
			}
		}
		return Optional.empty();
	}


	/**
	 * The name of the table;
	 * 
	 * @return
	 */
	public String getTableName() {
		return tableName;
	}


	/**
	 * The full name of the constraint.
	 * @return
	 */
	public String getConstraintName() {
		return constraintName;
	}

	/**
	 * The name of the column.
	 * 
	 * @return
	 */
	public String getColumnName() {
		return columnName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, constraintName, tableName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnConstraintInfo other = (ColumnConstraintInfo) obj;
		return Objects.equals(columnName, other.columnName) && Objects.equals(constraintName, other.constraintName)
				&& Objects.equals(tableName, other.tableName);
	}

}
