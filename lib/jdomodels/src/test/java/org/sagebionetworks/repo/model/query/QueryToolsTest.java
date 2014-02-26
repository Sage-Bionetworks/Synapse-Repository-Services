package org.sagebionetworks.repo.model.query;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;

public class QueryToolsTest {

	@Test
	public void testAnnosToMap() {
		Annotations annos = new Annotations();
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setKey("sKey"); sa.setValue("sValue"); // note we leave 'isPrivate' as null
		stringAnnos.add(sa);
		annos.setStringAnnos(stringAnnos);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation da = new DoubleAnnotation();
		da.setKey("dKey"); da.setValue(0.0D); // note we leave 'isPrivate' as null
		doubleAnnos.add(da);
		annos.setDoubleAnnos(doubleAnnos);

		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setKey("lKey"); la.setValue(0L); // note we leave 'isPrivate' as null
		longAnnos.add(la);
		annos.setLongAnnos(longAnnos);

		QueryTools.annosToMap(false, annos);
	}

}
