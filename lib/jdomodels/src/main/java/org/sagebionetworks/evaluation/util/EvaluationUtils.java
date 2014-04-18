package org.sagebionetworks.evaluation.util;

import java.util.Collection;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;

public class EvaluationUtils {

	/**
	 * Ensure that an argument is not null.
	 */
	public static void ensureNotNull(Object o, String name) {
		if (o == null) {
			throw new IllegalArgumentException(name + " cannot be null");
		}
	}
	
	/**
	 * Ensure that a Collection argument is not empty
	 * @param o
	 * @param name
	 */
	public static void ensureNotEmpty(Collection o, String name) {
	if (o == null || o.isEmpty()) {
		throw new IllegalArgumentException(name + " cannot be empty");
	}
}

	/**
	 * Ensure that a given Evaluation is in the OPEN state.
	 */
	public static void ensureEvaluationIsOpen(Evaluation comp) {
		if (!EvaluationStatus.OPEN.equals(comp.getStatus())) {
				throw new IllegalStateException("Evaluation ID: " +
						comp.getId() + " is not currently open");
		}
	}
}
