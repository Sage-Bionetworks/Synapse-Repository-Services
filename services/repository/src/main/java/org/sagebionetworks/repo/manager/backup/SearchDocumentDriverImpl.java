package org.sagebionetworks.repo.manager.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriver;
import org.sagebionetworks.repo.manager.backup.migration.MigrationDriverImpl;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.jdo.NodeAliasCache;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
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
	private static final String CATCH_ALL_FIELD = "annotations";
	private static final String DISEASE_FIELD = "disease";
	private static final String TISSUE_FIELD = "tissue";
	private static final String SPECIES_FIELD = "species";
	private static final String PLATFORM_FIELD = "platform";
	private static final String NUM_SAMPLES_FIELD = "num_samples";
	private static final Map<String, String> SEARCHABLE_NODE_ANNOTATIONS;

	@Autowired
	NodeBackupManager backupManager;

	@Autowired
	NodeAliasCache aliasCache;

	@Autowired
	UserManager userManager;

	// For now we can just create one of these. We might need to make beans in
	// the future.
	MigrationDriver migrationDriver = new MigrationDriverImpl();

	static {
		// These are both node primary annotations and additional annotation
		// names
		Map<String, String> searchableNodeAnnotations = new HashMap<String, String>();
		searchableNodeAnnotations.put("disease", DISEASE_FIELD);
		searchableNodeAnnotations.put("Disease", DISEASE_FIELD);
		searchableNodeAnnotations.put("Tissue_Tumor", TISSUE_FIELD);
		searchableNodeAnnotations.put("sampleSource", TISSUE_FIELD);
		searchableNodeAnnotations.put("SampleSource", TISSUE_FIELD);
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
	 * @param destination
	 * @param progress
	 * @param entitiesToBackup
	 * @throws IOException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 * @throws JSONObjectAdapterException
	 */
	public void writeSearchDocument(File destination, Progress progress,
			Set<String> entitiesToBackup) throws IOException,
			DatastoreException, NotFoundException, InterruptedException,
			JSONObjectAdapterException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file does not exist: "
							+ destination.getAbsolutePath());
		if (progress == null)
			throw new IllegalArgumentException("Progress cannot be null");
		// If the entitiesToBackup is null then include the root
		List<String> listToBackup = new ArrayList<String>();
		boolean isRecursive = false;
		if (entitiesToBackup == null) {
			// Just add the root
			isRecursive = true;
			listToBackup.add(backupManager.getRootId());
		} else {
			// Add all of the entities from the set.
			isRecursive = false;
			listToBackup.addAll(entitiesToBackup);
		}
		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.setTotalCount(backupManager.getTotalNodeCount());
		// First write to the file
		FileOutputStream outputStream = new FileOutputStream(destination);
		// DEV NOTE: (1) AwesomeSearch cannot currently accept zipped content so
		// we are not making a ZipOutputStream here (2) AwesomeSearch expects a
		// raw JSON array so we cannot use something like
		// org.sagebionetworks.repo.model.search.DocumentBatch here . . . also
		// building up a gigantic DocumentBatch isn't appropriate for the
		// streaming we are doing here to help with memory usage when dealing
		// with a large batch of entities to send to search so this is better
		// anyway
		outputStream.write('[');
		boolean isFirstEntry = true;
		// First write the root node as its own entry
		for (String idToBackup : listToBackup) {
			// Recursively write each node.
			NodeBackup backup = backupManager.getNode(idToBackup);
			if (backup == null)
				throw new IllegalArgumentException("Cannot backup node: "
						+ idToBackup + " because it does not exists");
			writeSearchDocumentBatch(outputStream, backup, "", progress,
					isRecursive, isFirstEntry);
			isFirstEntry = false;
		}
		outputStream.write(']');
		outputStream.flush();
		outputStream.close();
	}

	/**
	 * This is a recursive method that will write the full tree of node data to
	 * the search document batch.
	 * 
	 * @throws JSONObjectAdapterException
	 */
	private void writeSearchDocumentBatch(OutputStream outputStream,
			NodeBackup backup, String path, Progress progress,
			boolean isRecursive, boolean isFirstEntry)
			throws NotFoundException, DatastoreException, InterruptedException,
			IOException, JSONObjectAdapterException {
		if (backup == null)
			throw new IllegalArgumentException("NodeBackup cannot be null");
		if (backup.getNode() == null)
			throw new IllegalArgumentException("NodeBackup.node cannot be null");
		Node node = backup.getNode();
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		// Since this could be called in a tight loop, we need to be
		// CPU friendly
		Thread.yield();
		path = path + node.getId() + PATH_DELIMITER;
		// Write this node
		// A well-formed JSON array does not end with a final comma, so here's
		// how we ensure we add the right commas
		if (isFirstEntry) {
			isFirstEntry = false;
		} else {
			outputStream.write(",\n".getBytes());
		}
		writeSearchDocument(outputStream, backup, path);
		progress.setMessage(backup.getNode().getName());
		progress.incrementProgress();
		log.info(progress.toString());
		// Check for termination.
		checkForTermination(progress);
		if (isRecursive) {
			// now write each child
			List<String> childList = backup.getChildren();
			if (childList != null) {
				for (String childId : childList) {
					NodeBackup child = backupManager.getNode(childId);
					writeSearchDocumentBatch(outputStream, child, path,
							progress, isRecursive, false);
				}
			}
		}
	}

	/**
	 * @param progress
	 * @throws InterruptedException
	 */
	public void checkForTermination(Progress progress)
			throws InterruptedException {
		// Between each node check to see if we should terminate
		if (progress.shouldTerminate()) {
			throw new InterruptedException(
					"Search document batch terminated by the user");
		}
	}

	/**
	 * Write a single search document
	 * 
	 * @throws JSONObjectAdapterException
	 */
	private void writeSearchDocument(OutputStream outputStream,
			NodeBackup backup, String path) throws NotFoundException,
			DatastoreException, IOException, JSONObjectAdapterException {
		if (backup == null)
			throw new IllegalArgumentException("NodeBackup cannot be null");
		if (backup.getNode() == null)
			throw new IllegalArgumentException("NodeBackup.node cannot be null");
		Node node = backup.getNode();
		if (node.getId() == null)
			throw new IllegalArgumentException("node.id cannot be null");
		String benefactorId = backup.getBenefactor();
		NodeBackup benefactorBackup = backupManager.getNode(benefactorId);
		Long revId = node.getVersionNumber();
		NodeRevisionBackup rev = backupManager.getNodeRevision(node.getId(),
				revId);

		Document document = formulateSearchDocument(node, rev, benefactorBackup
				.getAcl());
		outputStream.write(cleanSearchDocument(document));
		outputStream.flush();
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
	public Document formulateSearchDocument(Node node, NodeRevisionBackup rev,
			AccessControlList acl) throws DatastoreException, NotFoundException {
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
		fields.setNode_type(aliasCache.getPreferredAlias(node.getNodeType()));
		if (null != node.getDescription()) {
			fields.setDescription(node.getDescription());
		}
		fields.setCreated_by(getDisplayNameForUserId(node.getCreatedBy()));
		fields.setCreated_on(node.getCreatedOn().getTime() / 1000);
		fields.setModified_by(getDisplayNameForUserId(node.getModifiedBy()));
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
		fields.setAnnotations(new ArrayList<String>());
		fields.setDisease(new ArrayList<String>());
		fields.setSpecies(new ArrayList<String>());
		fields.setTissue(new ArrayList<String>());
		fields.setPlatform(new ArrayList<String>());
		fields.setNum_samples(new ArrayList<Long>());
		addAnnotationsToSearchDocument(fields, rev.getNamedAnnotations()
				.getPrimaryAnnotations());
		addAnnotationsToSearchDocument(fields, rev.getNamedAnnotations()
				.getAdditionalAnnotations());

		// Transform the annotations array back to an array containing a single
		// string since we often overflow the upper limit on value array length
		// for AwesomeSearch
		String joinedAnnotations = StringUtils.join(fields.getAnnotations(),
				" ");
		List<String> annotationsValue = new ArrayList<String>();
		annotationsValue.add(joinedAnnotations);
		fields.setAnnotations(annotationsValue);

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
					AuthorizationConstants.ACCESS_TYPE.READ)) {
				if (FIELD_VALUE_SIZE_LIMIT > readAclValues.size()) {
					readAclValues.add(access.getGroupName());
				} else {
					log.error("Had to leave READ acl " + access.getGroupName()
							+ " out of search document " + node.getId()
							+ " due to AwesomeSearch limits");
				}
			}
			if (access.getAccessType().contains(
					AuthorizationConstants.ACCESS_TYPE.UPDATE)) {
				if (FIELD_VALUE_SIZE_LIMIT > updateAclValues.size()) {
					updateAclValues.add(access.getGroupName());
				} else {
					log.error("Had to leave UPDATE acl "
							+ access.getGroupName()
							+ " out of search document " + node.getId()
							+ " due to AwesomeSearch limits");
				}
			}
		}
		return document;
	}

	private String getDisplayNameForUserId(String userId) {
		String displayName = userId;
		try {
			displayName = userManager.getUserInfo(userId).getUser()
					.getDisplayName();
		} catch (NotFoundException ex) {
			// this is a best-effort attempt to fill in the display name and
			// this will happen for the 'bootstrap' user and users we may delete
			// from our system but are still the creators/modifiers of entities
			log.debug("Unable to get display name for user id: " + userId + ",", ex);
		} catch (Exception ex) {
			log.warn("Unable to get display name for user id: " + userId + ",", ex);
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

				// Put ALL annotations into the catch-all field even if they are
				// also in a facet, this way we can discover them both by free
				// text AND faceted search

				// TODO dates to epoch time? or skip them?
				String catchAllValue = key + ":" + objs[i].toString();

				// A multi-word annotation gets underscores so we can
				// exact-match find it but I'm not positive this is actually
				// true because AwesomeSearch might be splitting free text on
				// underscores
				catchAllValue = catchAllValue.replaceAll("\\s", "_");
				addAnnotationToSearchDocument(fields, CATCH_ALL_FIELD,
						catchAllValue);
			}
		}
	}

	static void addAnnotationToSearchDocument(DocumentFields fields,
			String key, Object value) {
		if (CATCH_ALL_FIELD == key) {
			// Since the annotations field is a text field, after this we just
			// join it into a single string instead of truncating it here since
			// there is no need to truncate for free text
			fields.getAnnotations().add((String) value);
		} else if (DISEASE_FIELD == key
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
}
