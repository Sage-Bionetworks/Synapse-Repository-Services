package org.sagebionetworks.repo.model.annotation;

import org.junit.Test;
import org.sagebionetworks.repo.model.InvalidModelException;

public class AnnotationsUtilsTest {

	@Test
	public void testValidateAnnotations() {
		Annotations annos = AnnotationsUtils.createDummyAnnotations();
		AnnotationsUtils.validateAnnotations(annos);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testValidateAnnotationsDuplicateKeySameType() {
		org.sagebionetworks.repo.model.annotation.Annotations annos = AnnotationsUtils.createDummyAnnotations();
		
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
		org.sagebionetworks.repo.model.annotation.Annotations annos = AnnotationsUtils.createDummyAnnotations();
		
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
}
