package org.sagebionetworks.table.query.model;

import org.sagebionetworks.util.ValidateArgument;

/**
 * An element used to override an existing element in the tree.
 * The value will be treated as a non-quoted string such as a RegularIdentifier or 
 * an UnsignedNumericLiteral
 *
 */
public class StringOverride extends LeafElement {
	
	String stringValue;
	
	/**
	 * Create an new override 
	 * @param value
	 */
	public StringOverride(String value){
		ValidateArgument.required(value, "value");
		this.stringValue = value;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(stringValue);
	}

}
