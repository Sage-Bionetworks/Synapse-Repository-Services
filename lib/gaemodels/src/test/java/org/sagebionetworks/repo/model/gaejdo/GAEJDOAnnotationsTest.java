package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOAnnotations;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODataset;
import org.sagebionetworks.repo.model.gaejdo.GAEJDORevision;
import org.sagebionetworks.repo.model.gaejdo.GAEJDOStringAnnotation;
import org.sagebionetworks.repo.model.gaejdo.Version;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class GAEJDOAnnotationsTest {
	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig());

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

	@Before
	public void setUp() throws Exception {
		helper.setUp();
	}

	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	private Key createAnnotations() {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Transaction tx = pm.currentTransaction();
			tx.begin();
			GAEJDOAnnotations a = GAEJDOAnnotations.newGAEJDOAnnotations();
			pm.makePersistent(a);
			tx.commit();
			return a.getId();
		} finally {
			if (pm != null)
				pm.close();
		}
	}

	private void addString(Key id) {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Transaction tx = pm.currentTransaction();
			tx.begin();
			GAEJDOAnnotations a = (GAEJDOAnnotations) pm.getObjectById(
					GAEJDOAnnotations.class, id);
			a.toString(); // <-- here we 'touch' all the annotations
			Set<GAEJDOStringAnnotation> ss = a.getStringAnnotations();

			ss.add(new GAEJDOStringAnnotation("tissue", "brain"));
			pm.makePersistent(a);
			tx.commit();
		} finally {
			if (pm != null)
				pm.close();
		}
	}

	private void addFloat(Key id) {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Transaction tx = pm.currentTransaction();
			tx.begin();
			GAEJDOAnnotations a = (GAEJDOAnnotations) pm.getObjectById(
					GAEJDOAnnotations.class, id);
			a.toString(); // <-- here we 'touch' all the annotations
			Set<GAEJDOFloatAnnotation> ss = a.getFloatAnnotations();
			// System.out.println("addFloat: isDirty="+JDOHelper.isDirty(a));
			ss.add(new GAEJDOFloatAnnotation("weight", 100F));
			// System.out.println("addFloat: isDirty="+JDOHelper.isDirty(a));
			pm.makePersistent(a);
			tx.commit();
		} finally {
			if (pm != null)
				pm.close();
		}
	}

	@Test
	public void testAnnotQuery() throws Exception {
		PersistenceManager pm = null;
		Key id = createAnnotations();
		addFloat(id);
		addString(id);
		try {
			pm = PMF.get();

			GAEJDOAnnotations a = (GAEJDOAnnotations) pm.getObjectById(
					GAEJDOAnnotations.class, id);
			// System.out.println(a);

			Query query = null;
			List<GAEJDOAnnotations> annots = null;

			// now query by the String annotation "tissue"
			query = pm.newQuery(GAEJDOAnnotations.class);
			query.setFilter("this.stringAnnotations.contains(vAnnotation) && "
					+ "vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
			query.declareVariables(GAEJDOStringAnnotation.class.getName()
					+ " vAnnotation");
			query.declareParameters(String.class.getName() + " pAttrib, "
					+ String.class.getName() + " pValue");

			annots = (List<GAEJDOAnnotations>) query.execute("tissue", "brain");
			Assert.assertEquals("Can't query by String annot", 1, annots
					.iterator().next().getStringAnnotations().size());

			query = pm.newQuery(GAEJDOAnnotations.class);
			query.setFilter("this.floatAnnotations.contains(vAnnotation) && "
					+ "vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
			query.declareVariables(GAEJDOFloatAnnotation.class.getName()
					+ " vAnnotation");
			query.declareParameters(String.class.getName() + " pAttrib, "
					+ Float.class.getName() + " pValue");
			annots = (List<GAEJDOAnnotations>) query.execute("weight",
					new Float(100F));
			Assert.assertEquals("Can't query by Float annot", 1, annots
					.iterator().next().getFloatAnnotations().size());

		} finally {
			if (pm != null)
				pm.close();
		}
	}
}
