package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;
import org.sagebionetworks.repo.model.util.RandomAnnotationsUtil;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

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

	@Test
	public void testGetStorageLocations() throws JSONObjectAdapterException, IOException {
		byte[] bytes = loadFileAsBytes("annotations_blob_syn313805");
		NamedAnnotations namedAnnos = JDOSecondaryPropertyUtils.decompressedAnnotations(bytes);
		StorageLocations sl = JDOSecondaryPropertyUtils.getStorageLocations(namedAnnos, 111L, 333L);
		assertEquals(111L, sl.getNodeId().longValue());
		assertEquals(333L, sl.getUserId().longValue());
		assertEquals(0, sl.getLocations().size());
		assertEquals(2, sl.getAttachments().size());
		assertEquals("application/pdf", sl.getAttachments().get(0).getContentType());
		assertEquals("9a1e40545fe47f40bd2ede38b6623620", sl.getAttachments().get(0).getMd5());
		assertEquals("673014/Protecting-Human-Subject-Research-Participants.pdf", sl.getAttachments().get(0).getTokenId());
		assertNull(sl.getAttachments().get(0).getUrl());
		assertEquals("image/png", sl.getAttachments().get(1).getContentType());
		assertEquals("34fbabc6cf229cba16a900e8f54e6402", sl.getAttachments().get(1).getMd5());
		assertEquals("1047896/China.png", sl.getAttachments().get(1).getTokenId());
		assertNull(sl.getAttachments().get(1).getUrl());
		bytes = loadFileAsBytes("annotations_blob_syn464184");
		namedAnnos = JDOSecondaryPropertyUtils.decompressedAnnotations(bytes);
		sl = JDOSecondaryPropertyUtils.getStorageLocations(namedAnnos, 111L, 333L);
		assertEquals(111L, sl.getNodeId().longValue());
		assertEquals(333L, sl.getUserId().longValue());
		assertEquals(0, sl.getAttachments().size());
		assertEquals(1, sl.getLocations().size());
		assertEquals("/464184/1048109/Chile.png", sl.getLocations().get(0).getPath());
		assertEquals(LocationTypeNames.awss3, sl.getLocations().get(0).getType());
		Map<String, List<String>> annos = sl.getStrAnnotations();
		assertNotNull(annos.get("md5"));
		assertEquals("d95da947125d54dc90b1dc7f4665bdb6", annos.get("md5").get(0));
		assertNotNull(annos.get("contentType"));
		assertEquals("image/png", annos.get("contentType").get(0));
	}

	/**
	 * Helper to load a file into a byte[]
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	private static byte[] loadFileAsBytes(String fileName) throws IOException{
		// First get the input stream from the class loader
		InputStream in = JDOSecondaryPropertyUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find:"+fileName+" on the classpath", in);
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BufferedInputStream buffIn = new BufferedInputStream(in);
			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];
			int count;
			while((count = buffIn.read(buffer, 0, bufferSize))!= -1){
				out.write(buffer, 0, count);
			}
			return out.toByteArray();
		}finally{
			in.close();
		}
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
