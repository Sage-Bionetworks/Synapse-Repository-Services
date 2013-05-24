package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;

/**
 * Basic test for converting between JDOs and DTOs.
 * 
 * @author jmhill
 *
 */
@SuppressWarnings("unchecked")
public class JDOSecondaryPropertyUtilsTest {
	
	
	DBONode owner;
	
	@Before
	public void before(){
		// Each test starts with a new owner
		owner = new DBONode();
	}
	

	
	@Test
	public void testCompression() throws IOException{
		NamedAnnotations named = new NamedAnnotations();
		Annotations dto = named.getAdditionalAnnotations();
		dto.addAnnotation("stringOne", "one");
		dto.addAnnotation("StringTwo", "3");
		dto.addAnnotation("longOne", new Long(324));
		dto.addAnnotation("doubleOne", new Double(32.4));
		dto.addAnnotation("dateOne", new Date(System.currentTimeMillis()));
		
		byte[] compressed = JDOSecondaryPropertyUtils.compressAnnotations(named);
		String xmlString = JDOSecondaryPropertyUtils.toXml(named);
		System.out.println(xmlString);
		NamedAnnotations mapClone = JDOSecondaryPropertyUtils.fromXml(xmlString);
		assertEquals(named, mapClone);
		assertNotNull(compressed);
		System.out.println("Size: "+compressed.length);
		System.out.println(new String(compressed, "UTF-8"));
		// Now make sure we can read the compressed data
		NamedAnnotations dtoCopy = JDOSecondaryPropertyUtils.decompressedAnnotations(compressed);
		assertNotNull(dtoCopy);
		// The copy should match the original
		assertEquals(named, dtoCopy);
	}
	

	@Test
	public void testNullBlob() throws Exception{
		// Create a revision with a null byte array
		DBORevision rev = new DBORevision();
		// Check the copy
		NamedAnnotations dtoCopy = JDOSecondaryPropertyUtils.createFromJDO(rev);
		assertNotNull(dtoCopy);
	}
	
	@Test
	public void testBlobCompression() throws IOException{
		NamedAnnotations named = new NamedAnnotations();
		Annotations dto = named.getAdditionalAnnotations();
		String[] values = new String[]{
				"I am the first blob in this set",
				"I am the second blob in this set with the same key as the first",
				"I am the thrid in the set with my own key"
		};
		dto.addAnnotation("blobOne", values[0].getBytes("UTF-8"));
		dto.addAnnotation("blobOne", values[1].getBytes("UTF-8"));
		dto.addAnnotation("blobTwo", values[2].getBytes("UTF-8"));
		byte[] comressedBytes = JDOSecondaryPropertyUtils.compressAnnotations(named);
		System.out.println("Compressed size: "+comressedBytes.length);
		assertNotNull(comressedBytes);
		// Make the round trip
		NamedAnnotations namedClone = JDOSecondaryPropertyUtils.decompressedAnnotations(comressedBytes);
		assertNotNull(namedClone);
		Annotations annos = namedClone.getAdditionalAnnotations();
		assertNotNull(annos);
		assertNotNull(annos.getBlobAnnotations());
		assertEquals(2, annos.getBlobAnnotations().size());
		Collection<byte[]> first = annos.getBlobAnnotations().get("blobOne");
		assertNotNull(first);
		assertEquals(2, first.size());
		Iterator<byte[]> it = first.iterator();
		assertEquals(values[0], new String(it.next(), "UTF-8"));
		assertEquals(values[1], new String(it.next(), "UTF-8"));
		
		Collection<byte[]> second = annos.getBlobAnnotations().get("blobTwo");
		assertNotNull(second);
		assertEquals(1, second.size());
		assertEquals(values[2], new String(second.iterator().next(), "UTF-8"));

	}
	
	/**
	 * Test for adding all values to strings.
	 */
	@Test
	public void testAddAllToString(){
		Annotations annos = new Annotations();
		annos.addAnnotation("one", "a");
		annos.addAnnotation("one", "b");
		long now = System.currentTimeMillis();
		annos.addAnnotation("date", new Date(now));
		annos.addAnnotation("double", new Double(123.5));
		annos.addAnnotation("long", new Long(999));
		// These conflict with the string
		annos.addAnnotation("one", new Long(45));
		// add some nulls
		annos.getLongAnnotations().put("nullLong", null);
		annos.getLongAnnotations().put("emptyLong", new ArrayList<Long>());
		List<Long> list = new ArrayList<Long>();
		list.add(null);
		annos.getLongAnnotations().put("nullValue", list);
		// Add them all to strings
		JDOSecondaryPropertyUtils.addAllToStrings(annos);
		List<String> values = annos.getStringAnnotations().get("one");
		assertEquals(3, values.size());
		assertEquals("a", values.get(0));
		assertEquals("b", values.get(1));
		assertEquals("45", values.get(2));
		// Date
		values = annos.getStringAnnotations().get("date");
		assertEquals(1, values.size());
		assertEquals(Long.toString(now), values.get(0));
		// Double
		values = annos.getStringAnnotations().get("double");
		assertEquals(1, values.size());
		assertEquals(Double.toString(123.5), values.get(0));
		// Long
		values = annos.getStringAnnotations().get("long");
		assertEquals(1, values.size());
		assertEquals(Long.toString(999), values.get(0));
		// the values should still be in their original location as well
		List<Long> longValues = annos.getLongAnnotations().get("long");
		assertEquals(1, longValues.size());
		assertEquals(new Long(999), longValues.get(0));

	}

