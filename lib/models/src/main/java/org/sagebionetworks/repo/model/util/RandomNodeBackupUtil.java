package org.sagebionetworks.repo.model.util;

import java.util.ArrayList;
import java.util.Random;

import org.sagebionetworks.repo.model.NodeBackup;

/**
 * A utility to create a randomly generated NodeBackup object.
 * 
 * @author jmhill
 *
 */
public class RandomNodeBackupUtil {

	/**
	 * Generate a random NodeBackup from a seed.
	 * @param seed
	 * @return
	 */
	public static NodeBackup generateRandome(long seed){
		Random rand = new Random(seed);
		NodeBackup nb = new NodeBackup();
		nb.setAcl(RandomAccessControlListUtil.generateRandom(rand));
		nb.setNode(RandomNodeUtil.generateRandom(rand));
		nb.setBenefactor(""+rand.nextLong());
		nb.setChildren(new ArrayList<String>());
		nb.setRevisions(new ArrayList<Long>());
		int count = rand.nextInt(100);
		for(int i=0; i<count; i++){
			nb.getChildren().add(""+rand.nextLong());
		}
		count = rand.nextInt(100);
		for(int i=0; i<count; i++){
			nb.getRevisions().add(rand.nextLong());
		}
		return nb;
	}
}
