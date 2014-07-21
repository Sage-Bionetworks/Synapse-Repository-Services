package org.sagebionetworks.repo.manager.migration;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;

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
	 * @param writer
	 */
	public static <B> void writeBackupToWriter(List<B> list, String alias, Writer writer) {
		if (list == null || list.size() < 1)
			return;
		XStream xstream = new XStream();
		xstream.alias(alias, list.get(0).getClass());
		xstream.toXML(list, writer);
	}
	
	/**
	 * Read a backup list from a stream
	 * @param clazz
	 * @param alias
	 * @param in
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <B> List<B> readBackupFromStream(Class<B> clazz, String alias, InputStream in){
		XStream xstream = new XStream();
		xstream.alias(alias, clazz);
		try{
			return (List<B>) xstream.fromXML(in);
		}catch(StreamException e){
			if(e.getMessage().indexOf("input contained no data") > 0){
				// Ignore empty files.
				return null;
			}else{
				throw new RuntimeException(e);
			}
		}
	}

}
