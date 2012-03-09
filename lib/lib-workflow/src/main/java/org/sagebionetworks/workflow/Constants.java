package org.sagebionetworks.workflow;

/**
 * A class to hold constants relevant to workflow activities
 * 
 * @author deflaux
 */
public class Constants {

	/**
	 * Use this value to indicate that no further activities need to occur for this workflow instance
	 */
	public static final String WORKFLOW_DONE = "workflowDone";
		
	/**
	 * A short timeout
	 */
	public static final int FIVE_MINUTES_OF_SECONDS = 300;
	
	/**
	 * A moderate timeout
	 */
	public static final int	ONE_HOUR_OF_SECONDS = 3600;

	/**
	 * A longer timeout
	 */
	public static final int	FOUR_HOURS_OF_SECONDS = ONE_HOUR_OF_SECONDS * 4;
	
	/**
	 * A really long timeout to be used when you do not have a better idea of an appropriate timeout
	 */
	public static final int ONE_DAY_OF_SECONDS = ONE_HOUR_OF_SECONDS * 24;
}
