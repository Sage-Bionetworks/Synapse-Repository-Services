package org.sagebionetworks.repo.model.util;

import java.util.Random;

import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeRevisionBackup;

public class RandomNodeRevisionUtil {
	
	/**
	 * Generate a random node revision using the passed seed.
	 * @param seed
	 * @return
	 */
	public static NodeRevisionBackup generateRandom(long seed, int annCount){
		Random rand = new Random(seed);
		return generateRandom(rand, annCount);
	}

	/**
	 * Generate a random node revision using the passed random object.
	 * @param annCount
	 * @param rand
	 * @return
	 */
	public static NodeRevisionBackup generateRandom(Random rand, int annCount) {
		NodeRevisionBackup rev = new NodeRevisionBackup();
		rev.setNodeId(""+rand.nextLong());
		rev.setComment("Comment: "+rand.nextLong());
		rev.setModifiedBy("modifiedBy: "+rand.nextLong());
		rev.setModifiedOn(RandomUtils.createRandomStableDate(rand));
		rev.setRevisionNumber(rand.nextLong());
		rev.setLabel("Label: "+rand.nextLong());
//		rev.setAnnotations(RandomAnnotationsUtil.generateRandom(rand, annCount));
		NamedAnnotations named = new NamedAnnotations();
		named.put(NamedAnnotations.NAME_SPACE_PRIMARY, RandomAnnotationsUtil.generateRandom(rand, annCount));
		named.put(NamedAnnotations.NAME_SPACE_ADDITIONAL, RandomAnnotationsUtil.generateRandom(rand, annCount));
		rev.setNamedAnnotations(named);
		return rev;
	}

}
