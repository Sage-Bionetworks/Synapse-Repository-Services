package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;

import org.sagebionetworks.repo.model.gaejdo.Annotatable;

import com.google.appengine.api.datastore.Text;

public interface AnnotationsAccessor<T extends Annotatable> {
	public Collection<T> getHavingStringAnnotation(String annot, String value);
	public Collection<T> getHavingIntegerAnnotation(String annot, Integer value);
	public Collection<T> getHavingBooleanAnnotation(String annot, Boolean value);
	public Collection<T> getHavingTextAnnotation(String annot, Text value);
	public Collection<T> getHavingFloatAnnotation(String annot, Float value);
	public Collection<T> getHavingDateAnnotation(String annot, Date value);
	public void close();
}
