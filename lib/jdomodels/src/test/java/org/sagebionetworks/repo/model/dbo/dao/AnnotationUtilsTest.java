package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.dbo.persistence.DBODateAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoubleAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOLongAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStringAnnotation;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class AnnotationUtilsTest {
	
	@Test
	public void testCreateStringAnnotationsTruncate(){
		Annotations annos = new Annotations();
		// Create a value that is larger than the max
		Long owner = new Long(45);
		char[] chars = new char[SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH+100];
		Arrays.fill(chars, 'a');
		String longString = new String(chars);
		String truncated = longString.substring(0, SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH-1);
		annos.addAnnotation("tooLong",longString);
		List<DBOStringAnnotation> results = AnnotationUtils.createStringAnnotations(owner, annos.getStringAnnotations());
		assertNotNull(results);
		assertEquals(1, results.size());
		// Check the truncated value
		DBOStringAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("tooLong", toCheck.getAttribute());
		assertEquals(truncated, toCheck.getValue());
	}
	
	@Test
	public void testCreateStringAnnotationsList(){
		Annotations annos = new Annotations();
		// Create a value that is larger than the max
		Long owner = new Long(45);
		annos.addAnnotation("list", "one");
		annos.addAnnotation("list", "two");
		List<DBOStringAnnotation> results = AnnotationUtils.createStringAnnotations(owner, annos.getStringAnnotations());
		assertNotNull(results);
		assertEquals(2, results.size());
		// Check the values
		DBOStringAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals("one", toCheck.getValue());
		// Check the values
		toCheck = results.get(1);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals("two", toCheck.getValue());
	}
	
	@Test
	public void testCreateLongList(){
		Annotations annos = new Annotations();
		// Create a value that is larger than the max
		Long owner = new Long(45);
		annos.addAnnotation("list", new Long(123));
		annos.addAnnotation("list", new Long(456));
		List<DBOLongAnnotation> results = AnnotationUtils.createLongAnnotations(owner, annos.getLongAnnotations());
		assertNotNull(results);
		assertEquals(2, results.size());
		// Check the values
		DBOLongAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals(new Long(123), toCheck.getValue());
		// Check the values
		toCheck = results.get(1);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals(new Long(456), toCheck.getValue());
	}
	
	@Test
	public void testCreateDoubleList(){
		Annotations annos = new Annotations();
		// Create a value that is larger than the max
		Long owner = new Long(45);
		annos.addAnnotation("list", new Double(123.1));
		annos.addAnnotation("list", new Double(456.3));
		List<DBODoubleAnnotation> results = AnnotationUtils.createDoubleAnnotations(owner, annos.getDoubleAnnotations());
		assertNotNull(results);
		assertEquals(2, results.size());
		// Check the values
		DBODoubleAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals(new Double(123.1), toCheck.getValue());
		// Check the values
		toCheck = results.get(1);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals(new Double(456.3), toCheck.getValue());
	}
	
	@Test
	public void testCreateDateList(){
		Annotations annos = new Annotations();
		// Create a value that is larger than the max
		Long owner = new Long(45);
		annos.addAnnotation("list", new Date(1));
		annos.addAnnotation("list", new Date(2));
		List<DBODateAnnotation> results = AnnotationUtils.createDateAnnotations(owner, annos.getDateAnnotations());
		assertNotNull(results);
		assertEquals(2, results.size());
		// Check the values
		DBODateAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals(new Date(1), toCheck.getValue());
		// Check the values
		toCheck = results.get(1);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("list", toCheck.getAttribute());
		assertEquals(new Date(2), toCheck.getValue());
	}

}
