package org.sagebionetworks.repo.manager.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.thoughtworks.xstream.XStream;

/**
 * Provides marshaling for backup objects.
 * 
 * @author John
 *
 */
public class BackupMarshalingUtils {
	
	/**
	 * Write a backup list to a Stream
	 * 
	 * @param clazz
	 * @param list
	 * @param alias
	 * @param out
	 */
	public static <B> void writeBackupToStream(List<B> list, String alias, OutputStream out){
		if(list == null || list.size() < 1) return;
		XStream xstream = new XStream();
		xstream.alias(alias, list.get(0).getClass());
		xstream.toXML(list, out);
	}
	
	/**
	 * Read a backup list from a stream
	 * @param clazz
	 * @param alias
	 * @param in
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <B> List<B> readBacckupFromStream(Class<B> clazz, String alias, InputStream in){
		XStream xstream = new XStream();
		xstream.alias(alias, clazz);
		return (List<B>) xstream.fromXML(in);
	}

}
