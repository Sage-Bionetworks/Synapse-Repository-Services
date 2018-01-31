package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_SPECIES;

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

	private static final String PATH_DELIMITER = "/";
	private static final Map<String, String> SEARCHABLE_NODE_ANNOTATIONS;

	@Autowired
	NodeDAO nodeDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	V2WikiPageDao wikiPageDao;

	static {
		//TODO: use case insensitive map instead?
		// These are both node primary annotations and additional annotation
		// names
		Map<String, String> searchableNodeAnnotations = new HashMap<String, String>();
		searchableNodeAnnotations.put("disease", FIELD_DISEASE);
		searchableNodeAnnotations.put("Disease", FIELD_DISEASE);
		searchableNodeAnnotations.put("Consortium", FIELD_CONSORTIUM);
		searchableNodeAnnotations.put("consortium", FIELD_CONSORTIUM);
		searchableNodeAnnotations.put("species", FIELD_SPECIES);
		searchableNodeAnnotations.put("Species", FIELD_SPECIES);
		searchableNodeAnnotations.put("platform", FIELD_PLATFORM);
		searchableNodeAnnotations.put("Platform", FIELD_PLATFORM);
		searchableNodeAnnotations.put("platformDesc", FIELD_PLATFORM);
		searchableNodeAnnotations.put("platformVendor", FIELD_PLATFORM);
		searchableNodeAnnotations.put("number_of_samples", FIELD_NUM_SAMPLES);
		searchableNodeAnnotations.put("Number_of_Samples", FIELD_NUM_SAMPLES);
		searchableNodeAnnotations.put("Number_of_samples", FIELD_NUM_SAMPLES);
		searchableNodeAnnotations.put("numSamples", FIELD_NUM_SAMPLES);
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
		// get the path
		EntityPath entityPath = getEntityPath(node.getId());

		// Get the wikipage text
		String wikiPagesText = getAllWikiPageText(node.getId());

		return formulateSearchDocument(node, annos, benefactorACL, entityPath, wikiPagesText);
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
			AccessControlList acl, EntityPath entityPath, String wikiPagesText)
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

		//TODO: maybe consider removing "ancestors" index field
		// Add each ancestor from the path
		List<Long> ancestors = new LinkedList<Long>();
		if (entityPath != null && entityPath.getPath() != null) {
			for (EntityHeader eh : entityPath.getPath()) {
				if (eh.getId() != null && node.getId().equals(eh.getId())) {
					ancestors.add(org.sagebionetworks.repo.model.jdo.KeyFactory
							.stringToKey(eh.getId()));
				}
			}
			// Add the fields.
			fields.setAncestors(ancestors);
		}

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
		addAnnotationsToSearchDocument(fields, annos.getPrimaryAnnotations());
		addAnnotationsToSearchDocument(fields, annos.getAdditionalAnnotations());

		// References, just put the node id to which the reference refers. Not
		// currently adding the version or the type of the reference (e.g.,
		// code/input/output)
		if (null != node.getReference()) {
			fields.setReferences(Arrays.asList(node.getReference().getTargetId()));
		}

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


	static void addAnnotationsToSearchDocument(DocumentFields fields,
			Annotations annots) {

		for (String key : annots.keySet()) {
			String searchFieldName = SEARCHABLE_NODE_ANNOTATIONS.get(key);
			if (null == searchFieldName) {
				continue;
			}

			List values = annots.getAllValues(key);
			if (values.isEmpty() || values.get(0) instanceof byte[]) {
				// no values so nothing to do here
				// don't add blob annotations to the search index
				continue;
			}

			for (Object obj : values) {
				if (obj != null) {
					addAnnotationToSearchDocument(fields, searchFieldName, obj);
					//once a non-null value is found and set, stop iterating
					break;
				}
			}
		}
	}

	static void addAnnotationToSearchDocument(DocumentFields fields, String key, Object value) {
		String stringValue = value.toString();
		//TODO: should we check for get___() != null before set___()? do we prioritize the Primary Annotations or the Additional annotations?
		switch (key){
			case FIELD_DISEASE:
				fields.setDisease(stringValue);
				break;
			case FIELD_CONSORTIUM:
				fields.setConsortium(stringValue);
				break;
			case FIELD_SPECIES:
				fields.setSpecies(stringValue);
				break;
			case FIELD_PLATFORM:
				fields.setPlatform(stringValue);
				break;
			case FIELD_NUM_SAMPLES:
				if (value instanceof Long) {
					fields.setNum_samples((Long) value);
				} else if (value instanceof String) {
					try {
						fields.setNum_samples(Long.valueOf((stringValue).trim()));
					} catch (NumberFormatException e) {
						// swallow this exception, this is just a best-effort
						// attempt to push more annotations into search
					}
				}
				break;
			default:
				throw new IllegalArgumentException(
						"Annotation "
								+ key
								+ " added to searchable annotations map but not added to addAnnotationToSearchDocument");
		}
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
