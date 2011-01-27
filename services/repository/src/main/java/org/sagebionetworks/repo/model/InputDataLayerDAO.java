package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOInputDataLayer;

import com.google.appengine.api.datastore.Key;

public interface InputDataLayerDAO  extends BaseDAO<InputDataLayer>, AnnotatableDAO<InputDataLayer>, RevisableDAO<InputDataLayer> {

}
