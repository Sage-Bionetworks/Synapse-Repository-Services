package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.dbo.AnnotationDBOUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBODateAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoubleAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOLongAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStringAnnotation;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class AnnotationDBOUtilsTest {
	
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
		List<DBOStringAnnotation> results = AnnotationDBOUtils.createStringAnnotations(owner, annos.getStringAnnotations());
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
		List<DBOStringAnnotation> results = AnnotationDBOUtils.createStringAnnotations(owner, annos.getStringAnnotations());
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
		List<DBOLongAnnotation> results = AnnotationDBOUtils.createLongAnnotations(owner, annos.getLongAnnotations());
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
		List<DBODoubleAnnotation> results = AnnotationDBOUtils.createDoubleAnnotations(owner, annos.getDoubleAnnotations());
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
		List<DBODateAnnotation> results = AnnotationDBOUtils.createDateAnnotations(owner, annos.getDateAnnotations());
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

	@Test
	public void testCreateStringAnnotationsWillNullValue(){
		String nullStringAnnotation = null;
		Map<String, List<String>> stringAnnotations = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add(nullStringAnnotation);
		stringAnnotations.put("nullStringAnnotation", values);
		Annotations annos = new Annotations();
		annos.setStringAnnotations(stringAnnotations);
		Long owner = new Long(45);
		List<DBOStringAnnotation> results = AnnotationDBOUtils.createStringAnnotations(owner, annos.getStringAnnotations());
		assertNotNull(results);
		assertEquals(1, results.size());
		// Check the values
		DBOStringAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("nullStringAnnotation", toCheck.getAttribute());
		assertEquals(null, toCheck.getValue());
	}
	
	@Test
	public void testCreateLongAnnotationsWillNullValue(){
		Long nullLongAnnotation = null;
		Map<String, List<Long>> longAnnotations = new HashMap<String, List<Long>>();
		List<Long> values = new ArrayList<Long>();
		values.add(nullLongAnnotation);
		longAnnotations.put("nullLongAnnotation", values);
		Annotations annos = new Annotations();
		annos.setLongAnnotations(longAnnotations);
		Long owner = new Long(45);
		List<DBOLongAnnotation> results = AnnotationDBOUtils.createLongAnnotations(owner, annos.getLongAnnotations());
		assertNotNull(results);
		assertEquals(1, results.size());
		// Check the values
		DBOLongAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("nullLongAnnotation", toCheck.getAttribute());
		assertEquals(null, toCheck.getValue());
	}
	
	@Test
	public void testCreateDoubleAnnotationsWillNullValue(){
		Double nullDoubleAnnotation = null;
		Map<String, List<Double>> doubleAnnotations = new HashMap<String, List<Double>>();
		List<Double> values = new ArrayList<Double>();
		values.add(nullDoubleAnnotation);
		doubleAnnotations.put("nullDoubleAnnotation", values);
		Annotations annos = new Annotations();
		annos.setDoubleAnnotations(doubleAnnotations);
		Long owner = new Long(45);
		List<DBODoubleAnnotation> results = AnnotationDBOUtils.createDoubleAnnotations(owner, annos.getDoubleAnnotations());
		assertNotNull(results);
		assertEquals(1, results.size());
		// Check the values
		DBODoubleAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("nullDoubleAnnotation", toCheck.getAttribute());
		assertEquals(null, toCheck.getValue());
	}
	
	@Test
	public void testCreateDateAnnotationsWillNullValue(){
		Date nullDateAnnotation = null;
		Map<String, List<Date>> dateAnnotations = new HashMap<String, List<Date>>();
		List<Date> values = new ArrayList<Date>();
		values.add(nullDateAnnotation);
		dateAnnotations.put("nullDateAnnotation", values);
		Annotations annos = new Annotations();
		annos.setDateAnnotations(dateAnnotations);
		Long owner = new Long(45);
		List<DBODateAnnotation> results = AnnotationDBOUtils.createDateAnnotations(owner, annos.getDateAnnotations());
		assertNotNull(results);
		assertEquals(1, results.size());
		// Check the values
		DBODateAnnotation toCheck = results.get(0);
		assertEquals(owner, toCheck.getOwner());
		assertEquals("nullDateAnnotation", toCheck.getAttribute());
		assertEquals(null, toCheck.getValue());
	}
}
