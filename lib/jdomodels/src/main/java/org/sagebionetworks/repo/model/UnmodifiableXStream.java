package org.sagebionetworks.repo.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import com.thoughtworks.xstream.XStream;

/**
 * Facade over an XStream object that makes it unmodifiable/unconfigurable.
 * Expose methods as needed, but DO NOT expose any methods which change the xStream configuration outside of the Builder.
 *
 * According to XStream documentation, when is configurations are unmodified, it can be considered thread-safe.
 * Creation of new XStream instances are expensive so it does not make sense to constantly create new instances.
 * Hold on to the built object for as long as possible.
 * http://x-stream.github.io/faq.html#Scalability
 *
 */
public class UnmodifiableXStream {
	private final XStream xStream;

	private UnmodifiableXStream(XStream xStream){
		this.xStream = xStream;
	}

	public void toXML(Object obj, Writer out){
		xStream.toXML(obj, out);
	}

	public void toXML(Object obj, OutputStream out){
		xStream.toXML(obj, out);
	}

	public Object fromXML(InputStream input){
		return xStream.fromXML(input);
	}

	public Object fromXML(InputStream input, Object root){
		return xStream.fromXML(input, root);
	}

	public Object fromXML(Reader reader){
		return xStream.fromXML(reader);
	}

	public Object fromXML(String input, Object root){
		return xStream.fromXML(input, root);
	}

	///////////
	// Builder
	///////////

	public static UnmodifiableXStream.Builder builder(){
		return new Builder();
	}

	public static class Builder{
		private XStream xStream;

		private Builder(){
			this.xStream = new XStream();
			//on deserialization if an unrecognized field exists in the XML, ignore it.
			this.xStream.ignoreUnknownElements();
		}

		public Builder alias(String name, Class type){
			xStream.alias(name, type);
			return this;
		}

		public Builder allowTypes(Class... types){
			xStream.allowTypes(types);
			return this;
		}

		public Builder allowTypeHierarchy(Class type){
			xStream.allowTypeHierarchy(type);
			return this;
		}

		public Builder omitField(Class definedIn, String fieldName){
			xStream.omitField(definedIn, fieldName);
			return this;
		}

		public UnmodifiableXStream build(){
			return new UnmodifiableXStream(xStream);
		}
	}
}
