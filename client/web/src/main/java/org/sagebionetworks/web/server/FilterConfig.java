package org.sagebionetworks.web.server;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.shared.DisplayableValue;
import org.sagebionetworks.web.shared.FilterEnumeration;

import com.thoughtworks.xstream.XStream;

public class FilterConfig {
	
	private List<FilterEnumeration> filters = new ArrayList<FilterEnumeration>();
	
	/**
	 * Marshal from xml
	 * @param in
	 * @return
	 */
	public static FilterConfig fromXml(Reader in){
		XStream xstream = createXStream();
		return (FilterConfig) xstream.fromXML(in);
	}
	
	/**
	 * Marshal to xml
	 * @param config
	 * @param out
	 */
	public static void toXml(FilterConfig config, Writer out){
		XStream xstream = createXStream();
		xstream.toXML(config, out);
	}
	
	/**
	 * Setup some class aliasing.
	 * @return
	 */
	private static XStream createXStream(){
		XStream xstream = new XStream();
		xstream.alias("configuration", FilterConfig.class);
		xstream.alias("filter", FilterEnumeration.class);
		xstream.alias("displayable", DisplayableValue.class);
		return xstream;
	}


	public List<FilterEnumeration> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterEnumeration> filters) {
		this.filters = filters;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filters == null) ? 0 : filters.hashCode());
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
		FilterConfig other = (FilterConfig) obj;
		if (filters == null) {
			if (other.filters != null)
				return false;
		} else if (!filters.equals(other.filters))
			return false;
		return true;
	}

}
