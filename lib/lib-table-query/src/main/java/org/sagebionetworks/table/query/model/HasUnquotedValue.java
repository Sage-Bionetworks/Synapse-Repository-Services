package org.sagebionetworks.table.query.model;

/**
 * Some SQLElements can optionally be surrounded with quotes. 
 * Such elements should implement this interfaces to provided
 * a value without quotes.
 *
 */
public interface HasUnquotedValue extends Element {

	/**
	 * Get the unquoted value from this element.
	 * @return
	 */
	public String getUnquotedValue();
}
