package org.sagebionetworks.repo.manager.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class writes out search documents in batch.
 * 
 * Note that it also encapsulates the configuration of our search index.
 * 
 * See also the script that creates and configures the search index fields
 * https://sagebionetworks.jira.com/svn/PLFM/users/deflaux/cloudSearchHacking/
 * createSearchIndexFields.sh
 * 
 */
public class SearchDocumentDriverImpl implements SearchDocumentDriver {

	/**
	 * No more than 100 values in a field value array
	 */
	public static final int FIELD_VALUE_SIZE_LIMIT = 100;

	private static Log log = LogFactory.getLog(SearchDocumentDriverImpl.class);

	static final Map<String, List<String>> SEARCHABLE_NODE_ANNOTATIONS;

	@Autowired
	NodeDAO nodeDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	V2WikiPageDao wikiPageDao;

	static { // initialize SEARCHABLE_NODE_ANNOTATIONS
		// NOTE: ORDER MATTERS. Earlier annotation key names will be preferred over later ones if both keys are present.
		Map<String, List<String>> searchableNodeAnnotations = new HashMap<>();
		searchableNodeAnnotations.put(FIELD_DISEASE, Collections.unmodifiableList(Arrays.asList("disease")));
		searchableNodeAnnotations.put(FIELD_TISSUE, Collections.unmodifiableList(Arrays.asList("tissue", "tissue_tumor", "sampleSource", "tissueType")));
		searchableNodeAnnotations.put(FIELD_PLATFORM, Collections.unmodifiableList(Arrays.asList("platform", "platformDesc", "platformVendor")));
		searchableNodeAnnotations.put(FIELD_NUM_SAMPLES, Collections.unmodifiableList(Arrays.asList("numSamples", "num_samples", "number_of_samples")));
		searchableNodeAnnotations.put(FIELD_CONSORTIUM, Collections.unmodifiableList(Arrays.asList("consortium")));

		SEARCHABLE_NODE_ANNOTATIONS = Collections
				.unmodifiableMap(searchableNodeAnnotations);
	}

	/**
	 * Used by Spring
	 */
	public SearchDocumentDriverImpl() {
	}

	/**
	 * @param node
	 * @return
	 * @throws NotFoundException
	 * @throws IOException 
	 * @throws DatastoreException 
	 */
	public Document formulateFromBackup(Node node) throws NotFoundException, DatastoreException, IOException {
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		String benefactorId = nodeDao.getBenefactor(node.getId());
		AccessControlList benefactorACL = aclDAO.get(benefactorId,
				ObjectType.ENTITY);
		Long revId = node.getVersionNumber();
		NamedAnnotations annos = nodeDao.getAnnotationsForVersion(node.getId(),
				revId);
		// Get the wikipage text
		String wikiPagesText = getAllWikiPageText(node.getId());

		return formulateSearchDocument(node, annos, benefactorACL, wikiPagesText);
	}

	/**
	 * Get the entity path
	 * 
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 */
	public EntityPath getEntityPath(String nodeId) throws NotFoundException {
		List<EntityHeader> pathHeaders = nodeDao.getEntityPath(nodeId);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(pathHeaders);
		return entityPath;
	}


	@Override
	public Document formulateSearchDocument(Node node, NamedAnnotations annos,
											AccessControlList acl, String wikiPagesText)
			throws DatastoreException, NotFoundException {
		DateTime now = DateTime.now();
		Document document = new Document();
		DocumentFields fields = new DocumentFields();
		document.setFields(fields);

		document.setType(DocumentTypeNames.add);
		// in the schema for this

		// Node fields
		document.setId(node.getId());
		fields.setEtag(node.getETag());
		fields.setParent_id(node.getParentId());
		fields.setName(node.getName());

		fields.setNode_type(node.getNodeType().name());

		// The description contains the entity description and all wiki page
		// text
		StringBuilder descriptionValue = new StringBuilder();
		if (wikiPagesText != null) {
			descriptionValue.append(wikiPagesText);
		}
		// Set the description
		fields.setDescription(descriptionValue.toString());

		fields.setCreated_by(node.getCreatedByPrincipalId().toString());
		fields.setCreated_on(node.getCreatedOn().getTime() / 1000);
		fields.setModified_by(node.getModifiedByPrincipalId().toString());
		fields.setModified_on(node.getModifiedOn().getTime() / 1000);

		// Stuff in this field any extra copies of data that you would like to
		// boost in free text search
		List<String> boost = new ArrayList<String>();
		fields.setBoost(boost);
		boost.add(node.getName());
		boost.add(node.getName());
		boost.add(node.getName());
		boost.add(node.getId());
		boost.add(node.getId());
		boost.add(node.getId());

		// Annotations
		addAnnotationsToSearchDocument(fields, annos);

		// READ and UPDATE ACLs
		List<String> readAclValues = new ArrayList<String>();
		fields.setAcl(readAclValues);
		List<String> updateAclValues = new ArrayList<String>();
		fields.setUpdate_acl(updateAclValues);
		for (ResourceAccess access : acl.getResourceAccess()) {
			if (access.getAccessType().contains(ACCESS_TYPE.READ)) {
				if (FIELD_VALUE_SIZE_LIMIT > readAclValues.size()) {
					readAclValues.add(access.getPrincipalId().toString()); // TODO
																			// could
																			// look
																			// up
																			// display
																			// name
																			// from
																			// UserProfile
																			// and
																			// substitute
																			// here
				} else {
					log.error("Had to leave READ acl "
							+ access.getPrincipalId()
							+ " out of search document " + node.getId()
							+ " due to AwesomeSearch limits");
				}
			}
			if (access.getAccessType().contains(ACCESS_TYPE.UPDATE)) {
				if (FIELD_VALUE_SIZE_LIMIT > updateAclValues.size()) {
					updateAclValues.add(access.getPrincipalId().toString()); // TODO
																				// could
																				// look
																				// up
																				// display
																				// name
																				// from
																				// UserProfile
																				// and
																				// substitute
																				// here
				} else {
					log.error("Had to leave UPDATE acl "
							+ access.getPrincipalId()
							+ " out of search document " + node.getId()
							+ " due to AwesomeSearch limits");
				}
			}
		}
		return document;
	}

