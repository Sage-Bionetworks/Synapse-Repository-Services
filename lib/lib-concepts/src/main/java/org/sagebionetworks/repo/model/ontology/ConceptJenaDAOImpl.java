package org.sagebionetworks.repo.model.ontology;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.impl.OntModelImpl;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

/**
 * A Jena implementation of the Concept DAO.
 * 
 * @author jmhill
 * 
 */
public class ConceptJenaDAOImpl implements ConceptDAO {
	
	public static final String SKOS_URL = "http://www.w3.org/2004/02/skos/core#";
	public static final String PREFERED_LABEL = "prefLabel";
	public static final String BORADER = "broader";
	public static final String ALT_LABEL = "altLabel";
	public static final String DESCRIPTION = "definition";

	public static final String SPARQL_PREFIXES = "PREFIX skos: <"+SKOS_URL+"> "
			+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";


	public static final String URI = "x";
	/**
	 * Get all children that belong to this concept with the skos:broader
	 */
	public static final String SPARQL_GET_CHILDREN_CONCEPTS = SPARQL_PREFIXES
			+ " SELECT ?x ?prefLabel WHERE { " + "?x skos:broaderTransitive <%1$s>. "
			+ "?x skos:prefLabel ?prefLabel" + "}";
	
	/**
	 * The prefered label property
	 */
	public static final Property PROP_PREF_LABEL = new PropertyImpl(SKOS_URL+PREFERED_LABEL);
	public static final Property PROP_BROADER = new PropertyImpl(SKOS_URL+BORADER);
	public static final Property PROP_ALT_LABEL = new PropertyImpl(SKOS_URL+ALT_LABEL);
	public static final Property PROP_DESCRIPTION = new PropertyImpl(SKOS_URL+DESCRIPTION);

	private Model rdfModel;
	
	@Autowired
	String ontologyBaseURI;
	
	/**
	 * Used by spring
	 * @throws IOException 
	 */
	public ConceptJenaDAOImpl(String classpathFile) throws IOException{
		URL url = ConceptJenaDAOImpl.class.getClassLoader().getResource(classpathFile);
		if(url == null) throw new IOException("Failed to find: "+classpathFile+" on the classpath");
		OntModelImpl reader = new OntModelImpl(OntModelSpec.OWL_MEM);
		rdfModel = reader.read(url.toString());
	}

	/**
	 * Create a new Concept DAO from a SKOS RDF file URI.
	 * 
	 * @param skosURI
	 * @throws SKOSCreationException
	 */
	public ConceptJenaDAOImpl(Model rdfModel) {
		if (rdfModel == null)
			throw new IllegalArgumentException("RDF model cannot be null");
		this.rdfModel = rdfModel;

	}

	@Override
	public List<ConceptSummary> getAllConcepts(String parentConceptURI) throws DatastoreException {
		if (parentConceptURI == null) throw new IllegalArgumentException("parentConceptURI cannot be null");
		// Create a query
		String queryString = String.format(SPARQL_GET_CHILDREN_CONCEPTS, parentConceptURI);
		//System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution qexec = QueryExecutionFactory.create(query, this.rdfModel);
		try {
			ResultSet resultSet = qexec.execSelect();
			List<ConceptSummary> results = new LinkedList<ConceptSummary>();
			while(resultSet.hasNext()){
				QuerySolution soln = resultSet.nextSolution();
				if(soln == null) throw new DatastoreException("Null QuerySolution");
				RDFNode node = soln.get(URI);
				if(node == null) throw new DatastoreException("Failed to get URI from QuerySolution :"+soln.toString());
				//System.out.println(node);
				ConceptSummary summary = new ConceptSummary();
				summary.setUri(node.toString());
				Literal prefLit = soln.getLiteral(PREFERED_LABEL);
				if(prefLit == null) throw new DatastoreException("No preferred label for "+node.toString());
				summary.setPreferredLabel(prefLit.toString());
				results.add(summary);
			}
			return results;
		} finally {
			qexec.close();
		}
	}

