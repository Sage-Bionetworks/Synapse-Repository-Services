package org.sagebionetworks.audit.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.utils.ExampleObject.SomeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class ObjectCSVDAOTest {
	@Autowired
	private AmazonS3Client s3Client;
	private int stackInstanceNumber;
	private String bucketName;
	private Class<ExampleObject> objectClass;
	private String[] headers;
	private ObjectCSVDAO<ExampleObject> dao;

	@Before
	public void setUp() {
		stackInstanceNumber = 1;
		bucketName = "object.csv.dao.test";
		objectClass = ExampleObject.class;
		headers = new String[]{"aString", "aLong", "aBoolean", "aDouble", "anInteger", "aFloat", "someEnum"};
		s3Client.createBucket(bucketName);
		dao = new ObjectCSVDAO<ExampleObject>(s3Client, stackInstanceNumber, bucketName, objectClass, headers);
	}

	@After
	public void cleanUp() {
		dao.deleteAllStackInstanceBatches();
	}

	/**
	 * Test write and read methods
	 * @throws Exception
	 */
	@Test
	public void testRoundTrip() throws Exception{
		Long timestamp = System.currentTimeMillis();
		boolean rolling = false;

		// Build up some sample data
		List<ExampleObject> data = buildExampleObjectList(12);
		String key = dao.write(data, timestamp, rolling);

		List<ExampleObject> actual = dao.read(key);
		assertEquals(data, actual);

		dao.delete(key);
		assertEquals(0, dao.listAllKeys().size());
	}

	@Test
	public void testListBatchKeys() throws Exception {
		int count = 5;
		Set<String> keys = new HashSet<String>();
		for(int i=0; i< count; i++){
			List<ExampleObject> data = buildExampleObjectList(12);
			String key = dao.write(data, System.currentTimeMillis(), false);
			assertNotNull(key);
			keys.add(key);
		}
		// Now iterate over all key and ensure all keys are found
		Set<String> foundKeys = dao.listAllKeys();
		// the two set should be equal
		assertEquals(keys, foundKeys);
	}

	private List<ExampleObject> buildExampleObjectList(int count) {
		List<ExampleObject> data = new LinkedList<ExampleObject>();
		for(int i=0; i<count; i++){
			ExampleObject ob = new ExampleObject();
			ob.setaBoolean(i%2 == 0);
			ob.setaString("Value,"+i);
			ob.setaLong(new Long(11*i));
			ob.setaDouble(12312312.34234/i);
			ob.setAnInteger(new Integer(i));
			ob.setaFloat(new Float(123.456*i));
			ob.setSomeEnum(SomeEnum.A);
			// Add some nulls
			if(i%3 == 0){
				ob.setaBoolean(null);
			}
			if(i%4 == 0){
				ob.setaString(null);
			}
			if(i%5 == 0){
				ob.setaLong(null);
			}
			data.add(ob);
		}
		return data;
	}

}
