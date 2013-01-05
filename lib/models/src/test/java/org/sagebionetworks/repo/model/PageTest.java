package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
public class PageTest {

       @Test
       public void testRoundTripPage() throws JSONObjectAdapterException {
               Page f1 = new Page();
               JSONObjectAdapter adapter1 = new JSONObjectAdapterImpl();
               JSONObjectAdapter adapter2 = new JSONObjectAdapterImpl();
               Date d = new Date();
               
               f1.setAccessControlList("/acl");
               f1.setAnnotations("/annotations");
               f1.setCreatedBy("createdBy");
               f1.setCreatedOn(d);
               f1.setDescription("description");
               f1.setEtag("1");
               f1.setId("1");
               f1.setModifiedBy("modifiedBy");
               f1.setModifiedOn(d);
               f1.setName("name");
               f1.setParentId("0");
               f1.setUri("uri");
               
               adapter1 = f1.writeToJSONObject(adapter1);
               String s = adapter1.toJSONString();
               adapter2 = new JSONObjectAdapterImpl(s);
               Page f2 = new Page(adapter2);
               
               assertEquals(f1, f2);
       }
}