package org.sagebionetworks.table.query.model;

/**
 * Any element that can determine a function type should implement this interface.
 *
 */
public interface HasFunctionType extends Element {
	
	/**
	 * The type of function represented by this element.
	 * 
	 * @return
	 */
	public FunctionType getFunctionType();

}
