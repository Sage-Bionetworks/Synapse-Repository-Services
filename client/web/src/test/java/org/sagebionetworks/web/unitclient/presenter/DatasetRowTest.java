package org.sagebionetworks.web.unitclient.presenter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.web.client.presenter.DatasetRow;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.LayerLink;

/**
 * 
 * @author jmhill
 *
 */
public class DatasetRowTest {
	
	@Test
	public void testMaskNoLayers(){
		// Create a 
		Dataset dataset = new Dataset();
		DatasetRow row = new DatasetRow(dataset);
		assertEquals(0x0, row.getLayersMask());
	
	}
	
	@Test
	public void testMaskDuplicate(){
		// Create a 
		Dataset dataset = new Dataset();
		List<LayerLink> layerList = new ArrayList<LayerLink>();
		layerList.add(new LayerLink("0", LayerLink.Type.C, "url"));
		layerList.add(new LayerLink("1", LayerLink.Type.C, "url"));
		layerList.add(new LayerLink("2", LayerLink.Type.C, "url"));
		dataset.setLayers(layerList);
		DatasetRow row = new DatasetRow(dataset);
		assertEquals(LayerLink.Type.C.getMask(), row.getLayersMask());
	
	}
	
	@Test
	public void testMaskTwo(){
		// Create a 
		Dataset dataset = new Dataset();
		List<LayerLink> layerList = new ArrayList<LayerLink>();
		layerList.add(new LayerLink("0", LayerLink.Type.C, "url"));
		layerList.add(new LayerLink("1", LayerLink.Type.E, "url"));
		layerList.add(new LayerLink("2",LayerLink.Type.C, "url"));
		dataset.setLayers(layerList);
		DatasetRow row = new DatasetRow(dataset);
		assertEquals((LayerLink.Type.C.getMask() | LayerLink.Type.E.getMask()), row.getLayersMask());
	
	}

	@Test
	public void testMaskThree(){
		// Create a 
		Dataset dataset = new Dataset();
		List<LayerLink> layerList = new ArrayList<LayerLink>();
		layerList.add(new LayerLink("0", LayerLink.Type.C, "url"));
		layerList.add(new LayerLink("1", LayerLink.Type.E, "url"));
		layerList.add(new LayerLink("2", LayerLink.Type.G, "url"));
		dataset.setLayers(layerList);
		DatasetRow row = new DatasetRow(dataset);
		assertEquals((LayerLink.Type.C.getMask() | LayerLink.Type.E.getMask() | LayerLink.Type.G.getMask())  , row.getLayersMask());
	
	}
	
}
