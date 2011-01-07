package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sage.datamodel.gaejdo.AccessorFactoryImpl;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;


public class DatasetTest {
    private final LocalServiceTestHelper helper = 
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()); 
    
    @BeforeClass
    public static void beforeClass() throws Exception {
    	// from http://groups.google.com/group/google-appengine-java/browse_thread/thread/96baed75e3c30a58/00d5afb2e0445882?lnk=gst&q=DataNucleus+plugin#00d5afb2e0445882  
    	//This one caused all the WARNING and SEVERE logs about eclipse UI  elements 
        Logger.getLogger("DataNucleus.Plugin").setLevel(Level.OFF); 
        //This one logged the last couple INFOs about Persistence configuration 
        Logger.getLogger("DataNucleus.Persistence").setLevel(Level.WARNING); 
    }


    private AccessorFactory fac;
	private Dataset dataset;

	@Before
	public void setUp() throws Exception {
        helper.setUp(); 
		fac = new AccessorFactoryImpl();
	}

	@After
	public void tearDown() throws Exception {
		if (fac!=null && dataset!=null) {
			fac.getDatasetAccessor().delete(dataset);
			fac.close();
			dataset = null;
		}
		helper.tearDown();
	}
	
	
	
	@Test
	public void testCreateandRetrieve() throws Exception {
		// create a new project
		Dataset dataset = new Dataset();
		dataset.setName("dataset name");
		dataset.setDescription("description");
		String overview = "This dataset is a megacross, and includes genotyoping data.";
		dataset.setOverview(new Text(overview));
		Date release = new Date();
		dataset.setReleaseDate(release);
		dataset.setStatus(Dataset.DatasetStatus.IN_PROGRESS);
		List<String> contributors = Arrays.asList(new String[]{"Larry", "Curly", "Moe"});
		dataset.setContributors(contributors);
		dataset.setDownloadable(true);
		
		Collection<InputDataLayer> layers = new HashSet<InputDataLayer>();
		dataset.setInputLayers(layers);
		InputDataLayer idl = new InputDataLayer();
		idl.setType(InputDataLayer.DataType.EXPRESSION);
		idl.setRevision(new Revision<DatasetLayer>());
		layers.add(idl);
		
		Revision<Dataset> r = new Revision<Dataset>();
		r.setVersion(new Version("1.0.0"));
		dataset.setRevision(r);
		
		// persist it
		DatasetAccessor da = fac.getDatasetAccessor();
		da.makePersistent(dataset);
		this.dataset=dataset;
		
		// persisting creates a Key, which we can grab
		Key id = dataset.getId();
		Assert.assertNotNull(id);
		
		// now retrieve the object by its key
		Dataset d2 = da.getDataset(id);
		Assert.assertNotNull(d2);
		
		// check that all the fields were persisted
		Assert.assertEquals("dataset name", d2.getName());
		Assert.assertEquals("description", d2.getDescription());
		Assert.assertEquals(overview, d2.getOverview().getValue());
		Assert.assertEquals(release, d2.getReleaseDate());
		Assert.assertEquals(Dataset.DatasetStatus.IN_PROGRESS, d2.getStatus());
		Assert.assertEquals(contributors, d2.getContributors());
		Assert.assertEquals(true, d2.isDownloadable());
		Assert.assertEquals(new Version("1.0.0"), d2.getRevision().getVersion());
		
		Collection<InputDataLayer> l2 = d2.getInputLayers();
		Assert.assertEquals(1, l2.size());
		DatasetLayer dl = l2.iterator().next();
		Assert.assertTrue((dl instanceof InputDataLayer));
		InputDataLayer.DataType type = ((InputDataLayer)dl).getType();
		Assert.assertEquals(InputDataLayer.DataType.EXPRESSION, type);
	}

}
