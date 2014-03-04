package org.sagebionetworks.evaluation.util;

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
	 * Ensure that a given Evaluation is in the OPEN state.
	 */
	public static void ensureEvaluationIsOpen(Evaluation comp) {
		if (!EvaluationStatus.OPEN.equals(comp.getStatus())) {
				throw new IllegalStateException("Evaluation ID: " +
						comp.getId() + " is not currently open");
		}
	}
}
