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
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.CompetitionBackup;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.SubmissionBackup;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

import com.thoughtworks.xstream.XStream;

/**
 * A utility to read and write node backup data.
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
	private static final String ALIAS_ACTIVITY = "activity";


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

	public static void writeActivityBackup(Activity act, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(act, writer);
	}
	
	public static Activity readActivityBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (Activity)xstream.fromXML(reader);
	}
	
	public static void writeCompetitionBackup(CompetitionBackup cb, OutputStream out) {
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = createXStream();
		xstream.toXML(cb, writer);
	}
	
	public static CompetitionBackup readCompetitionBackup(InputStream in) {
		InputStreamReader reader = new InputStreamReader(in);
		XStream xstream = createXStream();
		return (CompetitionBackup) xstream.fromXML(reader);
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

	private static XStream createXStream(){
		XStream xstream = new XStream();
		xstream.alias(ALIAS_NODE_BACKUP, NodeBackup.class);
		xstream.alias(ALIAS_ACCESS_TYPE, ACCESS_TYPE.class);
		xstream.alias(ALIAS_RESOURCE_ACCESS, ResourceAccess.class);
		xstream.alias(ALIAS_NODE_REVISION, NodeRevisionBackup.class);
		xstream.alias(ALIAS_ANNOTATIONS, Annotations.class);
		xstream.alias(ALIAS_NAME_SPACE, NamedAnnotations.class);
		xstream.alias(ALIAS_ACTIVITY, Activity.class);
		return xstream;
	}

}
