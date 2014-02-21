package org.sagebionetworks.repo.model.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.InvalidModelException;

public class AnnotationsUtilsTest {

	@Test
	public void testValidateAnnotations() {
		Annotations annos = createDummyAnnotations();
		AnnotationsUtils.validateAnnotations(annos);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testValidateAnnotationsDuplicateKeySameType() {
		org.sagebionetworks.repo.model.annotation.Annotations annos = createDummyAnnotations();
		
		StringAnnotation sa1 = new StringAnnotation();
		sa1.setIsPrivate(false);
		sa1.setKey("foo");
		sa1.setValue("bar");
		
		StringAnnotation sa2 = new StringAnnotation();
		sa2.setIsPrivate(false);
		sa2.setKey("foo");
		sa2.setValue("baz");
		
		annos.getStringAnnos().add(sa1);
		annos.getStringAnnos().add(sa2);		
		
		AnnotationsUtils.validateAnnotations(annos);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testValidateAnnotationsDuplicateKeyDifferentType() {
		org.sagebionetworks.repo.model.annotation.Annotations annos = createDummyAnnotations();
		
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey("foo");
		sa.setValue("bar");
		
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("foo");
		da.setValue(3.14);
		
		annos.getStringAnnos().add(sa);
		annos.getDoubleAnnos().add(da);		
		
		AnnotationsUtils.validateAnnotations(annos);
	}
	
	@Test
	public void testFillInMissingIsPrivateField() {
		Annotations expected = createDummyAnnotations();
		expected.getStringAnnos().get(0).setIsPrivate(true);
		expected.getLongAnnos().get(0).setIsPrivate(true);
		expected.getDoubleAnnos().get(0).setIsPrivate(true);
		Annotations actual = createDummyAnnotations();
		actual.getStringAnnos().get(0).setIsPrivate(null);
		actual.getLongAnnos().get(0).setIsPrivate(null);
		actual.getDoubleAnnos().get(0).setIsPrivate(null);
		assertFalse(expected.equals(actual));
		AnnotationsUtils.populateMissingFields(actual);
		assertEquals(expected, actual);
	}
	
	/**
	 * Create a populated Annotations object.
	 * 
	 * @return
	 */
	public static Annotations createDummyAnnotations() {
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey("string anno");
		sa.setValue("foo ");
		stringAnnos.add(sa);
		
		StringAnnotation sa2 = new StringAnnotation();
		sa2.setIsPrivate(false);
		sa2.setKey("string anno_null");
		sa2.setValue(null);
		stringAnnos.add(sa2);
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey("long anno");
		la.setValue(10L);
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da = new DoubleAnnotation();
		da.setIsPrivate(false);
		da.setKey("double anno");
		da.setValue(0.5);
		doubleAnnos.add(da);
		
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		annos.setObjectId("1");
		annos.setScopeId("2");
		return annos;
	}
}