	/**
	 * This main method is used to create a blob of the current version of annotations.
	 * Each time we change the annotations object, we should create a new version and add it to the
	 * files to test.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{	
		// There should be three args
		if(args == null || args.length != 3) throw new IllegalArgumentException("This utility requires three arguments: 0=filname, 1=randomSeed, 2=count");
		String name = args[0];
		long seed;
		int count;
		try{
			seed = Long.parseLong(args[1]);
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("The second argument should be a long representing the random seed to use.", e);
		}
		try{
			count = Integer.parseInt(args[2]);
		}catch(NumberFormatException e){
			throw new IllegalArgumentException("The thrid argument should be the number of annotations to used", e);
		}
		// First generate the random annotations to be used to create the compresssed blob fil.
		Annotations annos = RandomAnnotationsUtil.generateRandom(seed, count);
		NamedAnnotations named = new NamedAnnotations();
		named.put("newType", annos);

		// Now create the output file
		File outputFile = new File("src/test/resources/"+name);
		System.out.println("Creating file: "+outputFile.getAbsolutePath());
		if(outputFile.exists()){
			outputFile.delete();
		}
		outputFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(outputFile);
		try{
			// First create the blob
			byte[] compressedBlob = JDOSecondaryPropertyUtils.compressAnnotations(named);
			System.out.println("Compressed file size is: "+compressedBlob.length+" bytes");
						
			// Write this blob to the file
			BufferedOutputStream buffer = new BufferedOutputStream(fos);
			buffer.write(compressedBlob);
			buffer.flush();
			fos.flush();
		}finally{
			fos.close();
		}
		
	}
	
	@Test
	public void testPrepareAnnotationsForDBReplacement(){
		NamedAnnotations namedAnnos = new NamedAnnotations();
		namedAnnos.getPrimaryAnnotations().addAnnotation("string", "a");
		namedAnnos.getPrimaryAnnotations().addAnnotation("string", "b");
		namedAnnos.getPrimaryAnnotations().addAnnotation("long", new Long(123));
		namedAnnos.getPrimaryAnnotations().addAnnotation("long", new Long(456));
		namedAnnos.getAdditionalAnnotations().addAnnotation("addString", "c");
		Annotations forDb = JDOSecondaryPropertyUtils.prepareAnnotationsForDBReplacement(namedAnnos, "9810");
		assertNotNull(forDb);
		assertEquals("9810", forDb.getId());
		// the primary and secondary should be merged
		List<String> strings = forDb.getStringAnnotations().get("string");
		assertEquals(2, strings.size());
		assertEquals("a", strings.get(0));
		assertEquals("b", strings.get(1));
		// Did additional property get merged?
		strings = forDb.getStringAnnotations().get("addString");
		assertEquals(1, strings.size());
		assertEquals("c", strings.get(0));
		// Are all longs put in the string.
		strings = forDb.getStringAnnotations().get("long");
		assertEquals(2, strings.size());
		assertEquals("123", strings.get(0));
		assertEquals("456", strings.get(1));

	}
	
	@Test
	public void testBuildDistinctMap(){
		// The input list
		Map<String, List<Long>> start = new HashMap<String, List<Long>>();
		start.put("nullList", null);
		start.put("emptyList", new LinkedList<Long>());
		List<Long> list = new LinkedList<Long>();
		list.add(1l);
		start.put("single", list);
		list = new LinkedList<Long>();
		list.add(1l);
		list.add(2l);
		start.put("multiple", list);
		list = new LinkedList<Long>();
		list.add(333l);
		list.add(333l);
		start.put("duplicates", list);
		
		// This is the expected results
		Map<String, List<Long>> expected = new HashMap<String, List<Long>>();
		list = new LinkedList<Long>();
		list.add(1l);
		expected.put("single", list);
		list = new LinkedList<Long>();
		list.add(1l);
		list.add(2l);
		expected.put("multiple", list);
		list = new LinkedList<Long>();
		list.add(333l);
		expected.put("duplicates", list);
		
		Map<String, List<Long>> distinct = JDOSecondaryPropertyUtils.buildDistinctMap(start);
		assertEquals(expected, distinct);
	}
	
	@Test
	public void testBuildDistinctAnnotations(){
		// Add a duplicate of each type
		Annotations start = new Annotations();
		start.addAnnotation("string", "one");
		start.addAnnotation("string", "one");
		start.addAnnotation("date", new Date(1l));
		start.addAnnotation("date", new Date(1l));
		start.addAnnotation("long", 123l);
		start.addAnnotation("long", 123l);
		start.addAnnotation("double", 123.0);
		start.addAnnotation("double", 123.0);
		
		Annotations expected = new Annotations();
		expected.addAnnotation("string", "one");
		expected.addAnnotation("date", new Date(1l));
		expected.addAnnotation("long", 123l);
		expected.addAnnotation("double", 123.0);
		
		Annotations distinct = JDOSecondaryPropertyUtils.buildDistinctAnnotations(start);
		assertEquals(expected, distinct);
	}
	
	/**
	 * Simple data structure for holding blob file data.
	 * 
	 * @author jmhill
	 *
	 */
	private static class BlobData{
		
		String fileName;
		long randomSeed;
		int count;
		
		
		public BlobData(String fileName, long randomSeed, int count) {
			super();
			this.fileName = fileName;
			this.randomSeed = randomSeed;
			this.count = count;
		}
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		public long getRandomSeed() {
			return randomSeed;
		}
		public void setRandomSeed(long randomSeed) {
			this.randomSeed = randomSeed;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		@Override
		public String toString() {
			return "BlobData [fileName=" + fileName + ", randomSeed="
					+ randomSeed + ", count=" + count + "]";
		}
		
	}

}
