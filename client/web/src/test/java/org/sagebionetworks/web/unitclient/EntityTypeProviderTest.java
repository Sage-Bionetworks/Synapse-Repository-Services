package org.sagebionetworks.web.unitclient;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class EntityTypeProviderTest {
	EntityTypeProvider entityTypeProvider;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setup(){
		SynapseClientAsync synapseClient = mock(SynapseClientAsync.class);
		//String registryString = getRegistryJson();
		String registryString = "{    \"entityTypes\":[        {            \"validParentTypes\":[                \"/project\"            ],            \"urlPrefix\":\"/dataset\",            \"name\":\"dataset\",            \"className\":\"org.sagebionetworks.repo.model.Dataset\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/dataset\"            ],            \"urlPrefix\":\"/layer\",            \"name\":\"layer\",            \"className\":\"org.sagebionetworks.repo.model.Layer\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/dataset\",                \"/layer\",                \"/code\"            ],            \"urlPrefix\":\"/location\",            \"name\":\"location\",            \"className\":\"org.sagebionetworks.repo.model.Location\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/folder\",                \"/project\",                \"DEFAULT\"            ],            \"urlPrefix\":\"/project\",            \"name\":\"project\",            \"className\":\"org.sagebionetworks.repo.model.Project\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/layer\"            ],            \"urlPrefix\":\"/preview\",            \"name\":\"preview\",            \"className\":\"org.sagebionetworks.repo.model.Preview\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"DEFAULT\",                \"/folder\"            ],            \"urlPrefix\":\"/eula\",            \"name\":\"eula\",            \"className\":\"org.sagebionetworks.repo.model.Eula\",            \"defaultParentPath\":\"/root/eulas\"        },        {            \"validParentTypes\":[                \"DEFAULT\",                \"/folder\"            ],            \"urlPrefix\":\"/agreement\",            \"name\":\"agreement\",            \"className\":\"org.sagebionetworks.repo.model.Agreement\",            \"defaultParentPath\":\"/root/agreements\"        },        {            \"validParentTypes\":[                \"DEFAULT\",                \"/folder\"            ],            \"urlPrefix\":\"/folder\",            \"name\":\"folder\",            \"className\":\"org.sagebionetworks.repo.model.Folder\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/project\"            ],            \"urlPrefix\":\"/analysis\",            \"name\":\"analysis\",            \"className\":\"org.sagebionetworks.repo.model.Analysis\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/folder\",                \"/analysis\",                \"DEFAULT\"            ],            \"urlPrefix\":\"/step\",            \"name\":\"step\",            \"className\":\"org.sagebionetworks.repo.model.Step\",            \"defaultParentPath\":\"/root\"        },        {            \"validParentTypes\":[                \"/project\"            ],            \"name\":\"code\",            \"urlPrefix\":\"/code\",            \"className\":\"org.sagebionetworks.repo.model.Code\",            \"defaultParentPath\":\"/root\"        }            ]}";
		AsyncMockStubber.callSuccessWith(registryString).when(synapseClient).getEntityTypeRegistryJSON(any(AsyncCallback.class));
		JSONObjectAdapter jsonObjectAdapter = new JSONObjectAdapterImpl();
		entityTypeProvider = new EntityTypeProvider(synapseClient, jsonObjectAdapter);
	}

	@Test
	public void testGetProjectEntity() {
		String uri = "http://HOST/PATH/project/ID";
		EntityType projectType = entityTypeProvider.getEntityTypeForUri(uri);
		assertNotNull(projectType);
	}	

	
	
	
	
	/*
	 * Private Methods
	 */	
	private String getRegistryJson() {
		ClassLoader classLoader = EntityType.class.getClassLoader();
		InputStream in = classLoader.getResourceAsStream(org.sagebionetworks.repo.model.EntityType.REGISTER_JSON_FILE_NAME);
		if(in == null) throw new IllegalStateException("Cannot find the "+org.sagebionetworks.repo.model.EntityType.REGISTER_JSON_FILE_NAME+" file on the classpath");
		String registryString = "";
		try {
			registryString = readToString(in);
		} catch (IOException e) {
			// error reading file
		}
		return registryString;
	}	
	

	/**
	 * Read an input stream into a string.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static String readToString(InputStream in) throws IOException {
		try {
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, "UTF-8"));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}

}
