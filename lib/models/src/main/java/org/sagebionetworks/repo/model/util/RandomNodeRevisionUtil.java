package org.sagebionetworks.repo.model.util;

import java.util.Random;

import org.sagebionetworks.repo.model.NodeRevision;

public class RandomNodeRevisionUtil {
	
	/**
	 * Generate a random node revision using the passed seed.
	 * @param seed
	 * @return
	 */
	public static NodeRevision generateRandom(long seed, int annCount){
		Random rand = new Random(seed);
		return generateRandom(rand, annCount);
	}

	/**
	 * Generate a random node revision using the passed random object.
	 * @param annCount
	 * @param rand
	 * @return
	 */
	public static NodeRevision generateRandom(Random rand, int annCount) {
		NodeRevision rev = new NodeRevision();
		rev.setNodeId(""+rand.nextLong());
		rev.setComment("Comment: "+rand.nextLong());
		rev.setModifiedBy("modifiedBy: "+rand.nextLong());
		rev.setModifiedOn(RandomUtils.createRandomStableDate(rand));
		rev.setRevisionNumber(rand.nextLong());
		rev.setLabel("Label: "+rand.nextLong());
		rev.setAnnotations(RandomAnnotationsUtil.generateRandom(rand, annCount));
		return rev;
	}

}
