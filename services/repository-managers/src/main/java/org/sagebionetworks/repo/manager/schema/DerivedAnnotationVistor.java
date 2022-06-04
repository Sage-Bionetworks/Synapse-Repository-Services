package org.sagebionetworks.repo.manager.schema;

import org.everit.json.schema.event.ValidationListener;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;

public interface DerivedAnnotationVistor extends ValidationListener{

	Annotations getDerivedAnnotations();
}
