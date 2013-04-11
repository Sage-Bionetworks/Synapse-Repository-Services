package org.sagebionetworks.doi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class EzidMetadataTest {

	private EzidMetadata metadata;
	private final String target = "http://eiqucmdjeisxkd.org/";
	private final String creator = "Last Name, First Name";
	private final String title = "Some test title";
	private final String publisher = "publisher";
	private final int year = Calendar.getInstance().get(Calendar.YEAR);

	@Before
	public void before() {
		metadata = new EzidMetadata();
		metadata.setTarget(target);
		metadata.setCreator(creator);
		metadata.setTitle(title);
		metadata.setPublisher(publisher);
		metadata.setPublicationYear(year);
	}

	@Test
	public void testGetMetadataAsString() throws Exception {
		String plainText = metadata.getMetadataAsString();
		assertNotNull(plainText);
		Map<String, String> map = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new StringReader(plainText));
		String line = reader.readLine();
		while (line != null) {
			String[] splits = line.split(":");
			assertNotNull(splits);
			assertEquals(2, splits.length);
			map.put(splits[0].trim(), splits[1].trim());
			line = reader.readLine();
		}
		reader.close();
		assertEquals(5, map.entrySet().size());
		assertEquals(URLEncoder.encode(title, "UTF-8"), map.get("datacite.title"));
		assertEquals(URLEncoder.encode(creator, "UTF-8"), map.get("datacite.creator"));
		assertEquals(URLEncoder.encode(publisher, "UTF-8"), map.get("datacite.publisher"));
		assertEquals(year, Integer.parseInt(map.get("datacite.publicationyear")));
		assertEquals(URLEncoder.encode(target, "UTF-8"), map.get("_target"));
	}

	@Test
	public void testInitFromString() throws Exception {
		String plainText = metadata.getMetadataAsString();
		plainText = plainText + "\r\n_status: public";
		EzidMetadata metadata = new EzidMetadata();
		metadata.initFromString(plainText);
		assertEquals(title, metadata.getTitle());
		assertEquals(creator, metadata.getCreator());
		assertEquals(publisher, metadata.getPublisher());
		assertEquals(year, metadata.getPublicationYear());
		assertEquals(target, metadata.getTarget());
	}

	@Test
	public void testGetSet() {
		assertEquals(title, metadata.getTitle());
		metadata.setTitle("title");
		assertEquals("title", metadata.getTitle());
		assertEquals(creator, metadata.getCreator());
		metadata.setCreator("creator");
		assertEquals("creator", metadata.getCreator());
		assertEquals(publisher, metadata.getPublisher());
		metadata.setPublisher("publisher");
		assertEquals("publisher", metadata.getPublisher());
		assertEquals(year, metadata.getPublicationYear());
		metadata.setPublicationYear(-300); // 300 BC
		assertEquals(-300, metadata.getPublicationYear());
		assertEquals(target, metadata.getTarget());
		metadata.setTarget("target");
		assertEquals("target", metadata.getTarget());
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetTitle() {
		EzidMetadata md = new EzidMetadata();
		md.setTitle(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetTitle() {
		EzidMetadata md = new EzidMetadata();
		md.getTitle();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetCreator() {
		EzidMetadata md = new EzidMetadata();
		md.setCreator(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetCreator() {
		EzidMetadata md = new EzidMetadata();
		md.getCreator();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetPublisher() {
		EzidMetadata md = new EzidMetadata();
		md.setPublisher(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetPublisher() {
		EzidMetadata md = new EzidMetadata();
		md.getPublisher();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRequriedSetTarget() {
		EzidMetadata md = new EzidMetadata();
		md.setTarget(null);
	}

	@Test(expected=NullPointerException.class)
	public void testRequriedGetTarget() {
		EzidMetadata md = new EzidMetadata();
		md.getTarget();
	}
}