	void addAnnotationsToSearchDocument(DocumentFields fields, NamedAnnotations annotations){
		// process a map of annotation keys to values
		Map<String, String> firstAnnotationValues = getFirsAnnotationValues(annotations);

		//set the values for the document fields
		fields.setDisease(getSearchIndexFieldValue(firstAnnotationValues, FIELD_DISEASE));
		fields.setConsortium(getSearchIndexFieldValue(firstAnnotationValues, FIELD_CONSORTIUM));
		fields.setTissue(getSearchIndexFieldValue(firstAnnotationValues, FIELD_TISSUE));
		fields.setPlatform(getSearchIndexFieldValue(firstAnnotationValues, FIELD_PLATFORM));
		try {
			fields.setNum_samples(NumberUtils.createLong(getSearchIndexFieldValue(firstAnnotationValues, FIELD_NUM_SAMPLES)));
		}catch (NumberFormatException e){
			/* If the user did not provide a numeric value for FIELD_NUM_SAMPLES
			   then that value will not be added to the search index. This is not considered an error.
			 */
		}

	}

	/**
	 * Returns a Map from the keys of the NamedAnnotations to the first value (as a String) for that key.
	 * @param annotations
	 * @return
	 */
	Map<String, String> getFirsAnnotationValues(NamedAnnotations annotations){
		Map<String, String> firstAnnotationValues = new HashMap<>();
		addFirstAnnotationValuesToMap(annotations.getAdditionalAnnotations(), firstAnnotationValues);
		return firstAnnotationValues;
	}

	/**
	 * For each key in the Annotations's key set, add the first matching value as a String to the map.
	 * @param anno Annotation source from which the keys and values are retrieved.
	 * @param annoValuesMap map to which the Annotation values will be added.
	 *                      Annotation keys will be converted to lower case before they are added to this map
	 */
	void addFirstAnnotationValuesToMap(Annotations anno, Map<String, String> annoValuesMap){
		for(String key: anno.keySet()){
			Object value = anno.getSingleValue(key);
			if( value != null && !(value instanceof byte[])) {
				annoValuesMap.putIfAbsent(key.toLowerCase(), value.toString());
			}
		}
	}

	/**
	 * Given the index field name, retrieve
	 * @param firsAnnotationValues map of annotation keys to values. Assumes that all key Strings are in lower case.
	 * @param indexFieldKey field value from @see org.sagebionetworks.search.SearchConstants
	 * @return
	 */
	String getSearchIndexFieldValue(Map<String, String> firsAnnotationValues, String indexFieldKey){
		for(String possibleAnnotationName: SEARCHABLE_NODE_ANNOTATIONS.get(indexFieldKey)){
			String value = firsAnnotationValues.get(possibleAnnotationName.toLowerCase());
			if(value != null){
				return value;
			}
		}
		return null;
	}


	@Override
	public Document formulateSearchDocument(String nodeId)
			throws DatastoreException, NotFoundException, IOException {
		if (nodeId == null)
			throw new IllegalArgumentException("NodeId cannot be null");
		Node node = nodeDao.getNode(nodeId);
		return formulateFromBackup(node);
	}

	@Override
	public boolean doesNodeExist(String nodeId, String etag) {
		if (nodeId == null)
			throw new IllegalAccessError("NodeId cannot be null");
		if (etag == null)
			throw new IllegalArgumentException("Etag cannot be null");
		try {
			String current = nodeDao.peekCurrentEtag(nodeId);
			return etag.equals(current);
		} catch (DatastoreException | NotFoundException e) {
			return false;
		}
	}

	/**
	 * Get all wiki text for an entity.
	 * 
	 * @param nodeId
	 * @return
	 * @throws DatastoreException
	 * @throws IOException 
	 */
	public String getAllWikiPageText(String nodeId) throws DatastoreException, IOException {
		// Lookup all wiki pages for this node
		try {
			long limit = 100L;
			long offset = 0L;
			List<V2WikiHeader> wikiHeaders = wikiPageDao.getHeaderTree(nodeId,
					ObjectType.ENTITY, limit, offset);
			if (wikiHeaders == null)
				return null;
			// For each header get the wikipage
			StringBuilder builder = new StringBuilder();
			for (V2WikiHeader header : wikiHeaders) {
				WikiPageKey key = WikiPageKeyHelper.createWikiPageKey(nodeId, ObjectType.ENTITY, header.getId());
				V2WikiPage page = wikiPageDao.get(key, null);
				// Append the title and markdown
				if (page.getTitle() != null) {
					builder.append("\n");
					builder.append(page.getTitle());
				}
				String markdownString = wikiPageDao.getMarkdown(key, null);
				builder.append("\n");
				builder.append(markdownString);
			}
			return builder.toString();
		} catch (NotFoundException e) {
			// There is no WikiPage for this node.
			return null;
		}
	}

}
