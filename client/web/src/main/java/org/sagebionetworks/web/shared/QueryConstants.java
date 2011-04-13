package org.sagebionetworks.web.shared;

public class QueryConstants {

	/**
	 * The basic object types supported by the QueryService.
	 * 
	 */
	public enum ObjectType {
		dataset(), layer();
	}

	/**
	 * The supported where operators.
	 * 
	 */
	public enum WhereOperator {

		EQUALS("=="), 
		NOT_EQUALS("!="),
		GREATER_THAN(">"),
		GREATER_THAN_OR_EQUALS(">="),
		LESS_THAN("<"),
		LESS_THAN_OR_EQUALS("<="),
		;

		String sqlString;

		WhereOperator(String sqlString) {
			this.sqlString = sqlString;
		};

		public String toSql() {
			return sqlString;
		}

		/**
		 * Get the operator for the given SQL
		 * 
		 * @param operatorSql
		 * @return
		 */
		public static WhereOperator fromSql(String operatorSql) {
			WhereOperator[] array = values();
			for (int i = 0; i < array.length; i++) {
				if (array[i].toSql().equals(operatorSql))
					return array[i];
			}
			throw new IllegalArgumentException("Unknown opperator: "+ operatorSql);
		}
	}
	
	/**
	 * "And" and "Or" are the basic condition operators.
	 *
	 */
	public enum ConitionOperator {

		AND("and"), 
		OR("or");

		String sqlString;

		ConitionOperator(String sqlString) {
			this.sqlString = sqlString;
		};

		public String toSql() {
			return sqlString;
		}

		/**
		 * Get the operator for the given SQL
		 * 
		 * @param operatorSql
		 * @return
		 */
		public static ConitionOperator fromSql(String operatorSql) {
			ConitionOperator[] array = values();
			for (int i = 0; i < array.length; i++) {
				if (array[i].toSql().equals(operatorSql))
					return array[i];
			}
			throw new IllegalArgumentException("Unknown opperator: "+ operatorSql);
		}
	}

}
