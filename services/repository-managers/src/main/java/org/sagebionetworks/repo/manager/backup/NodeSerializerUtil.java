package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementBackup;
import org.sagebionetworks.repo.model.ActivityBackup;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.SubmissionBackup;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.backup.WikiPageAttachmentBackup;
import org.sagebionetworks.repo.model.backup.WikiPageBackup;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.provenance.Used;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.model.provenance.UsedURL;
import org.sagebionetworks.repo.web.NotFoundException;

import com.thoughtworks.xstream.XStream;

/**
 * A utility to read and write backup data.
 * @author jmhill
 *
 */
public class NodeSerializerUtil  {
	
	private static final String ALIAS_NODE_BACKUP = "node-backup";
	private static final String ALIAS_ACCESS_TYPE = "access-type";
	private static final String ALIAS_RESOURCE_ACCESS = "resource-access";
	private static final String ALIAS_NODE_REVISION = "node-revision";
	private static final String ALIAS_ANNOTATIONS = "annotations";
	private static final String ALIAS_NAME_SPACE = "name-space";
	private static final String ALIAS_ACTIVITY_BACKUP = "activity-backup";	
	private static final String ALIAS_USED_INTERFACE = "used-interface";
	private static final String ALIAS_USED_ENTITY = "used-entity";
	private static final String ALIAS_USED_URL = "used-url";
	private static final String ALIAS_COMPETITION = "competition";
	private static final String ALIAS_SUBMISSION = "submission";
	private static final String ALIAS_TRASHED_ENTITY = "trashed-entity";
	private static final String ALIAS_FAVORITE = "favorite";
	private static final String ALIAS_FILEHANDLE = "file-handle";
	private static final String ALIAS_WIKI_PAGE = "wiki-page";
	private static final String ALIAS_WIKI_ATTACHMENT = "wiki-attachment";
	private static final String ALIAS_DOI = "doi";


	/**
	 * Write to a stream
	 * @param node
	 * @param out
	 * @throws NotFoundException
	 */
	public static void writeNodeBackup(NodeBackup node, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writeNodeBackup(node, writer);
	}


	/**
	 * Write to a writer
	 * @param node
	 * @param writer
	 */
	public static void writeNodeBackup(NodeBackup node,	Writer writer) {
		// For now we just let xstream do the work
		XStream xstream = createXStream();
		xstream.toXML(node, writer);
	}


	/**
	 * Read from a stream
	 * @param in
	 * @return
	 */
	public static NodeBackup readNodeBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		NodeBackup backup = readNodeBackup(reader);
		return backup;
	}


	/**
	 * Read from a writer.
	 * @param reader
	 * @return
	 */
	public static NodeBackup readNodeBackup(Reader reader) {
		XStream xstream = createXStream();
		NodeBackup backup = new NodeBackup();
		xstream.fromXML(reader, backup);
		return backup;
	}
	
	public static void writeNodeRevision(NodeRevisionBackup revision, OutputStream out){
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writeNodeRevision(revision, writer);
	}
	
	public static void writeNodeRevision(NodeRevisionBackup revision, Writer writer){
		XStream xstream = createXStream();
		xstream.toXML(revision, writer);
	}
	
	public static NodeRevisionBackup readNodeRevision(InputStream in){
		InputStreamReader reader = new InputStreamReader(in);
		return readNodeRevision(reader);
	}
	
	public static NodeRevisionBackup readNodeRevision(Reader reader){
		XStream xstream = createXStream();
		NodeRevisionBackup rev = new NodeRevisionBackup();
		xstream.fromXML(reader, rev);
		return rev;
	}
	
	public static void writePrincipalBackups(Collection<PrincipalBackup> principalBackups, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(principalBackups, writer);
	}
	
	@SuppressWarnings("unchecked")
	public static Collection<PrincipalBackup> readPrincipalBackups(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (Collection<PrincipalBackup>)xstream.fromXML(reader);
	}

	public static void writeAccessRequirementBackup(AccessRequirementBackup arb, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(arb, writer);
	}
	
	public static AccessRequirementBackup readAccessRequirementBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (AccessRequirementBackup)xstream.fromXML(reader);
	}

	public static void writeActivityBackup(ActivityBackup actBackup, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(actBackup, writer);
	}
	
	public static ActivityBackup readActivityBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (ActivityBackup)xstream.fromXML(reader);
	}
	
	public static void writeSubmissionBackup(SubmissionBackup sb, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(sb, writer);
	}
	
	public static SubmissionBackup readSubmissionBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (SubmissionBackup) xstream.fromXML(reader);
	}

	public static void writeTrashedEntityBackup(TrashedEntity trash, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(trash, writer);
	}

	public static TrashedEntity readTrashedEntityBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (TrashedEntity) xstream.fromXML(reader);
	}

	public static void writeFavoriteBackup(Favorite favorite, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(favorite, writer);
	}
	
	public static Favorite readFavoriteBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (Favorite)xstream.fromXML(reader);
	}

	public static void writeDoiBackup(Doi doi, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(doi, writer);
	}

	public static Doi readDoiBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (Doi) xstream.fromXML(reader);
	}

	/**
	 * Write and object to a Stream
	 * @param object
	 * @param out
	 */
	public static <T> void writeToStream(T object, OutputStream out){
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(object, writer);
	}
	
	/**
	 * Read an object from a stream.
	 * @param in
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T readFromStream(InputStream in, Class<? extends T> clazz){
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (T)xstream.fromXML(reader);
	}
	
	private static XStream createXStream(){
		XStream xstream = new XStream();
		xstream.alias(ALIAS_NODE_BACKUP, NodeBackup.class);
		xstream.alias(ALIAS_ACCESS_TYPE, ACCESS_TYPE.class);
		xstream.alias(ALIAS_RESOURCE_ACCESS, ResourceAccess.class);
		xstream.alias(ALIAS_NODE_REVISION, NodeRevisionBackup.class);
		xstream.alias(ALIAS_ANNOTATIONS, Annotations.class);
		xstream.alias(ALIAS_NAME_SPACE, NamedAnnotations.class);
		xstream.alias(ALIAS_ACTIVITY_BACKUP, ActivityBackup.class);
		xstream.alias(ALIAS_USED_INTERFACE, Used.class);
		xstream.alias(ALIAS_USED_ENTITY, UsedEntity.class);
		xstream.alias(ALIAS_USED_URL, UsedURL.class);
		xstream.alias(ALIAS_SUBMISSION, SubmissionBackup.class);
		xstream.alias(ALIAS_TRASHED_ENTITY, TrashedEntity.class);
		xstream.alias(ALIAS_FAVORITE, Favorite.class);
		xstream.alias(ALIAS_FILEHANDLE, FileHandleBackup.class);
		xstream.alias(ALIAS_WIKI_PAGE, WikiPageBackup.class);
		xstream.alias(ALIAS_WIKI_ATTACHMENT, WikiPageAttachmentBackup.class);
		xstream.alias(ALIAS_DOI, Doi.class);
		return xstream;
	}

}
