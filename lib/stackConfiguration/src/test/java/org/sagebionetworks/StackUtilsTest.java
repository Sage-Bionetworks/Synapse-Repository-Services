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
		required.setProperty("keyThree", "");
		required.setProperty(StackConstants.STACK_PROPERTY_NAME, "");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullBoth(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(null, null, null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullRequired(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(null, toTest, null, null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullToTest(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(required, null, "test", "A");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToTestEmpty(){
		Properties toTest = new Properties();
		StackUtils.validateRequiredProperties(required, toTest, "test", "A");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testToTestSameCountButWrong(){
		Properties toTest = new Properties();
		// Fill it with the same number but wrong
		for(int i=0; i<required.size(); i++){
			toTest.setProperty("wrongKey"+i, ""+i);
		}
		StackUtils.validateRequiredProperties(required, toTest, "test", "A");
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
		StackUtils.validateRequiredProperties(required, toTest, "test", "A");
	}
	
	@Test
	public void testValidateFileURL(){
		String key = "some.url";
		String stack = "stackName";
		String value = "file:///c:/Users/jmhill/.m2/"+stack+"my-local.properties";
		StackUtils.validateStackProperty(stack, key, value);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFileURLInvalid(){
		String key = "some.url";
		String stack = "stackName";
		String value = "file:///c:/Users/jmhill/.m2/a"+stack+"my-local.properties";
		StackUtils.validateStackProperty(stack, key, value);
	}
	
	@Test
	public void testValidateFileURLBackslash(){
		String key = "some.url";
		String stack = "stackName";
		String value = "file:///c:\\Users\\jmhill\\.m2\\"+stack+"my-local.properties";
		StackUtils.validateStackProperty(stack, key, value);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateFileURLBackslashInvalid(){
		String key = "some.url";
		String stack = "stackName";
		String value = "file:///c:\\Users\\jmhill\\.m2\\a"+stack+"my-local.properties";
		StackUtils.validateStackProperty(stack, key, value);
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
		StackUtils.validateRequiredProperties(required, toTest, "test", "A");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidStackPrefixMissing(){
		Properties template = new Properties();
		String stack ="myStackName";
		String instance ="B";
		template.setProperty("org.sagebionetworks.repository.database.username", StackConstants.REQUIRES_STACK_PREFIX);
		template.setProperty(StackConstants.STACK_PROPERTY_NAME, StackConstants.REQUIRES_STACK_PREFIX);
		
		Properties realValues = new Properties();
		realValues.setProperty("org.sagebionetworks.repository.database.username", "missingPrifix");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, "myStackName");
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test
	public void testValidStackPrefix(){
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.repository.database.username", StackConstants.REQUIRES_STACK_PREFIX);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		realValues.setProperty("org.sagebionetworks.repository.database.username", stack+"DBUserName");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test
	public void testValidStackPrefixInstance(){
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.repository.database.username", StackConstants.REQUIRES_STACK_PREFIX+StackConstants.REQUIRES_STACK_INTANCE_PREFIX);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		realValues.setProperty("org.sagebionetworks.repository.database.username", stack+instance+"DBUserName");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidStackPrefixInstanceInvalid(){
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.repository.database.username", StackConstants.REQUIRES_STACK_PREFIX+StackConstants.REQUIRES_STACK_INTANCE_PREFIX);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		realValues.setProperty("org.sagebionetworks.repository.database.username", stack+"DBUserName");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test
	public void testValidStackPrefixInstanceDatabaseUrl(){
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.repository.database.connection.url", StackConstants.REQUIRES_STACK_PREFIX+StackConstants.REQUIRES_STACK_INTANCE_PREFIX);

		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		String dbURL = "jdbc:mysql://localhost/"+stack+instance+"Schema";
		realValues.setProperty("org.sagebionetworks.repository.database.connection.url", dbURL);
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidStackPrefixInstanceDatabaseUrlInvalid(){
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.repository.database.connection.url", StackConstants.REQUIRES_STACK_PREFIX+StackConstants.REQUIRES_STACK_INTANCE_PREFIX);
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		String dbURL = "jdbc:mysql://localhost/"+stack+"Schema";
		realValues.setProperty("org.sagebionetworks.repository.database.connection.url", dbURL);
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}

	@Test
	public void testValidHardcodedPrefix(){
		String prefixPart1 = "https://doc-";	
		
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		realValues.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1+stack+"XXXXX.cloudsearch.amazonaws.com/2011-02-01/documents/batch");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidHardcodedPrefix(){
		String prefixPart1 = "https://doc-";	
		
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		// note that this url is http instead of https
		realValues.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", "http://doc-"+stack+"XXXXX.cloudsearch.amazonaws.com/2011-02-01/documents/batch");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test
	public void testValidStackCompoundPrefix(){
		String prefixPart1 = "https://doc-";	
		
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1 + StackConstants.REQUIRES_STACK_PREFIX);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		realValues.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1+stack+"XXXXX.cloudsearch.amazonaws.com/2011-02-01/documents/batch");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidStackCompoundPrefixPart1(){
		String prefixPart1 = "https://doc-";	
		
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1 + StackConstants.REQUIRES_STACK_PREFIX);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		// note that this url is http instead of https
		realValues.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", "http://doc-"+stack+"XXXXX.cloudsearch.amazonaws.com/2011-02-01/documents/batch");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInvalidStackCompoundPrefixPart2(){
		String prefixPart1 = "https://doc-";	
		
		Properties template = new Properties();
		template.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1 + StackConstants.REQUIRES_STACK_PREFIX);
		
		Properties realValues = new Properties();
		String stack ="myStackName";
		String instance ="B";
		// Note that we have the wrong stack name here
		realValues.setProperty("org.sagebionetworks.cloudsearch.documentservice.endpoint", prefixPart1+"wrongStack"+"XXXXX.cloudsearch.amazonaws.com/2011-02-01/documents/batch");
		realValues.setProperty(StackConstants.STACK_PROPERTY_NAME, stack);
		StackUtils.validateRequiredProperties(template, realValues, stack, instance);
	}
}
