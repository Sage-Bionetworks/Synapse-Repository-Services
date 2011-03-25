package org.sagebionetworks.repo.model.jdo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.JDOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.PMF;




@SuppressWarnings("unchecked")
public class JDOAnnotationsTest {

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
		this.annotsId = null;
	}

	@After
	public void tearDown() throws Exception {
		PersistenceManager pm = null;
		try {
			pm = PMF.get();
			Transaction tx = pm.currentTransaction();
			tx.begin();
			if (annotsId!=null) {
				pm.deletePersistent(pm.getObjectById(JDOAnnotations.class, annotsId));
			}
			tx.commit();
		} finally {
			if (pm != null)
				pm.close();
		}
	}
	
	@Ignore
	@Test
	public void testOuterJoin() throws Exception {
		Long id;
		id= createAnnotations();
		addString(id, "Tissue", "Brain");
		id= createAnnotations();
		PersistenceManager pm = PMF.get();
		{
			Query query = pm.newQuery(JDOAnnotations.class);
			Collection<JDOAnnotations> ans = (Collection<JDOAnnotations>)query.execute();
			o("Total number of JDOAnnotations objects: "+ans.size());
		}	
		{
			Query query = pm.newQuery(JDOAnnotations.class);
			query.setFilter("stringAnnotations.contains(vAnnot) && vAnnot.attribute==pAttr");
			query.declareParameters("String pAttr");
			query.declareVariables(JDOStringAnnotation.class.getName()+" vAnnot");
			Collection<JDOAnnotations> ans = (Collection<JDOAnnotations>)query.execute("Tissue");
			o("Number of JDOAnnotations objects having a 'Tissue' attribute: "+ans.size());
		}	
//		{
//			Query query = pm.newQuery(JDOAnnotations.class);
//			query.setFilter("stringAnnotations.contains(vAnnot) && vAnnot.attribute==pAttr");
//			query.declareParameters("String pAttr");
//			query.declareVariables(JDOStringAnnotation.class.getName()+" vAnnot");
//			query.setResult("this.id, vAnnot.value");
//			Collection<Object[]> ans = (Collection<Object[]>)query.execute("Tissue");
//			for (Object[] row : ans) {
//				o("AnnotationsID: "+row[0]+", annot value: "+row[1]+", total number of col's: "+row.length);
//			}
//		}	
		{
			Query query = pm.newQuery(JDOAnnotations.class);
			query.setFilter("(stringAnnotations.contains(vAnnot)&& vAnnot.attribute==pAttr) "+
					"|| !(stringAnnotations.contains(vAnnot)&& vAnnot.attribute==pAttr)");
			query.declareParameters("String pAttr");
			query.declareVariables(JDOStringAnnotation.class.getName()+" vAnnot");
			//query.setResult("this.id");
			Collection<JDOAnnotations> ans = (Collection<JDOAnnotations>)query.execute("Tissue");
			o("\n"+ans.size()+" row(s):");
			for (JDOAnnotations row : ans) {
				o("AnnotationsID: "+row.getId());
			}
//			for (Object[] row : ans) {
//				o("AnnotationsID: "+row[0]+", total number of col's: "+row.length);
//			}
		}	

//		{
//			Query subQuery = pm.newQuery(JDOStringAnnotation.class);
//			subQuery.setFilter("attribute==\"Tissue\"");
//			subQuery.setResult("this.id");
//			
//			Query query = pm.newQuery(JDOAnnotations.class);
//			query.setFilter("stringAnnotations.contains(vAnnot) && selectedAnnots.contains(vAnnot)");
//			//query.declareParameters("String pAttr");
//			query.declareVariables(JDOStringAnnotation.class.getName()+" vAnnot");
//			query.setResult("this.id, vAnnot.value");
//			query.addSubquery(subQuery, JDOStringAnnotation.class.getName()+" selectedAnnots", null, "Tissue");
//			Collection<Object[]> ans = (Collection<Object[]>)query.execute();
//			o("\n"+ans.size()+" row(s):");
//			for (Object[] row : ans) {
//				o(row.length+" col's: AnnotationsID: "+row[0]+", annot value: "+row[1]);
//			}
//		}	
	}
	
	private static void o(Object s) {System.out.println(s);}
	
	private Long annotsId  = null;

	private Long createAnnotations() {
		PersistenceManager pm = null;
		Transaction tx = null;
		try {
			pm = PMF.get();
			tx = pm.currentTransaction();
			tx.begin();
			JDOAnnotations a = JDOAnnotations.newJDOAnnotations();
			pm.makePersistent(a);
			tx.commit();
			annotsId = a.getId();
			return a.getId();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			if (pm != null)
				pm.close();
		}
	}

	private void addString(Long id) {
		addString(id, "tissue", "brain");
	}
	
	private void addString(Long id, String attr, String value) {
		PersistenceManager pm = null;
		Transaction tx = null;
		try {
			pm = PMF.get();
			 tx = pm.currentTransaction();
			tx.begin();
			JDOAnnotations a = (JDOAnnotations) pm.getObjectById(
					JDOAnnotations.class, id);
			a.toString(); // <-- here we 'touch' all the annotations
			Set<JDOStringAnnotation> ss = a.getStringAnnotations();

			ss.add(new JDOStringAnnotation(attr, value));
			pm.makePersistent(a);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			if (pm != null)
				pm.close();
		}
	}

	private void addDouble(Long id) {
		PersistenceManager pm = null;
		Transaction tx = null;
		try {
			pm = PMF.get();
			tx = pm.currentTransaction();
			tx.begin();
			JDOAnnotations a = (JDOAnnotations) pm.getObjectById(
					JDOAnnotations.class, id);
			a.toString(); // <-- here we 'touch' all the annotations
			Set<JDODoubleAnnotation> ss = a.getDoubleAnnotations();
			// System.out.println("addDouble: isDirty="+JDOHelper.isDirty(a));
			ss.add(new JDODoubleAnnotation("weight", 99.5D));
			// System.out.println("addDouble: isDirty="+JDOHelper.isDirty(a));
			pm.makePersistent(a);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			if (pm != null)
				pm.close();
		}
	}

	private void addLong(Long id) {
		PersistenceManager pm = null;
		Transaction tx = null;
		try {
			pm = PMF.get();
			 tx = pm.currentTransaction();
			tx.begin();
			JDOAnnotations a = (JDOAnnotations) pm.getObjectById(
					JDOAnnotations.class, id);
			a.toString(); // <-- here we 'touch' all the annotations
			Set<JDOLongAnnotation> ss = a.getLongAnnotations();
			// System.out.println("addLong: isDirty="+JDOHelper.isDirty(a));
			ss.add(new JDOLongAnnotation("age", 71L));
			// System.out.println("addLong: isDirty="+JDOHelper.isDirty(a));
			pm.makePersistent(a);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
			if (pm != null)
				pm.close();
		}
	}

	
	@Test
	public void testAnnotQuery() throws Exception {
		PersistenceManager pm = null;
		Long id = createAnnotations();
		addDouble(id);
		addLong(id);
		addString(id);
		try {
			pm = PMF.get();

			//JDOAnnotations a = (JDOAnnotations)
			pm.getObjectById(JDOAnnotations.class, id);
			// System.out.println(a);

			Query query = null;
//			List<JDOAnnotations> annots = null;

			// now query by the String annotation "tissue"
			query = pm.newQuery(JDOAnnotations.class);
			query.setFilter("this.stringAnnotations.contains(vAnnotation) && "
					+ "vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
			query.declareVariables(JDOStringAnnotation.class.getName()
					+ " vAnnotation");
			query.declareParameters(String.class.getName() + " pAttrib, "
					+ String.class.getName() + " pValue");

			List<JDOAnnotations> annots = (List<JDOAnnotations>) query.execute("tissue", "brain");
			Assert.assertEquals("Can't query by String annot", 1, annots
					.iterator().next().getStringAnnotations().size());

			// now query by the Double annotation "weight"
			query = pm.newQuery(JDOAnnotations.class);
			query.setFilter("this.doubleAnnotations.contains(vAnnotation) && "
					+ "vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
			query.declareVariables(JDODoubleAnnotation.class.getName()
					+ " vAnnotation");
			query.declareParameters(String.class.getName() + " pAttrib, "
					+ Double.class.getName() + " pValue");
			annots = (List<JDOAnnotations>) query.execute("weight",
					new Double(99.5));
			Assert.assertEquals("Can't query by Double annot", 1, annots
					.iterator().next().getDoubleAnnotations().size());

			// now query by the Long annotation "age"
			query = pm.newQuery(JDOAnnotations.class);
			query.setFilter("this.longAnnotations.contains(vAnnotation) && "
					+ "vAnnotation.attribute==pAttrib && vAnnotation.value==pValue");
			query.declareVariables(JDOLongAnnotation.class.getName()
					+ " vAnnotation");
			query.declareParameters(String.class.getName() + " pAttrib, "
					+ Long.class.getName() + " pValue");
			annots = (List<JDOAnnotations>) query.execute("age",
					new Long(71));
			Assert.assertEquals("Can't query by Long annot", 1, annots
					.iterator().next().getLongAnnotations().size());

		} finally {
			if (pm != null)
				pm.close();
		}
	}
}
