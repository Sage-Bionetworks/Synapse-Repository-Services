package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

class DoubleValueValidator implements AnnotationV2ValueValidator{
	@Override
	//todo: handle +inf -inf and nan
	public boolean validateValue(String value) {
		return false;
	}
}
