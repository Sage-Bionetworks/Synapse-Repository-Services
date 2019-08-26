package org.sagebionetworks.util.doubles;

import org.sagebionetworks.util.ValidateArgument;

public class DoubleUtils {


	/**
	 * Converts strings to their representation of a {@link Double}. Unlike {@link Double#valueOf(double)},
	 * this accepts alternate string representations of Infinity and NaN, such as "inf" and "nan".
	 * @param string the string to be parsed.
	 * @return a {@code Double} object holding the value represented by the {@code String} argument.
	 * @throws NumberFormatException if the string is not a parsable double
	 */
	public static Double fromString(String string) throws NumberFormatException{
		ValidateArgument.required(string, "string");
		try {
			return Double.valueOf(string);
		} catch (NumberFormatException e){
			// Try to match it to NaN or Infinity.
			return AbstractDouble.lookupType(string).getDoubleValue();
		}
	}
}
