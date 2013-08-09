package org.sagebionetworks.repo.model.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.InvalidModelException;

public class AnnotationsUtils {
	
	/**
	 * Create a populated Annotations object.
	 * 
	 * @return
	 */
	public static Annotations createDummyAnnotations() {
		return createDummyAnnotations(1);
	}

	public static Annotations createDummyAnnotations(int i) {
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey("string anno");
		sa.setValue("foo " + i);
		stringAnnos.add(sa);
		
		StringAnnotation sa2 = new StringAnnotation();
		sa2.setIsPrivate(false);
		sa2.setKey("string anno_null");
		if (i % 2 == 1) {
			sa2.setValue(null);
		} else {
			sa2.setValue("not null");
		}
		stringAnnos.add(sa2);
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey("long anno");
		la.setValue(new Long(i*10));
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("double anno");
		da.setValue(0.5 + i);
		doubleAnnos.add(da);
		
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		annos.setObjectId("" + i);
		annos.setScopeId("" + 2*i);
		return annos;
	}

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
