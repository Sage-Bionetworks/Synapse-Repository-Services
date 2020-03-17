package org.sagebionetworks.repo.manager.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_DIAGNOSIS;
import static org.sagebionetworks.search.SearchConstants.FIELD_ORGAN;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdAndAlias;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.entity.NameIdType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchUtil;
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
		searchableNodeAnnotations.put(FIELD_DIAGNOSIS, Collections.unmodifiableList(Arrays.asList("diagnosis")));
		searchableNodeAnnotations.put(FIELD_TISSUE, Collections.unmodifiableList(Arrays.asList("tissue")));
		searchableNodeAnnotations.put(FIELD_CONSORTIUM, Collections.unmodifiableList(Arrays.asList("consortium")));
		searchableNodeAnnotations.put(FIELD_ORGAN, Collections.unmodifiableList(Arrays.asList("organ")));

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
	public Document formulateFromBackup(Node node) throws NotFoundException, DatastoreException {
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		String benefactorId = nodeDao.getBenefactor(node.getId());
		AccessControlList benefactorACL = aclDAO.get(benefactorId,
				ObjectType.ENTITY);
		Long revId = node.getVersionNumber();

		Annotations annos = nodeDao.getUserAnnotationsForVersion(node.getId(),
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
	@Override
	public EntityPath getEntityPath(String nodeId) throws NotFoundException {
		List<EntityHeader> pathHeaders = NameIdType.toEntityHeader(nodeDao.getEntityPath(nodeId));
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(pathHeaders);
		return entityPath;
	}
	
	@Override
	public List<IdAndAlias> getAliases(List<String> nodeIds) {
		return nodeDao.getAliasByNodeId(nodeIds);
	}


	@Override
	public Document formulateSearchDocument(Node node, Annotations annos,
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
		String descriptionValue = wikiPagesText != null ? wikiPagesText : "";
		// Set user generated description after sanitizing it
		fields.setDescription(SearchUtil.stripUnsupportedUnicodeCharacters(descriptionValue));

		fields.setCreated_by(node.getCreatedByPrincipalId().toString());
		fields.setCreated_on(node.getCreatedOn().getTime() / 1000);
		fields.setModified_by(node.getModifiedByPrincipalId().toString());
		fields.setModified_on(node.getModifiedOn().getTime() / 1000);

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

	void addAnnotationsToSearchDocument(DocumentFields fields, Annotations annotations){
		// process a map of annotation keys to values
		Map<String, String> firstAnnotationValues = getFirsAnnotationValues(annotations);

		//set the values for the document fields
		fields.setDiagnosis(getSearchIndexFieldValue(firstAnnotationValues, FIELD_DIAGNOSIS));
		fields.setConsortium(getSearchIndexFieldValue(firstAnnotationValues, FIELD_CONSORTIUM));
		fields.setTissue(getSearchIndexFieldValue(firstAnnotationValues, FIELD_TISSUE));
		fields.setOrgan(getSearchIndexFieldValue(firstAnnotationValues, FIELD_ORGAN));

	}

	/**
	 * Returns a Map from the keys of the NamedAnnotations to the first value (as a String) for that key.
	 * @param anno Annotation source from which the keys and values are retrieved.
	 *             Annotation keys will be converted to lower case before they are added to this map
	 */
	Map<String, String> getFirsAnnotationValues(Annotations anno){
		Map<String, String> firstAnnotationValues = new HashMap<>();
		for(Map.Entry<String, AnnotationsValue> entry: anno.getAnnotations().entrySet()){
			firstAnnotationValues.putIfAbsent(entry.getKey().toLowerCase(), AnnotationsV2Utils.getSingleValue(entry.getValue()));
		}
		return firstAnnotationValues;
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
				//sanitize user generated values
				return SearchUtil.stripUnsupportedUnicodeCharacters(value);
			}
		}
		return null;
	}


	@Override
	public Document formulateSearchDocument(String nodeId) throws DatastoreException, NotFoundException {
		if (nodeId == null)
			throw new IllegalArgumentException("NodeId cannot be null");
		Node node = nodeDao.getNode(nodeId);
		return formulateFromBackup(node);
	}

	/**
	 * Get all wiki text for an entity.
	 * 
	 * @param nodeId
	 * @return
	 * @throws DatastoreException
	 * @throws IOException 
	 */
	public String getAllWikiPageText(String nodeId) throws DatastoreException {
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
		} catch (IOException e){
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			// There is no WikiPage for this node.
			return null;
		}
	}

	@Override
	public boolean doesEntityExistInRepository(String entityId){
		return nodeDao.isNodeAvailable(entityId);
	}

}
