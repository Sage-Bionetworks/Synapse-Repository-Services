package org.sagebionetworks.repo.model.dbo;

/**
 * Utility for generating Data Manipulation Language (DML) statements.
 *  
 * @author John
 *
 */
public class DMLUtils {

	/**
	 * Create an INSERT statement for a given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createInsertStatement(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("DBOMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("INSERT INTO ");
		main.append(mapping.getTableName());

		// Build up the columns and values.
		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(i != 0){
				columns.append(", ");
				values.append(", ");
			}
			columns.append("`");
			columns.append(fc.getColumnName());
			columns.append("`");
			values.append(":");
			values.append(fc.getFieldName());
		}
		main.append("(");
		main.append(columns.toString());
		main.append(") VALUES (");
		main.append(values.toString());
		main.append(")");
		return main.toString();
	}
	
	/**
	 * Create an INSERT statement for a given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createGetByIDStatement(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("DBOMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT * FROM ");
		main.append(mapping.getTableName());
		// Setup the primary key
		main.append(" WHERE ");
		appendPrimaryKey(mapping, main);
		return main.toString();
	}
	
	/**
	 * Create a COUNT statement for a given mapping
	 * @param mapping
	 * @return the COUNT statement
	 */
	public static String createGetCountStatement(TableMapping mapping) {
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("SELECT COUNT("+getPrimaryFieldColumnName(mapping)+") FROM ");
		main.append(mapping.getTableName());
		return main.toString();		
	}

	public static String getPrimaryFieldColumnName(TableMapping mapping) {
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(fc.isPrimaryKey()) return fc.getColumnName();
		}
		throw new IllegalArgumentException("Table "+mapping.getTableName()+" has no primary key.");
	}

	/**
	 * Append the primary key
	 * @param mapping
	 * @param main
	 */
	public static void appendPrimaryKey(TableMapping mapping, StringBuilder main) {
		int keyCount = 0;
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(fc.isPrimaryKey()){
				if(keyCount > 0){
					main.append(" AND ");
				}
				main.append("`");
				main.append(fc.getColumnName());
				main.append("`");
				main.append(" = :");
				main.append(fc.getFieldName());
				keyCount++;
			}
		}
	}
	
	/**
	 * Create an INSERT statement for a given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createDeleteStatement(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("TableMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("DELETE FROM ");
		main.append(mapping.getTableName());
		main.append(" WHERE ");
		appendPrimaryKey(mapping, main);
		return main.toString();
	}
	
	/**
	 * Build the UPDATE sql for A given mapping.
	 * @param mapping
	 * @return
	 */
	public static String createUpdateStatment(TableMapping mapping){
		if(mapping == null) throw new IllegalArgumentException("Mapping cannot be null");
		if(mapping.getFieldColumns() == null) throw new IllegalArgumentException("TableMapping.getFieldColumns() cannot be null");
		StringBuilder main = new StringBuilder();
		main.append("UPDATE ");
		main.append(mapping.getTableName());
		main.append(" SET ");
		int count = 0;
		for(int i=0; i<mapping.getFieldColumns().length; i++){
			FieldColumn fc = mapping.getFieldColumns()[i];
			if(!fc.isPrimaryKey()){
				if(count > 0){
					main.append(", ");
				}
				main.append("`");
				main.append(fc.getColumnName());
				main.append("`");
				main.append(" = :");
				main.append(fc.getFieldName());
				count++;
			}
		}
		main.append(" WHERE ");
		appendPrimaryKey(mapping, main);
		return main.toString();
	}
}
