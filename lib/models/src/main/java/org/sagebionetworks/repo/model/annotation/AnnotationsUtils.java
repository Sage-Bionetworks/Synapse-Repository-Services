package org.sagebionetworks.repo.model.annotation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.InvalidModelException;

public class AnnotationsUtils {
	
	/**
	 * Ensure that Annotation keys are unique.
	 * 
	 * @param annos
	 */
	public static void validateAnnotations(Annotations annos) {
		if (annos == null) {
			throw new IllegalArgumentException("Annotations cannot be null");
		}
		Set<String> keys = new HashSet<String>();
		if (annos.getDoubleAnnos() != null) {
			checkAnnos(keys, annos.getDoubleAnnos());
		}
		if (annos.getLongAnnos() != null) {
			checkAnnos(keys, annos.getLongAnnos());
		}
		if (annos.getStringAnnos() != null) {
			checkAnnos(keys, annos.getStringAnnos());
		}
	}
	
	private static void checkAnnos(Set<String> keys, Collection<? extends AnnotationBase> annoCollection) {
		for (AnnotationBase ba : annoCollection) {
			if (!keys.add(ba.getKey())) {
				throw new InvalidModelException("Duplicate annotations found for key: " + ba.getKey());
			}
		}
	}
}
