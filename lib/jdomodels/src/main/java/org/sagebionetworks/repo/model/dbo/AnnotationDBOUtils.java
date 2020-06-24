package org.sagebionetworks.repo.model.dbo;

import org.sagebionetworks.evaluation.dbo.DoubleAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.LongAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.StringAnnotationDBO;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.AnnotationsUtils;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class AnnotationDBOUtils {

	public static LongAnnotationDBO createLongAnnotationDBO(Long ownerId, LongAnnotation anno) {
		if (anno == null || anno.getKey() == null) {
			throw new IllegalArgumentException("Annotations must have a non-null key!");
		}
		LongAnnotationDBO dbo = new LongAnnotationDBO();
		dbo.setAttribute(anno.getKey());
		dbo.setValue(anno.getValue());
		dbo.setOwnerId(ownerId);
		dbo.setIsPrivate(anno.getIsPrivate()==null? AnnotationsUtils.DEFAULT_ANNOTATION_PRIVACY: anno.getIsPrivate());
		return dbo;
	}

	public static StringAnnotationDBO createStringAnnotationDBO(Long ownerId, AnnotationBase anno) {
		if (anno == null || anno.getKey() == null) {
			throw new IllegalArgumentException("Annotations must have a non-null key!");
		}
		StringAnnotationDBO dbo = new StringAnnotationDBO();
		dbo.setAttribute(anno.getKey());
		dbo.setOwnerId(ownerId);
		dbo.setIsPrivate(anno.getIsPrivate()==null? AnnotationsUtils.DEFAULT_ANNOTATION_PRIVACY: anno.getIsPrivate());
		
		// we must manually handle different typed Annos, since the AnnotationBase interface
		// does not specify the getValue() method
		if (anno instanceof StringAnnotation) {
			StringAnnotation sa = (StringAnnotation) anno;
			String stringValue = sa.getValue();
			if((null != stringValue) && stringValue.length() > SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH){
				stringValue = stringValue.substring(0, SqlConstants.STRING_ANNOTATIONS_VALUE_LENGTH-1);
			}
			dbo.setValue(stringValue);
		} else if (anno instanceof DoubleAnnotation) {
			DoubleAnnotation da = (DoubleAnnotation) anno;
			if (da.getValue() != null) {
				dbo.setValue(da.getValue().toString());
			}
		} else if (anno instanceof LongAnnotation) {
			LongAnnotation la = (LongAnnotation) anno;
			if (la.getValue() != null) {
				dbo.setValue(la.getValue().toString());
			}
		} else {
			throw new IllegalArgumentException(
					"Unable to determine annotation type for key: " + anno.getKey());
		}
		
		return dbo;
	}

	public static DoubleAnnotationDBO createDoubleAnnotationDBO(Long ownerId, DoubleAnnotation anno) {
		if (anno == null || anno.getKey() == null) {
			throw new IllegalArgumentException("Annotations must have a non-null key!");
		}
		DoubleAnnotationDBO dbo = new DoubleAnnotationDBO();
		dbo.setAttribute(anno.getKey());
		dbo.setValue(anno.getValue());
		dbo.setOwnerId(ownerId);
		dbo.setIsPrivate(anno.getIsPrivate()==null? AnnotationsUtils.DEFAULT_ANNOTATION_PRIVACY: anno.getIsPrivate());
		return dbo;
	}
	
}
