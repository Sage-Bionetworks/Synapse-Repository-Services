package org.sagebionetworks.repo.model.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
public class MigrationUtilsTest {
	
	@Test
	public void testBucketByTreeLevel(){
		// the first long is the ID the second long is the parent ID.
		List<RowMetadata> input = buildList(new Long[][]{
				//level zero
				new Long[]{4l,null},
				new Long[]{5l,null},
				// this is level zero since its parent is not in the list.
				new Long[]{25l,30l},
				// Level one
				new Long[]{2l,4l},
				new Long[]{6l,5l},
				new Long[]{7l,5l},
				new Long[]{8l,5l},
				// Level two
				new Long[]{3l,2l},
				new Long[]{9l,6l},
				// Level three
				new Long[]{10l,3l},
				// Theses should also be on level two
				new Long[]{12l,6l},
				new Long[]{13l,6l},
				new Long[]{14l,6l},
		});
		
		// Sort to put them in the same order as they will be during migration
		Collections.sort(input, new Comparator<RowMetadata>() {
			@Override
			public int compare(RowMetadata o1, RowMetadata o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
		System.out.println(input);
		// bucket
		ListBucketProvider provider = new ListBucketProvider();
		MigrationUtils.bucketByTreeLevel(input.iterator(), provider);
		// Check the results

		List<List<Long>> results = provider.getListOfBuckets();
		assertNotNull(results);
		assertEquals("Expected 4 buckets for this test, one for each level", 4, results.size());
		// Check the level zero
		List<Long> expected = Arrays.asList(4l,5l,25l);
		assertEquals(expected, results.get(0));
		// level one
		expected = Arrays.asList(2l,6l,7l,8l);
		assertEquals(expected, results.get(1));
		// Level two
		expected = Arrays.asList(3l,9l,12l,13l,14l);
		assertEquals(expected, results.get(2));
		// Level three
		expected = Arrays.asList(10l);
		assertEquals(expected, results.get(3));
	}

	/**
	 * Buildup a list from an simple array. The first long is the id the second long
	 * is the parent id.
	 * @param data
	 * @return
	 */
	List<RowMetadata> buildList(Long[][] data){
		List<RowMetadata> list = new LinkedList<RowMetadata>();
		for(int i=0; i<data.length; i++){
			RowMetadata row = new RowMetadata();
			row.setId(data[i][0]);
			row.setParentId(data[i][1]);
			list.add(row);
		}
		return list;
	}
	
}
