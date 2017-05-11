package org.sagebionetworks.repo.manager.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
	 * The index field holding the access control list info
	 */
	public static final String ACL_INDEX_FIELD = "acl";
	/**
	 * No more than 100 values in a field value array
	 */
	public static final int FIELD_VALUE_SIZE_LIMIT = 100;

	private static Log log = LogFactory.getLog(SearchDocumentDriverImpl.class);

	private static final String PATH_DELIMITER = "/";
	private static final String DISEASE_FIELD = "disease";
	private static final String TISSUE_FIELD = "tissue";
	private static final String SPECIES_FIELD = "species";
	private static final String PLATFORM_FIELD = "platform";
	private static final String NUM_SAMPLES_FIELD = "num_samples";
	// private static final String INVESTIGATOR = "investigator";
	// private static final String INSTITUTION = "institution";
	private static final Map<String, String> SEARCHABLE_NODE_ANNOTATIONS;

	@Autowired
	NodeDAO nodeDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	V2WikiPageDao wikiPageDao;

	static {
		// These are both node primary annotations and additional annotation
		// names
		Map<String, String> searchableNodeAnnotations = new HashMap<String, String>();
		searchableNodeAnnotations.put("disease", DISEASE_FIELD);
		searchableNodeAnnotations.put("Disease", DISEASE_FIELD);
		searchableNodeAnnotations.put("Tissue_Tumor", TISSUE_FIELD);
		searchableNodeAnnotations.put("sampleSource", TISSUE_FIELD);
		searchableNodeAnnotations.put("SampleSource", TISSUE_FIELD);
		searchableNodeAnnotations.put("tissueType", TISSUE_FIELD);
		searchableNodeAnnotations.put("species", SPECIES_FIELD);
		searchableNodeAnnotations.put("Species", SPECIES_FIELD);
		searchableNodeAnnotations.put("platform", PLATFORM_FIELD);
		searchableNodeAnnotations.put("Platform", PLATFORM_FIELD);
		searchableNodeAnnotations.put("platformDesc", PLATFORM_FIELD);
		searchableNodeAnnotations.put("platformVendor", PLATFORM_FIELD);
		searchableNodeAnnotations.put("number_of_samples", NUM_SAMPLES_FIELD);
		searchableNodeAnnotations.put("Number_of_Samples", NUM_SAMPLES_FIELD);
		searchableNodeAnnotations.put("Number_of_samples", NUM_SAMPLES_FIELD);
		searchableNodeAnnotations.put("numSamples", NUM_SAMPLES_FIELD);
		// searchableNodeAnnotations.put("Investigator", INVESTIGATOR);
		// searchableNodeAnnotations.put("Institution", INSTITUTION);
		SEARCHABLE_NODE_ANNOTATIONS = Collections
				.unmodifiableMap(searchableNodeAnnotations);
	}

	/**
	 * Used by Spring
	 */
	public SearchDocumentDriverImpl() {
	}

	/**
	 * @param backup
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

		Document document = formulateSearchDocument(node, annos, benefactorACL,
				entityPath, wikiPagesText);
		return document;
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

	static byte[] cleanSearchDocument(Document document)
			throws UnsupportedEncodingException, JSONObjectAdapterException {
		String serializedDocument = EntityFactory
				.createJSONStringForEntity(document);

		// AwesomeSearch pukes on control characters. Some descriptions have
		// control characters in them for some reason, in any case, just get rid
		// of all control characters in the search document
		String cleanedDocument = serializedDocument.replaceAll("\\p{Cc}", "");

		// Get rid of escaped control characters too
		cleanedDocument = cleanedDocument.replaceAll("\\\\u00[0,1][0-9,a-f]",
				"");

		// AwesomeSearch expects UTF-8
		return cleanedDocument.getBytes("UTF-8");
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
		document.setLang("en"); // TODO this should have been set via "default"
		// in the schema for this

		// Node fields
		document.setId(node.getId());
		document.setVersion(now.getMillis() / 1000);
		fields.setId(node.getId()); // this is redundant because document id
		// is returned in search results, but its cleaner to have this also show
		// up in the "data" section of AwesomeSearch results
		fields.setEtag(node.getETag());
		fields.setParent_id(node.getParentId());
		fields.setName(node.getName());

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
		fields.setDisease(new ArrayList<String>());
		fields.setSpecies(new ArrayList<String>());
		fields.setTissue(new ArrayList<String>());
		fields.setPlatform(new ArrayList<String>());
		fields.setNum_samples(new ArrayList<Long>());
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
			Collection values = annots.getAllValues(key);

			if (1 > values.size()) {
				// no values so nothing to do here
				continue;
			}

			Object objs[] = values.toArray();

			if (objs[0] instanceof byte[]) {
				// don't add blob annotations to the search index
				continue;
			}

			String searchFieldName = SEARCHABLE_NODE_ANNOTATIONS.get(key);
			for (int i = 0; i < objs.length; i++) {
				if (null == objs[i])
					continue;

				if (null != searchFieldName) {
					addAnnotationToSearchDocument(fields, searchFieldName,
							objs[i]);
				}
			}
		}
	}

	static void addAnnotationToSearchDocument(DocumentFields fields,
			String key, Object value) {
		if (DISEASE_FIELD == key
				&& FIELD_VALUE_SIZE_LIMIT > fields.getDisease().size()) {
			fields.getDisease().add((String) value);
		} else if (TISSUE_FIELD == key
				&& FIELD_VALUE_SIZE_LIMIT > fields.getTissue().size()) {
			fields.getTissue().add((String) value);
		} else if (SPECIES_FIELD == key
				&& FIELD_VALUE_SIZE_LIMIT > fields.getSpecies().size()) {
			fields.getSpecies().add((String) value);
		} else if (PLATFORM_FIELD == key
				&& FIELD_VALUE_SIZE_LIMIT > fields.getPlatform().size()) {
			fields.getPlatform().add((String) value);
		} else if (NUM_SAMPLES_FIELD == key
				&& FIELD_VALUE_SIZE_LIMIT > fields.getNum_samples().size()) {
			if (value instanceof Long) {
				fields.getNum_samples().add((Long) value);
			} else if (value instanceof String) {
				try {
					fields.getNum_samples().add(
							Long.valueOf(((String) value).trim()));
				} catch (NumberFormatException e) {
					// swallow this exception, this is just a best-effort
					// attempt to push more annotations into search
				}
			}
		} else {
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
	public boolean doesDocumentExist(String nodeId, String etag) {
		if (nodeId == null)
			throw new IllegalAccessError("NodeId cannot be null");
		if (etag == null)
			throw new IllegalArgumentException("Etag cannot be null");
		try {
			String current = nodeDao.peekCurrentEtag(nodeId);
			return etag.equals(current);
		} catch (DatastoreException e) {
			return false;
		} catch (NotFoundException e) {
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
