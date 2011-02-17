package org.sagebionetworks.web.server;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.LinkColumnInfo;
import org.sagebionetworks.web.shared.UrlTemplate;

import com.thoughtworks.xstream.XStream;

/**
 * Currently, we are using XStream to marshal this object from xml.
 * 
 * @author jmhill
 *
 */
public class ColumnConfig {
	
	private List<HeaderData> columns = new ArrayList<HeaderData>();

	public List<HeaderData> getColumns() {
		return columns;
	}

	public void setColumns(List<HeaderData> columns) {
		this.columns = columns;
	}
	

	/**
	 * Marshal from xml
	 * @param in
	 * @return
	 */
	public static ColumnConfig fromXml(Reader in){
		XStream xstream = createXStream();
		return (ColumnConfig) xstream.fromXML(in);
	}
	
	/**
	 * Marshal to xml
	 * @param config
	 * @param out
	 */
	public static void toXml(ColumnConfig config, Writer out){
		XStream xstream = createXStream();
		xstream.toXML(config, out);
	}
	
	/**
	 * Setup some class aliasing.
	 * @return
	 */
	private static XStream createXStream(){
		XStream xstream = new XStream();
		// This lets us reference columns by their ids.
		xstream.setMode(XStream.ID_REFERENCES);
		xstream.alias("configuration", ColumnConfig.class);
		xstream.alias("column", ColumnInfo.class);
		xstream.aliasField("display", ColumnInfo.class, "displayName");
		xstream.useAttributeFor(ColumnInfo.class, "id");
		xstream.useAttributeFor(LinkColumnInfo.class, "id");
		xstream.alias("link-column", LinkColumnInfo.class);
		xstream.alias("url-template", UrlTemplate.class);
		return xstream;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columns == null) ? 0 : columns.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnConfig other = (ColumnConfig) obj;
		if (columns == null) {
			if (other.columns != null)
				return false;
		} else if (!columns.equals(other.columns))
			return false;
		return true;
	}
	

}
