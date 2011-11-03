package org.sagebionetworks.web.util;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.web.shared.LayerLink;

public class RandomDataset {
	
	private static final Random rand = new Random(12345);
	// The milliseconds in a day.
	private static final int DAY_MS = 1000*60*60*24;
	// The milliseconds in a day.
	private static final int ONE_YEAR_MS = DAY_MS*365;
	// All ids are issued from this sequence
	private static int idSequence = 0;
	
	/**
	 * Create a randomly generated dataset.
	 * @return
	 */
	public static Dataset createRandomDataset(){
		Dataset c = new Dataset();
		long now = System.currentTimeMillis();
		c.setId(""+idSequence++);
		// Created sometime within the last year.
		long created = now - rand.nextInt(ONE_YEAR_MS);
		// Released within 5 days of creation.
		long release = created + rand.nextInt(DAY_MS*5);
		c.setCreatedOn(new Date(created));
		c.setReleaseDate(new Date(created));
		c.setName(RandomStrings.generateRandomString(2, 15));
		c.setCreatedBy(RandomStrings.generateRandomString(2, 15));
		int descWordCount = rand.nextInt(25)+5;
		c.setDescription(RandomStrings.generateRandomString(descWordCount, 15));
		// Create the layers
		int numLayers = rand.nextInt(5);
		List<LayerLink> layers = new LinkedList<LayerLink>();
		LayerLink.Type[] types = LayerLink.Type.values();
		for(int i=0; i<numLayers; i++){
			int typeIndex = rand.nextInt(types.length);
			layers.add(new LayerLink(""+idSequence++, types[typeIndex], RandomStrings.generateRandomUrl(4, 8)));
			switch(types[typeIndex]) {
			case G:
				c.setHasGeneticData(true);
				break;
			case C:
				c.setHasClinicalData(true);
				break;
			case E:
				c.setHasExpressionData(true);
				break;
			}
		}
//		c.setLayerPreviews(layers);
		c.setStatus(RandomStrings.generateRandomString(1, 10));
		c.setVersion("1.0."+rand.nextInt(9));
		
		return c;
	}
	
	public static String nextSequenceId(){
		// Increment and return
		idSequence++;
		return ""+idSequence;
	}

}
