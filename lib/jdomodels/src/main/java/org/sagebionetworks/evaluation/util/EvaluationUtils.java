package org.sagebionetworks.evaluation.util;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;

public class EvaluationUtils {
	
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
	 * Ensure that a given Evaluation is in the OPEN state.
	 * 
	 * @param comp
	 */
	public static void ensureEvaluationIsOpen(Evaluation comp) {
		if (comp.getStatus() != EvaluationStatus.OPEN)
			throw new IllegalStateException("Evaluation ID: " + comp.getId() + " is not currently open");
	}

}
