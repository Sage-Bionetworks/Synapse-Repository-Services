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
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.model.jdo.JDORevision;
import org.sagebionetworks.repo.model.jdo.JDOScript;
import org.sagebionetworks.repo.model.jdo.Version;






public class ScriptTest {

	@BeforeClass
	public static void beforeClass() throws Exception {
		// from
		// http://groups.google.com/group/google-appengine-java/browse_thread/thread/96baed75e3c30a58/00d5afb2e0445882?lnk=gst&q=DataNucleus+plugin#00d5afb2e0445882
		// This one caused all the WARNING and SEVERE logs about eclipse UI
		// elements
		Logger.getLogger("DataNucleus.Plugin").setLevel(Level.OFF);
		// This one logged the last couple INFOs about Persistence configuration
		Logger.getLogger("DataNucleus.Persistence").setLevel(Level.WARNING);
	}

	private DAOFactory fac;
	private Collection<String> scriptIds;

	@Before
	public void setUp() throws Exception {
		fac = new JDODAOFactoryImpl();
		scriptIds = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		if (fac != null) {
			for (String id : scriptIds) {
				// fac.getScriptDAO().delete(id);
			}
			scriptIds.clear();
		}
	}

	@Test
	@Ignore
	public void testCreateandRetrieve() throws Exception {
		JDOScript script = new JDOScript();
		script.setName("script name");
		script.setRevision(new JDORevision<JDOScript>());
		Date now = new Date();
		script.getRevision().setVersion(new Version("1.0"));
		script.getRevision().setRevisionDate(now);
		script.getRevision().setOriginal(null); // THIS is the original
		script.setPublicationDate(now);
		URI uri = new URI("https://sagebionetworks.com");
		script.setSource(uri);
		String overview = "This R script converts genotyping data into gene interaction networks.";
		script.setOverview(new String(overview));

		// // persist it
		// ScriptDAO sa = fac.getScriptAccessor();
		// sa.makePersistent(script);
		//
		// // persisting creates a Long, which we can grab
		// Long id = script.getId();
		// scriptIds.add(id);
		// Assert.assertNotNull(id);
		//
		// // now retrieve the object by its key
		// JDOScript s2 = sa.getScript(id);
		// Assert.assertNotNull(s2);
		//
		// // check that all the fields were persisted
		// Assert.assertEquals("script name", s2.getName());
		// Assert.assertEquals(now, s2.getPublicationDate());
		// Assert.assertEquals(uri, s2.getSource());
		//
		// // the original version of script is itself
		// Assert.assertEquals(null, script.getRevision().getOriginal());
		//
		// // the original of the retrieved script is also this script
		// Assert.assertNotNull(s2.getRevision());
		// Assert.assertEquals(null, s2.getRevision().getOriginal());
		//
		// Assert.assertEquals(script.getRevision().getVersion(),
		// s2.getRevision().getVersion());
		// Assert.assertEquals(script.getRevision().getRevisionDate(),
		// s2.getRevision().getRevisionDate());
		// Assert.assertEquals(overview, s2.getOverview().getValue());

	}

	@Test
	@Ignore
	public void testCreateandDelete() throws Exception {
		JDOScript script = new JDOScript();
		script.setName("script name");
		script.setRevision(new JDORevision<JDOScript>());

		// // persist it
		// ScriptDAO sa = fac.getScriptAccessor();
		// sa.makePersistent(script);
		// scriptIds.add(script.getId());
		//
		// // persisting creates a Long, which we can grab
		// Long id = script.getId();
		// Assert.assertNotNull(id);
		//
		// // now retrieve the object by its key
		// JDOScript s2 = sa.getScript(id);
		// Assert.assertNotNull(s2);
		//
		// Long revisionId = s2.getRevision().getId();
		// Assert.assertNotNull(revisionId);
		//
		// // deletion of Script should delete the revision, since it's an
		// owned, dependent relationship
		// sa.delete(id);
		//
		// // need to 'flush the deletion'. There may be other ways to do this.
		// //fac.close();
		// sa = fac.getScriptAccessor();
		//
		// try {
		// sa.getScript(id);
		// Assert.fail(id+" should be gone.");
		// } catch (JDOObjectNotFoundException e) {
		// // as expected
		// scriptIds.remove(script.getId());
		// }
		//
		//
		// RevisionAccessor<JDOScript> ra = fac.getScriptRevisionAccessor();
		// try {
		// ra.getRevision(revisionId);
		// Assert.fail(revisionId+" should be gone.");
		// } catch (JDOObjectNotFoundException e) {
		// // as expected
		// }
	}

	@Test
	@Ignore
	public void testMultipleRevisions() throws Exception {
		// JDOScript s1 = new JDOScript();
		// s1.setName("script1");
		// s1.setRevision(new JDORevision<JDOScript>());
		// JDORevision<JDOScript> r1 = s1.getRevision();
		// r1.setVersion(new Version("1.0"));
		//
		// // persist it
		// ScriptDAO sa = fac.getScriptAccessor();
		// sa.makePersistent(s1);
		// scriptIds.add(s1.getId()); // queue for clean up
		//
		// // now create a revision
		// JDOScript s2 = new JDOScript();
		// s2.setRevision(new JDORevision<JDOScript>());
		// s2.setName("script1"); // same script, just new version
		// JDORevision<JDOScript> r2 = s2.getRevision();
		// r2.setVersion(r1.getVersion().increment());
		//
		// Assert.assertNotNull(r1); // when s1 was persisted, r1's Long should
		// have been assigned
		// r2.setOriginal(r1.getId()); // the revision points to the original
		//
		// // persist it
		// sa.makePersistent(s2);
		// scriptIds.add(s2.getId()); // queue for clean up
		//
		// Long s2Id = s2.getId();
		//
		// // Now, using just s2Id, we should be able to retrieve the original
		// s2 = sa.getScript(s2Id);
		// r2 = s2.getRevision();
		// Long originalRevisionId = r2.getOriginal();
		// RevisionAccessor<JDOScript> ra = fac.getScriptRevisionAccessor();
		// JDORevision<JDOScript> originalRevision =
		// ra.getRevision(originalRevisionId);
		// Assert.assertNotNull(originalRevision);
		// // // now, do I automatically get the script that owns the revision?
		// // s1 = originalRevision.getOwner();
		// // Assert.assertNotNull(s1);
		//
		// // finally, try the helper function to return the latest
		// JDOScript latestRevision = sa.getLatest(s2);
		// Assert.assertEquals(s2Id, latestRevision.getId());
		// latestRevision = sa.getLatest(s1);
		// Assert.assertEquals(s2Id, latestRevision.getId());
	}
}
