package org.sagebionetworks.repo.model.jdo.temp;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.jdo.JDOAnnotatable;
import org.sagebionetworks.repo.model.jdo.JDOAnnotationsUtils;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jdo.JdoTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TempAnnotationsDaoImpl implements TempAnnotationsDao {
	
	@Autowired
	private JdoTemplate jdoTemplate;

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(Class<JDOAnnotatable> ownerClass, String ownerId) {
//		// first look up the owner
//		JDOAnnotatable owner = jdoTemplate.getObjectById(ownerClass, Long.valueOf(ownerId));
//		if(owner == null) return null;
//		JDOAnnotations jdoAnno = owner.getAnnotations();
//		// TODO Auto-generated method stub
		return null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void setAnnoations(Class ownerClass, String ownerId,	Annotations newValues) {
//		JDOAnnotatable owner = jdoTemplate.getObjectById(ownerClass, Long.valueOf(ownerId));
//		if(owner  == null) throw new IllegalAccessError("Cannot find: "+ownerClass.getName()+" with id: "+ownerId);
//		// Get the old and delete them.
//		JDOAnnotations jdoAnno = owner.getAnnotations();
//		if(jdoAnno != null){
//			jdoTemplate.deletePersistent(jdoAnno);			
//		}
//
//		// Create the new annotations
//		jdoAnno = JDOAnnotationsUtils.createFromDTO(newValues);
//		owner.setAnnotations(jdoAnno);
//		jdoTemplate.makePersistent(owner);
	}

}
