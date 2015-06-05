package org.sagebionetworks.util.csv;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class CsvNullReaderTest {
	Object[] testCases = {
		"", Lists.newArrayList(),
		"\n", Lists.newArrayList(),
		"\n\n", Lists.newArrayList(new String[][] { new String[] { null } }),
		"a,b", Lists.newArrayList(new String[][] { new String[] { "a", "b" } }),
		"a,b\n", Lists.newArrayList(new String[][] { new String[] { "a", "b" } }),
		"a,b\n\n", Lists.newArrayList(new String[][] { new String[] { "a", "b" } }),
		"a,b\n\n\n", Lists.newArrayList(new String[][] { new String[] { "a", "b" }, new String[] { null } }),
		"a,b\n\n\n\n", Lists.newArrayList(new String[][] { new String[] { "a", "b" }, new String[] { null }, new String[] { null } }),
		"a,b\nc,d", Lists.newArrayList(new String[][] { new String[] { "a", "b" }, new String[] { "c", "d" } }),
		"a,b\nc,d\n", Lists.newArrayList(new String[][] { new String[] { "a", "b" }, new String[] { "c", "d" } }),
		"a,b\n\nc,d", Lists.newArrayList(new String[][] { new String[] { "a", "b" }, new String[] { null }, new String[] { "c", "d" } }),
		"a,b\n\n\nc,d", Lists.newArrayList(new String[][] { new String[] { "a", "b" }, new String[] { null }, new String[] { null }, new String[] { "c", "d" } }),
		"a\n\nc\n", Lists.newArrayList(new String[][] { new String[] { "a" }, new String[] { null }, new String[] { "c" } })
	};

	@Test
	public void testEmpty() throws Exception {
		for (int i = 0; i < testCases.length; i += 2) {
			String input = (String) testCases[i];
			List<String[]> expected = (List<String[]>) testCases[i + 1];
			CsvNullReader reader = new CsvNullReader(new StringReader(input));
			List<String[]> result = reader.readAll();
			reader.close();
			assertEquals("for '" + input + "'", expected.size(), result.size());
			for (int j = 0; j < expected.size(); j++) {
				assertArrayEquals(expected.get(j), result.get(j));
			}
		}
	}
}
