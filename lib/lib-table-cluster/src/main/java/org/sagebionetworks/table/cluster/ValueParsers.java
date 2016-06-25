package org.sagebionetworks.table.cluster;

import org.sagebionetworks.util.TimeUtils;

/**
 * Reusable ValueParsers.
 * 
 *
 */
public class ValueParsers {

	/**
	 * Parser for strings
	 */
	public static final ValueParser STRING_PARSER = new ValueParser() {

		@Override
		public Object parseValue(String value) throws IllegalArgumentException {
			// nothing to parse for strings.
			return value;
		}
	};

	/**
	 * Parser for dates
	 */
	public static final ValueParser DATE_PARSER = new ValueParser() {

		@Override
		public Object parseValue(String value) throws IllegalArgumentException {
			/*
			 * Dates can be a long or string. Long is tried first, then the
			 * string.
			 */
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException e) {
				return TimeUtils.parseSqlDate(value);
			}
		}
	};

	/**
	 * Parser for long values.
	 */
	public static final ValueParser LONG_PARSER = new ValueParser() {
		@Override
		public Object parseValue(String value) throws IllegalArgumentException {
			try {
				return Long.parseLong(value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
		}
	};

	/**
	 * Parser for Doubles.
	 */
	public static final ValueParser DOUBLE_PARSER = new ValueParser() {
		@Override
		public Object parseValue(String value) throws IllegalArgumentException {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(e);
			}
		}
	};

	/**
	 * Parser for booleans.
	 */
	public static final ValueParser BOOLEAN_PARSER = new ValueParser() {

		@Override
		public Object parseValue(String value) throws IllegalArgumentException {
			return Boolean.parseBoolean(value);
		}
	};

}
