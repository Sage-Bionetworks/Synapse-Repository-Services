package org.sagebionetworks.repo.manager.grid.internal.replica.view.filter;

public interface ViewFilter {

	/**
	 * The SQL of the condition modified by the index.
	 * 
	 * @param index The position of this filter in list of all filters. This can be
	 *              used to provide unique keys when there are multiple instances of
	 *              the same filter type used.
	 * @return
	 */
	String getConditionSql(int index);

	/**
	 * Get the parameter key used for this filter's value.
	 * 
	 * @param index The position of this filter in list of all filters. This can be
	 *              used to provide unique keys when there are multiple instances of
	 *              the same filter type used.
	 * @return
	 */
	String getParameterKey(int index);

	/**
	 * This filter's parameter value.
	 * 
	 * @return
	 */
	Object getParameterValue();

}
