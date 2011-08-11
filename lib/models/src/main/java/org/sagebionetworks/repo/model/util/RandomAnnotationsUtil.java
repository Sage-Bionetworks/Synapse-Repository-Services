package org.sagebionetworks.repo.model.util;

import java.util.Random;

import org.sagebionetworks.repo.model.Annotations;

/**
 * Utility for creating an Annotations object populated with random data.
 * @author jmhill
 *
 */
public class RandomAnnotationsUtil {
	
	/**
	 * Create an populate annotations using the passed seed and count.
	 * Note: for each count there will be two entries for each annotation type.
	 * @param seed
	 * @param count
	 * @return
	 */
	public static Annotations generateRandom(long seed, int count){
		// The first argument should the file to create.
		Random rand = new Random(seed);
		return generateRandom(rand, count);
	}

	/**
	 * Create an populate annotations using the passed seed and count.
	 * Note: for each count there will be two entries for each annotation type.
	 * @param count
	 * @param rand
	 * @return
	 */
	public static Annotations generateRandom(Random rand, int count) {
		Annotations annos = new Annotations();
		// Fill up the annotations object
		// Strings
		for(int i=0; i<count; i++){
			String key = "stringKey"+i;
			annos.addAnnotation(key, "some string: "+rand.nextLong());
			annos.addAnnotation(key, "some string: "+rand.nextLong());
		}
		// Booleans
		for(int i=0; i<count; i++){
			String key = "booleanKey"+i;
			annos.addAnnotation(key, ""+rand.nextBoolean());
			annos.addAnnotation(key, ""+rand.nextBoolean());
		}
		// Dates
		for(int i=0; i<count; i++){
			String key = "dateKey"+i;
			annos.addAnnotation(key, RandomUtils.createRandomStableDate(rand));
			annos.addAnnotation(key, RandomUtils.createRandomStableDate(rand));
		}
		// Longs
		for(int i=0; i<count; i++){
			String key = "longKey"+i;
			annos.addAnnotation(key, rand.nextLong());
			annos.addAnnotation(key, rand.nextLong());
		}
		// Doubles
		for(int i=0; i<count; i++){
			String key = "doubleKey"+i;
			annos.addAnnotation(key, rand.nextDouble());
			annos.addAnnotation(key, rand.nextDouble());
		}
		// Blobs
		for(int i=0; i<count; i++){
			String key = "blobKey"+i;
			// Create  
			byte[] blob = RandomUtils.createRandomBlob(rand, 1024);
			annos.addAnnotation(key, blob);
			blob = RandomUtils.createRandomBlob(rand, 1024);
			annos.addAnnotation(key, blob);
		}
		return annos;
	}
	


}
