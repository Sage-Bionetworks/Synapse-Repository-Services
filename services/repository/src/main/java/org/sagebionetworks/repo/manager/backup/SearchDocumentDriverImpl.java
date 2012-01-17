package org.sagebionetworks.repo.manager.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class writes out search documents in batch.
 * 
 */
public class SearchDocumentDriverImpl implements NodeBackupDriver {

	static private Log log = LogFactory.getLog(SearchDocumentDriverImpl.class);

	private static final String PATH_DELIMITER = "/";
	private static final String CATCH_ALL_FIELD = "annotations";
	private static final Map<String, String> SEARCHABLE_NODE_ANNOTATIONS;

	@Autowired
	NodeBackupManager backupManager;
	@Autowired
	NodeSerializer nodeSerializer;

	// For now we can just create one of these. We might need to make beans in
	// the future.
	MigrationDriver migrationDriver = new MigrationDriverImpl();

	static {
		Map<String, String> searchableNodeAnnotations = new HashMap<String, String>();
		searchableNodeAnnotations.put("Disease", "disease");
		searchableNodeAnnotations.put("Tissue_Tumor", "tissue");
		searchableNodeAnnotations.put("Species", "species");
		searchableNodeAnnotations.put("Number_of_Samples", "num_samples");
		searchableNodeAnnotations.put("numSamples", "num_samples");
		searchableNodeAnnotations.put("platform", "platform");
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
		this.nodeSerializer = new NodeSerializerImpl();
	}

	@Override
	public boolean restoreFromBackup(File source, Progress progress)
			throws IOException, InterruptedException {
		return false;
	}

	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> entitiesToBackup) throws IOException,
			DatastoreException, NotFoundException, InterruptedException {
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
		outputStream.write('[');
		try {
			// First write the root node as its own entry
			for (String idToBackup : listToBackup) {
				// Recursively write each node.
				NodeBackup backup = backupManager.getNode(idToBackup);
				if (backup == null)
					throw new IllegalArgumentException("Cannot backup node: "
							+ idToBackup + " because it does not exists");
				writeSearchDocumentBatch(outputStream, backup, "", progress,
						isRecursive);
			}
		} catch (JSONException e) {
			throw new DatastoreException(e);
		} finally {
			if (outputStream != null) {
				outputStream.write(']');
				outputStream.flush();
				outputStream.close();
			}
		}
		return true;
	}

	/**
	 * This is a recursive method that will write the full tree of node data to
	 * the search document batch.
	 */
	private boolean writeSearchDocumentBatch(OutputStream outputStream,
			NodeBackup backup, String path, Progress progress,
			boolean isRecursive) throws JSONException, NotFoundException,
			DatastoreException, InterruptedException, IOException {
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
							progress, isRecursive);
				}
			}
		}
		return true;
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
	 */
	private void writeSearchDocument(OutputStream outputStream,
			NodeBackup backup, String path) throws JSONException,
			NotFoundException, DatastoreException, IOException {
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
		JSONObject document = formulateSearchDocument(node, rev,
				benefactorBackup.getAcl());

		outputStream.write(document.toString(4).getBytes());
		outputStream.write(",\n".getBytes());
		outputStream.flush();
	}

	private JSONObject formulateSearchDocument(Node node,
			NodeRevisionBackup rev, AccessControlList acl) throws JSONException {
		JSONObject document = new JSONObject();
		JSONObject fields = new JSONObject();

		// Node fields
		document.put("type", "add");
		document.put("id", node.getId());
		document.put("version", node.getETag());
		document.put("lang", "en");
		document.put("fields", fields);
		fields.put("node_type", node.getNodeType());
		fields.put("id", node.getId());
		fields.put("name", node.getName());
		if (null != node.getDescription()) {
			fields.put("description", node.getDescription());
		}
		fields.put("created_by", node.getCreatedBy());
		fields.put("created_on", node.getCreatedOn().getTime() / 1000);
		fields.put("modified_by", node.getModifiedBy());
		fields.put("modified_on", node.getModifiedOn().getTime() / 1000);

		// Annotations
		addAnnotationsToSearchDocument(fields, rev.getNamedAnnotations()
				.getPrimaryAnnotations());
		addAnnotationsToSearchDocument(fields, rev.getNamedAnnotations()
				.getAdditionalAnnotations());

		// References, just put the node id to which the reference refers. Not
		// currently adding the version or the type of the reference (e.g.,
		// code/input/output)
		if (0 < node.getReferences().size()) {
			JSONArray referenceValues = new JSONArray();
			fields.put("reference", referenceValues);
			for (Set<Reference> refs : node.getReferences().values()) {
				for (Reference ref : refs) {
					referenceValues.put(ref.getTargetId());
				}
			}
		}

		// ACL
		JSONArray aclValues = new JSONArray();
		fields.put("acl", aclValues);
		for (ResourceAccess access : acl.getResourceAccess()) {
			if (access.getAccessType().contains(
					AuthorizationConstants.ACCESS_TYPE.READ)) {
				aclValues.put(access.getGroupName());
			}
		}
		return document;
	}

	@SuppressWarnings("unchecked")
	private void addAnnotationsToSearchDocument(JSONObject fields,
			Annotations annots) throws JSONException {

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

			String searchFieldName = CATCH_ALL_FIELD;
			if (SEARCHABLE_NODE_ANNOTATIONS.containsKey(key)) {
				searchFieldName = SEARCHABLE_NODE_ANNOTATIONS.get(key);
			}

			JSONArray fieldValues = fields.optJSONArray(searchFieldName);
			if (null == fieldValues) {
				fieldValues = new JSONArray();
				fields.put(searchFieldName, fieldValues);
			}

			for (int i = 0; i < objs.length; i++) {
				if (CATCH_ALL_FIELD.equals(searchFieldName)) {
					// TODO dates to epoch time? or skip them?
					String catchAllValue = key + ":" + objs[i].toString();
					catchAllValue = catchAllValue.replaceAll("\\s", "_");
					fieldValues.put(catchAllValue);
				} else {
					fieldValues.put(objs[i]);
				}
			}
		}
	}
}
