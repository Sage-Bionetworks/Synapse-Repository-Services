package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;

import com.thoughtworks.xstream.XStream;

public class PartialRowUtils {

	/**
	 * Write the passed PartialRowSet as compressed XML to the passed output stream.
	 * 
	 * @param columns
	 * @param delta
	 * @throws IOException 
	 */
	public static void writePartialRows(PartialRowSet delta, OutputStream out) throws IOException {
		GZIPOutputStream zipOut = null;
		try{
			zipOut = new GZIPOutputStream(out);
			XStream xstream = createXStream();
			xstream.toXML(delta, zipOut);
			zipOut.flush();
		}finally{
			IOUtils.closeQuietly(zipOut);
		}
	}


	/**
	 * Read the compressed XML from the given input stream.
	 * 
	 * @param input
	 * @return
	 * @throws IOException
	 */
	public static PartialRowSet readPartialRows(
			InputStream input) throws IOException {
		GZIPInputStream zipIn = null;
		try{
			zipIn = new GZIPInputStream(input);
			XStream xstream = createXStream();
			return (PartialRowSet) xstream.fromXML(zipIn);
		}finally{
			IOUtils.closeQuietly(zipIn);
		}
	}
	
	private static XStream createXStream(){
		XStream xstream = new XStream();
		xstream.alias("PartialRowSet", PartialRowSet.class);
		xstream.alias("PartialRow", PartialRow.class);
		return xstream;
	}
}
