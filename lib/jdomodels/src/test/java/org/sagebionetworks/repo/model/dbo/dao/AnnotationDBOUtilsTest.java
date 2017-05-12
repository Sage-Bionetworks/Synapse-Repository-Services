package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.sagebionetworks.evaluation.dbo.StringAnnotationDBO;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.dbo.AnnotationDBOUtils;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class AnnotationDBOUtilsTest {

	@Test
	public void testCreateStringAnnotationDBOTruncate(){
		StringAnnotation anno = new StringAnnotation();
		anno.setIsPrivate(true);
		anno.setKey("toolong");
		// Create a value that is larger than the max
		Long owner = new Long(45);
		char[] chars = new char[SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH+100];
		Arrays.fill(chars, 'a');
		String longString = new String(chars);
		String truncated = longString.substring(0, SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH-1);
		anno.setValue(longString);
		StringAnnotationDBO dbo = AnnotationDBOUtils.createStringAnnotationDBO(owner, anno);
		assertEquals(owner, dbo.getOwnerId());
		assertEquals("toolong", dbo.getAttribute());
		assertTrue(dbo.getIsPrivate());
		assertEquals(truncated, dbo.getValue());
	}
	

}
