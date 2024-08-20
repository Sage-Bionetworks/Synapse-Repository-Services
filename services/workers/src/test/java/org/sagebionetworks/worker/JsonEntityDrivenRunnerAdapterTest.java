package org.sagebionetworks.worker;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class JsonEntityDrivenRunnerAdapterTest {

	@Mock
	private TypedMessageDrivenRunner<TestEntity> mockRunner;

	@InjectMocks
	private JsonEntityDrivenRunnerAdapter<TestEntity> adapter;

	@Mock
	private ProgressCallback mockCallback;

	@Mock
	private Message mockMessage;

	@Test
	public void testRun() throws Exception {

		when(mockRunner.getObjectClass()).thenReturn(TestEntity.class);
		when(mockMessage.getBody()).thenReturn("{\"someProperty\": \"someValue\"}");

		TestEntity expectedEntity = new TestEntity().setSomeProperty("someValue");

		// Call under test
		adapter.run(mockCallback, mockMessage);

		verify(mockRunner).run(mockCallback, mockMessage, expectedEntity);

	}
	
	@Test
	public void testRunWithTopicMessage() throws Exception {

		when(mockRunner.getObjectClass()).thenReturn(TestEntity.class);
		when(mockMessage.getBody()).thenReturn("{\"Message\": {\"someProperty\": \"someValue\"}, \"TopicArn\": \"topicArn\"}");

		TestEntity expectedEntity = new TestEntity().setSomeProperty("someValue");

		// Call under test
		adapter.run(mockCallback, mockMessage);

		verify(mockRunner).run(mockCallback, mockMessage, expectedEntity);

	}

	public static class TestEntity implements JSONEntity {

		private String someProperty;

		public TestEntity() {
		}

		public TestEntity(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
			this.initializeFromJSONObject(adapter);
		}

		public String getSomeProperty() {
			return someProperty;
		}

		public TestEntity setSomeProperty(String someProperty) {
			this.someProperty = someProperty;
			return this;
		}

		@Override
		public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
			if (!adapter.isNull("someProperty")) {
				someProperty = adapter.getString("someProperty");
			}
			return adapter;
		}

		@Override
		public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter adapter) throws JSONObjectAdapterException {
			if (someProperty != null) {
				adapter.put("someProperty", someProperty);
			}
			return adapter;
		}

		@Override
		public int hashCode() {
			return Objects.hash(someProperty);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof TestEntity)) {
				return false;
			}
			TestEntity other = (TestEntity) obj;
			return Objects.equals(someProperty, other.someProperty);
		}

	}

}
