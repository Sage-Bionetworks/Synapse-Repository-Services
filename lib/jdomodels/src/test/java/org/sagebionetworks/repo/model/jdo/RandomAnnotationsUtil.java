package org.sagebionetworks.repo.model.jdo;

import java.util.Date;
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
			annos.addAnnotation(key, new Date(rand.nextLong()));
			annos.addAnnotation(key, new Date(rand.nextLong()));
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
			byte[] blob = createRandomBlob(rand, 1024);
			annos.addAnnotation(key, blob);
			blob = createRandomBlob(rand, 1024);
			annos.addAnnotation(key, blob);
		}
		return annos;
	}
	
	/**
	 * Create a random blob using the maximum size.  Note: The size of the blob 
	 * is random and will be between 1 and max.
	 * @param rand
	 * @param maxSize
	 * @return
	 */
	public static byte[] createRandomBlob(Random rand, int maxSize){
		int size = rand.nextInt(maxSize);
		// Exclude zero
		size++;
		byte[] array = new byte[size];
		rand.nextBytes(array);
		return array;
	}

}
