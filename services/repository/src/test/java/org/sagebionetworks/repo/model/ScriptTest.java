package org.sagebionetworks.repo.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOObjectNotFoundException;

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


public class ScriptTest {
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
		private Collection<Script> scripts;

		@Before
		public void setUp() throws Exception {
	        helper.setUp(); 
			fac = new AccessorFactoryImpl();
			scripts = new ArrayList<Script>();
		}

		@After
		public void tearDown() throws Exception {
			if (fac!=null) {
				for (Script script: scripts) {
					fac.getScriptAccessor().delete(script);
				}
				scripts.clear();
				fac.close();
			}
			helper.tearDown();
		}
		
		@Test
		public void testCreateandRetrieve() throws Exception {
			Script script = new Script();
			script.setName("script name");
			script.setRevision(new Revision<Script>());
			Date now = new Date();
			script.getRevision().setVersion(new Version("1.0"));
			script.getRevision().setRevisionDate(now);
			script.getRevision().setOriginal(null); // THIS is the original
			script.setPublicationDate(now);
			URI uri = new URI("https://sagebionetworks.com");
			script.setSource(uri);
			String overview = "This R script converts genotyping data into gene interaction networks.";
			script.setOverview(new Text(overview));
			
			// persist it
			ScriptAccessor sa = fac.getScriptAccessor();
			sa.makePersistent(script);
			scripts.add(script);
			
			// persisting creates a Key, which we can grab
			Key id = script.getId();
			Assert.assertNotNull(id);
			
			// now retrieve the object by its key
			Script s2 = sa.getScript(id);
			Assert.assertNotNull(s2);
			
			// check that all the fields were persisted
			Assert.assertEquals("script name", s2.getName());
			// the original version of script is itself
			Assert.assertEquals(null, script.getRevision().getOriginal());
			// the original of the retrieved script is also this script
			Assert.assertEquals(null, s2.getRevision().getOriginal());
			
			Assert.assertEquals(script.getRevision().getVersion(), s2.getRevision().getVersion());
			Assert.assertEquals(script.getRevision().getRevisionDate(), s2.getRevision().getRevisionDate());
			Assert.assertEquals(overview, s2.getOverview().getValue());
			Assert.assertEquals(uri, s2.getSource());
			Assert.assertEquals(now, s2.getPublicationDate());

		}
		@Test
		public void testCreateandDelete() throws Exception {
			Script script = new Script();
			script.setName("script name");
			script.setRevision(new Revision<Script>());

			// persist it
			ScriptAccessor sa = fac.getScriptAccessor();
			sa.makePersistent(script);
			scripts.add(script);
			
			// persisting creates a Key, which we can grab
			Key id = script.getId();
			Assert.assertNotNull(id);
			
			// now retrieve the object by its key
			Script s2 = sa.getScript(id);
			Assert.assertNotNull(s2);
			
			Key revisionId = s2.getRevision().getId();
			Assert.assertNotNull(revisionId);
			
			// deletion of Script should delete the revision, since it's an owned, dependent relationship
			sa.delete(script);
			
			// need to 'flush the deletion'.  There may be other ways to do this.
			fac.close();
			sa = fac.getScriptAccessor();
			
			try {
				sa.getScript(id);
				Assert.fail(id+" should be gone.");
			} catch (JDOObjectNotFoundException e) {
				// as expected
				scripts.remove(script);
			}

			
			RevisionAccessor<Script> ra = fac.getScriptRevisionAccessor();
			try {
				ra.getRevision(revisionId);
				Assert.fail(revisionId+" should be gone.");
			} catch (JDOObjectNotFoundException e) {
				// as expected
			}
		}
		
		@Test
		public void testMultipleRevisions() throws Exception {
			Script s1 = new Script();
			s1.setName("script1");
			s1.setRevision(new Revision<Script>());
			Revision<Script> r1 = s1.getRevision();
			r1.setVersion(new Version("1.0"));

			// persist it
			ScriptAccessor sa = fac.getScriptAccessor();
			sa.makePersistent(s1);
			scripts.add(s1); // queue for clean up 

			// now create a revision
			Script s2 = new Script();
			s2.setRevision(new Revision<Script>());
			s2.setName("script1"); // same script, just new version
			Revision<Script> r2 = s2.getRevision();
			r2.setVersion(r1.getVersion().increment());
			
			Assert.assertNotNull(r1); // when s1 was persisted, r1's Key should have been assigned
			r2.setOriginal(r1.getId()); // the revision points to the original
			
			// persist it
			sa.makePersistent(s2);
			scripts.add(s2); // queue for clean up 
			
			Key s2Id = s2.getId();
			
			// Now, using just s2Id, we should be able to retrieve the original
			s2 = sa.getScript(s2Id);
			r2 = s2.getRevision();
			Key originalRevisionId = r2.getOriginal();
			RevisionAccessor<Script> ra = fac.getScriptRevisionAccessor();
			Revision<Script> originalRevision = ra.getRevision(originalRevisionId);
			Assert.assertNotNull(originalRevision);
			// now, do I automatically get the script that owns the revision?
			s1 = originalRevision.getOwner();
			Assert.assertNotNull(s1);
			
			// finally, try the helper function to return the latest
			Revision<Script> latestRevision = ra.getLatest(originalRevision);
			Assert.assertEquals(s2Id, latestRevision.getOwner().getId());
		}
}
