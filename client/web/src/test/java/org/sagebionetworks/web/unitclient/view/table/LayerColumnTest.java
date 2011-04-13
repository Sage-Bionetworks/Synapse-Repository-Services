package org.sagebionetworks.web.unitclient.view.table;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.ImagePrototypeSingleton;
import org.sagebionetworks.web.client.view.table.LayerColumn;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.LayerColumnInfo;

public class LayerColumnTest {
	// These are the mock strings that will be rendered.
	String geneExpression = "geneExpression16";
	String genotype = "genotype16";
	String phenoType = "phenotypes16";
	String transparent = "transparent16";
	ImagePrototypeSingleton mockProtytype;
	
	// These are the Ids for the three base columns
	String hasExpresionId = "hasExpresionId";
	String hasGeneticId = "hasGeneticId";
	String hasClinicalId = "hasClinicalId";
	
	ColumnInfo hasExpresion;
	ColumnInfo hasGenetic;
	ColumnInfo hasClinical;
	
	LayerColumnInfo layerInfo;
	// The is the object we are testing
	LayerColumn column;
	
	@Before
	public void setup(){
		
		mockProtytype = Mockito.mock(ImagePrototypeSingleton.class);
		when(mockProtytype.getIconGeneExpression16()).thenReturn(geneExpression);
		when(mockProtytype.getIconGenotype16()).thenReturn(genotype);
		when(mockProtytype.getIconPhenotypes16()).thenReturn(phenoType);
		when(mockProtytype.getIconTransparent16Html()).thenReturn(transparent);
		// Create the three columns types
		hasExpresion = new ColumnInfo(hasExpresionId, ColumnInfo.Type.String.name(), "SomeDisplay", "SomeDescription");
		hasGenetic = new ColumnInfo(hasGeneticId, ColumnInfo.Type.String.name(), "Genetic", "Desc");
		hasClinical = new ColumnInfo(hasClinicalId, ColumnInfo.Type.String.name(), "Clinical", "Desc2");
		
		// Build up the metadata used for the test.
		layerInfo = new LayerColumnInfo();
		layerInfo.setId("LayerId");
		layerInfo.setDisplayName("Layer");
		layerInfo.setHasExpression(hasExpresion);
		layerInfo.setHasGenetic(hasGenetic);
		layerInfo.setHasClinical(hasClinical);
	
		column = new LayerColumn(mockProtytype);
		column.setLayerColumnInfo(layerInfo);
		
		
	}
	
	@Test
	public void testAllFalse(){
		// The test row
		Map<String, Object> row = new TreeMap<String, Object>();
		row.put(hasExpresionId, Boolean.FALSE);
		row.put(hasGeneticId, Boolean.FALSE);
		row.put(hasClinicalId, Boolean.FALSE);
		String results = column.getValue(row);
		assertNotNull(results);
		assertTrue(results.contains(transparent));
		assertFalse(results.contains(geneExpression));
		assertFalse(results.contains(genotype));
		assertFalse(results.contains(phenoType));

	}
	@Test
	public void testClinical(){
		// The test row
		Map<String, Object> row = new TreeMap<String, Object>();
		row.put(hasExpresionId, Boolean.FALSE);
		row.put(hasGeneticId, Boolean.FALSE);
		row.put(hasClinicalId, Boolean.TRUE);
		String results = column.getValue(row);
		assertNotNull(results);
		assertTrue(results.contains(transparent));
		assertFalse(results.contains(geneExpression));
		assertFalse(results.contains(genotype));
		assertTrue(results.contains(phenoType));

	}
	
	@Test
	public void testExpresion(){
		// The test row
		Map<String, Object> row = new TreeMap<String, Object>();
		row.put(hasExpresionId, Boolean.TRUE);
		row.put(hasGeneticId, Boolean.FALSE);
		row.put(hasClinicalId, Boolean.FALSE);
		String results = column.getValue(row);
		assertNotNull(results);
		assertTrue(results.contains(transparent));
		assertTrue(results.contains(geneExpression));
		assertFalse(results.contains(genotype));
		assertFalse(results.contains(phenoType));
	}
	
	@Test
	public void testGenetic(){
		// The test row
		Map<String, Object> row = new TreeMap<String, Object>();
		row.put(hasExpresionId, Boolean.FALSE);
		row.put(hasGeneticId, Boolean.TRUE);
		row.put(hasClinicalId, Boolean.FALSE);
		String results = column.getValue(row);
		assertNotNull(results);
		assertTrue(results.contains(transparent));
		assertFalse(results.contains(geneExpression));
		assertTrue(results.contains(genotype));
		assertFalse(results.contains(phenoType));
	}
	
	@Test
	public void testAll(){
		// The test row
		Map<String, Object> row = new TreeMap<String, Object>();
		row.put(hasExpresionId, Boolean.TRUE);
		row.put(hasGeneticId, Boolean.TRUE);
		row.put(hasClinicalId, Boolean.TRUE);
		String results = column.getValue(row);
		assertNotNull(results);
		assertFalse(results.contains(transparent));
		assertTrue(results.contains(geneExpression));
		assertTrue(results.contains(genotype));
		assertTrue(results.contains(phenoType));
	}

}
