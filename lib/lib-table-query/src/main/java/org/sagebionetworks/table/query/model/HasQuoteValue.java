package org.sagebionetworks.table.query.model;

/**
 * For SQLElements that have a value that can be surrounded with quotes. 
 *
 */
public interface HasQuoteValue extends Element {

	/**
	 * Get the value of this element without quotes.
	 * 
	 * @return
	 */
	public String getValueWithoutQuotes();
	
	/**
	 * Is this element surrounded with quotes?
	 * @return
	 */
	public boolean isSurrounedeWithQuotes();
	
	/**
	 * Replace the unquoted value.
	 * @param newValue
	 */
	public void replaceUnquoted(String newValue);
}
