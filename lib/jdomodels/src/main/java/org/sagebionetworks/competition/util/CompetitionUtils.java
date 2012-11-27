package org.sagebionetworks.competition.util;

import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;

public class CompetitionUtils {
	
	/**
	 * Ensure that an argument is not null
	 * 
	 * @param o
	 * @param objectName
	 */
	public static void ensureNotNull(Object o, String name) {
		if (o == null)
			throw new IllegalArgumentException(name + " cannot be null");		
	}
	
	/**
	 * Ensure that a given Competition is in the OPEN state.
	 * 
	 * @param comp
	 */
	public static void ensureCompetitionIsOpen(Competition comp) {
		if (comp.getStatus() != CompetitionStatus.OPEN)
			throw new IllegalStateException("Competition ID: " + comp.getId() + " is not currently open");
	}

}
