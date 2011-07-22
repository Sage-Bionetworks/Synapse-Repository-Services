package org.sagebionetworks;

import java.util.Iterator;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class StackUtilsTest {
	
	private Properties required;
	
	@Before
	public void before(){
		required = new Properties();
		required.setProperty("keyOne", "");
		required.setProperty("keyTwo", "");
		required.setProperty("keyThree", "empty");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullBoth(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullRequired(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(null, toTest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullToTest(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(required, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToTestEmpty(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(required, toTest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToTestSameCountButWrong(){
		Properties toTest = new Properties();
		// Fill it with the same number but wrong
		for(int i=0; i<required.size(); i++){
			toTest.setProperty("wrongKey"+i, ""+i);
		}
		StackUtils.validateRequiredProperties(required, toTest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToTestEmptyValue(){
		Properties toTest = new Properties();
		// Fill it with the same number but wrong
		Iterator<Object> it = required.keySet().iterator();
		while(it.hasNext()){
			String key = (String) it.next();
			toTest.setProperty(key, " ");
		}
		StackUtils.validateRequiredProperties(required, toTest);
	}
	
	@Test
	public void testValid(){
		Properties toTest = new Properties();
		// Fill it with the same number but wrong
		Iterator<Object> it = required.keySet().iterator();
		int count = 0;
		while(it.hasNext()){
			String key = (String) it.next();
			toTest.setProperty(key, ""+count);
			count++;
		}
		StackUtils.validateRequiredProperties(required, toTest);
	}


}
