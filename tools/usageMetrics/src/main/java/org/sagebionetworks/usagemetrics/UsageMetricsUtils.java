package org.sagebionetworks.usagemetrics;

public class UsageMetricsUtils {
	
	/**
	 * 
	 * 
	 * @param name
	 * @return true if the user name is that known to be an administrator or a developer
	 */
	public static boolean isOmittedName(String name) {
		name = name.toLowerCase().trim();
		
		for (int i=0; i<distinctiveDeveloperAndAdminNames.length; i++) {
			if (name.indexOf(distinctiveDeveloperAndAdminNames[i])>=0)  return true;
		}
		
		if (name.endsWith("sagebase.org")) {
			for (int i=0; i<developerAndAdminNames.length; i++) {
				if (name.indexOf(developerAndAdminNames[i])>=0)  return true;
			}
		}
		
		return false;
	}
	
	// names we are confident to be developer or admin names in ANY domain
	// must all be lower case
	private static final String[] distinctiveDeveloperAndAdminNames = {
		"kellen",  "deflaux", "burdick", "furia", "schildwachter", 
		"gepipeline", "public", "synapsify", "produseradmin"
	};
	
	// names we are confident to be developer or admin names in the 'sagebase' domain
	// (omitting those in distinctiveDeveloperAndAdminNames, checked previously)
	// must all be lower case
	private static final String[] developerAndAdminNames = {
		"hoff", "hill", "shepardson", "holt", "platform"
	};
	
	
}