	/**
	 * Lookup a concpet using its URI.
	 */
	@Override
	public Concept getConceptForUri(String conceptUri) throws DatastoreException, NotFoundException {
		if(conceptUri == null) throw new IllegalArgumentException("Concept URI cannot be null");
		RDFNode node = this.rdfModel.getRDFNode(Node.createURI(conceptUri));
		if(node == null) throw new NotFoundException("Cannot find concept: "+conceptUri);
		if(! this.rdfModel.containsResource(node)) throw new NotFoundException("Cannot find concept: "+conceptUri);
//		System.out.println(node);
		Resource resource = this.rdfModel.getResource(conceptUri);
//		System.out.println(resource);
		Concept concept = new Concept();
		ConceptSummary summary = new ConceptSummary();
		concept.setUri(conceptUri);
		concept.setPreferredLabel(getPropertyAsString(resource, PROP_PREF_LABEL));
		concept.setParent(getPropertyAsNodeString(resource, PROP_BROADER));
		concept.setSynonyms(listPropertyAsString(resource, PROP_ALT_LABEL));
		concept.setDefinition(getPropertyAsString(resource, PROP_PREF_LABEL));
		
		// convert it to a concept
		return concept;
	}
	
	/**
	 * Extract a string value from a property.
	 * @param resource
	 * @param prop
	 * @return
	 * @throws DatastoreException
	 */
	public static String getPropertyAsString(Resource resource, Property prop) throws DatastoreException{
		try{
			return getProperty(resource, prop).getString();
		}catch (Exception e){
			// Convert any exception to a DatastoreException
			throw new DatastoreException("Cannot get a property: "+prop.getURI()+" for resource: "+resource.getURI()+" as a string: "+e.getMessage());
		}
	}
	
	/**
	 * Extract a string value from a property.
	 * @param resource
	 * @param prop
	 * @return
	 * @throws DatastoreException
	 */
	public static String getPropertyAsNodeString(Resource resource, Property prop) throws DatastoreException{
		try{
			Statement s = getProperty(resource, prop);
			return s.getObject().toString();
		}catch (Exception e){
			// Convert any exception to a DatastoreException
			throw new DatastoreException("Cannot get a property: "+prop.getURI()+" for resource: "+resource.getURI()+" as a RDFNode : "+e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param resource
	 * @param prop
	 * @return
	 * @throws DatastoreException
	 */
	public static List<String> listPropertyAsString(Resource resource, Property prop) throws DatastoreException{
		try{
			List<String> results = new ArrayList<String>();
			StmtIterator it = listProperties(resource, prop);
			while(it.hasNext()){
				Statement s = it.next();
				results.add(s.getString());
			}
			return results;
		}catch (Exception e){
			// Convert any exception to a DatastoreException
			throw new DatastoreException("Cannot get a property: "+prop.getURI()+" for resource: "+resource.getURI()+" as a list of strings: "+e.getMessage());
		}
	}
	
	/**
	 * Get a property
	 * @param resource
	 * @param prop
	 * @return
	 * @throws DatastoreException 
	 */
	public static Statement getProperty(Resource resource, Property prop) throws DatastoreException {
		if(resource == null) throw new IllegalArgumentException("Resource cannot be null");
		if(prop == null) throw new IllegalArgumentException("Property cannot be null");
		Statement s = resource.getProperty(prop);
		if(s == null) throw new DatastoreException("Cannot find a property: "+prop.getURI()+" for resource: "+resource.getURI());
		return s;
	}
	
	/**
	 * Get a property
	 * @param resource
	 * @param prop
	 * @return
	 * @throws DatastoreException 
	 */
	public static StmtIterator listProperties(Resource resource, Property prop) throws DatastoreException {
		if(resource == null) throw new IllegalArgumentException("Resource cannot be null");
		if(prop == null) throw new IllegalArgumentException("Property cannot be null");
		StmtIterator it = resource.listProperties(prop);
		if(it == null) throw new DatastoreException("Cannot list a property: "+prop.getURI()+" for resource: "+resource.getURI());
		return it;
	}

}
