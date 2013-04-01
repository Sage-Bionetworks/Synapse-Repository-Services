package org.sagebionetworks.repo.manager.search;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.NodeBackupManager;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriverImpl;
import org.sagebionetworks.repo.manager.wiki.WikiManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.query.jdo.NodeAliasCache;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
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
//	private static final String INVESTIGATOR = "investigator";
//	private static final String INSTITUTION = "institution";
	private static final Map<String, String> SEARCHABLE_NODE_ANNOTATIONS;

	@Autowired
	NodeBackupManager backupManager;

	@Autowired
	NodeAliasCache aliasCache;

	@Autowired
	UserManager userManager;
	
	@Autowired
	NodeManager nodeManager;
	
	@Autowired
	WikiPageDao wikiPageDao;
		
	// For now we can just create one of these. We might need to make beans in
	// the future.
	MigrationDriver migrationDriver = MigrationDriverImpl.instanceForTesting();

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
//		searchableNodeAnnotations.put("Investigator", INVESTIGATOR);
//		searchableNodeAnnotations.put("Institution", INSTITUTION);
		SEARCHABLE_NODE_ANNOTATIONS = Collections
				.unmodifiableMap(searchableNodeAnnotations);
	}

	/**
	 * Used by Spring
	 */
	public SearchDocumentDriverImpl() {
	}

	/**
	 * Used by unit tests.
	 * 
	 * @param backupManager
	 * 
	 */
	public SearchDocumentDriverImpl(NodeBackupManager backupManager) {
		super();
		this.backupManager = backupManager;
	}



	/**
	 * @param backup
	 * @return
	 * @throws NotFoundException
	 */
	public Document formulateFromBackup(NodeBackup backup)
			throws NotFoundException {
		Node node = backup.getNode();
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		String benefactorId = backup.getBenefactor();
		NodeBackup benefactorBackup = backupManager.getNode(benefactorId);
		Long revId = node.getVersionNumber();
		NodeRevisionBackup rev = backupManager.getNodeRevision(node.getId(),
				revId);
		// get the path
		EntityPath entityPath = getEntityPath(node.getId());

		// Get the wikipage text
		 String wikiPagesText = getAllWikiPageText(node.getId());
		
		Document document = formulateSearchDocument(node, rev, benefactorBackup
				.getAcl(), entityPath, wikiPagesText);
		return document;
	}

	/**
	 * Get the entity path
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 */
	public EntityPath getEntityPath(String nodeId) throws NotFoundException {
		List<EntityHeader> pathHeaders = nodeManager.getNodePathAsAdmin(nodeId);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(pathHeaders);
		return entityPath;
	}

	static byte[] cleanSearchDocument(Document document)
			throws UnsupportedEncodingException, JSONObjectAdapterException {
		String serializedDocument = EntityFactory.createJSONStringForEntity(document);

		// AwesomeSearch pukes on control characters. Some descriptions have
		// control characters in them for some reason, in any case, just get rid
		// of all control characters in the search document
		String cleanedDocument = serializedDocument.replaceAll("\\p{Cc}", "");

		// Get rid of escaped control characters too
		cleanedDocument = cleanedDocument.replaceAll("\\\\u00[0,1][0-9,a-f]","");

		// AwesomeSearch expects UTF-8
		return cleanedDocument.getBytes("UTF-8");
	}

	@Override
	public Document formulateSearchDocument(Node node, NodeRevisionBackup rev,
			AccessControlList acl, EntityPath entityPath,  String wikiPagesText) throws DatastoreException, NotFoundException {
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
		if(entityPath != null && entityPath.getPath() != null){
			for(EntityHeader eh: entityPath.getPath()){
				if(eh.getId() != null && node.getId().equals(eh.getId())){
					ancestors.add(org.sagebionetworks.repo.model.jdo.KeyFactory.stringToKey(eh.getId()));
				}
			}
			// Add the fields.
			fields.setAncestors(ancestors);
		}

		
		fields.setNode_type(aliasCache.getPreferredAlias(node.getNodeType()));
		
		// The description contains the entity description and all wiki page text
		StringBuilder descriptionValue = new StringBuilder();
		if(node.getDescription() != null){
			descriptionValue.append(node.getDescription());
		}
		if(wikiPagesText != null){
			descriptionValue.append(wikiPagesText);
		}
		// Set the description
		fields.setDescription(descriptionValue.toString());
		
		fields.setCreated_by(getDisplayNameForPrincipalId(node.getCreatedByPrincipalId()));
		fields.setCreated_on(node.getCreatedOn().getTime() / 1000);
		fields.setModified_by(getDisplayNameForPrincipalId(node.getModifiedByPrincipalId()));
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
		addAnnotationsToSearchDocument(fields, rev.getNamedAnnotations()
				.getPrimaryAnnotations());
		addAnnotationsToSearchDocument(fields, rev.getNamedAnnotations()
				.getAdditionalAnnotations());

		// References, just put the node id to which the reference refers. Not
		// currently adding the version or the type of the reference (e.g.,
		// code/input/output)
		if ((null != node.getReferences()) && (0 < node.getReferences().size())) {
			List<String> referenceValues = new ArrayList<String>();
			fields.setReferences(referenceValues);
			for (Set<Reference> refs : node.getReferences().values()) {
				for (Reference ref : refs) {
					if (FIELD_VALUE_SIZE_LIMIT > referenceValues.size()) {
						referenceValues.add(ref.getTargetId());
					} else {
						log.warn("Had to leave reference " + ref.getTargetId()
								+ " out of search document " + node.getId()
								+ " due to AwesomeSearch limits");
					}
				}
			}
		}

		// READ and UPDATE ACLs
		List<String> readAclValues = new ArrayList<String>();
		fields.setAcl(readAclValues);
		List<String> updateAclValues = new ArrayList<String>();
		fields.setUpdate_acl(updateAclValues);
		for (ResourceAccess access : acl.getResourceAccess()) {
			if (access.getAccessType().contains(
					ACCESS_TYPE.READ)) {
				if (FIELD_VALUE_SIZE_LIMIT > readAclValues.size()) {
					readAclValues.add(access.getPrincipalId().toString()); // TODO could look up display name from UserProfile and substitute here
				} else {
					log.error("Had to leave READ acl " + access.getPrincipalId()
							+ " out of search document " + node.getId()
							+ " due to AwesomeSearch limits");
				}
			}
			if (access.getAccessType().contains(
					ACCESS_TYPE.UPDATE)) {
				if (FIELD_VALUE_SIZE_LIMIT > updateAclValues.size()) {
					updateAclValues.add(access.getPrincipalId().toString()); // TODO could look up display name from UserProfile and substitute here
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

	private String getDisplayNameForPrincipalId(long principalId) {
		String displayName = ""+principalId;
		try {
			displayName = userManager.getDisplayName(principalId);
		} catch (NotFoundException ex) {
			// this is a best-effort attempt to fill in the display name and
			// this will happen for the 'bootstrap' user and users we may delete
			// from our system but are still the creators/modifiers of entities
			log.debug("Unable to get display name for principal id: " + principalId + ",", ex);
		} catch (Exception ex) {
			log.warn("Unable to get display name for principal id: " + principalId + ",", ex);
		}
		return displayName;
	}

	@SuppressWarnings("unchecked")
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
	public Document formulateSearchDocument(String nodeId) throws DatastoreException, NotFoundException {
		if(nodeId == null) throw new IllegalArgumentException("NodeId cannot be null");
		NodeBackup backup = backupManager.getNode(nodeId);
		return formulateFromBackup(backup);
	}

	@Override
	public boolean doesDocumentExist(String nodeId, String etag) {
		return backupManager.doesNodeExist(nodeId, etag);
	}
	
	/**
	 * Get all wiki text for an entity.
	 * @param nodeId
	 * @return
	 * @throws DatastoreException
	 */
	public String getAllWikiPageText(String nodeId) throws DatastoreException{
		// Lookup all wiki pages for this node
		try {
			List<WikiHeader> wikiHeaders = wikiPageDao.getHeaderTree(nodeId, ObjectType.ENTITY);
			if(wikiHeaders == null) return null;
			// For each header get the wikipage
			StringBuilder builder = new StringBuilder();
			for(WikiHeader header: wikiHeaders){
				WikiPage page = wikiPageDao.get(new WikiPageKey(nodeId, ObjectType.ENTITY,  header.getId()));
				// Append the title and markdown
				if(page.getTitle() != null){
					builder.append("/n");
					builder.append(page.getTitle());
				}
				if(page.getMarkdown() != null){
					builder.append("/n");
					builder.append(page.getMarkdown());
				}
			}
			return builder.toString();
		} catch (NotFoundException e) {
			// There is no WikiPage for this node.
			return null;
		}
	}

}
