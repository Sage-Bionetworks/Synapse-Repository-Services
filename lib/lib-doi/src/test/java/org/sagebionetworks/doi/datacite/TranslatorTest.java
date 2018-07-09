package org.sagebionetworks.doi.datacite;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.doi.*;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TranslatorTest {

	@Mock
	private DoiMetadata mockMetadata;
	@Mock
	private List<DoiCreator> mockDoiCreators;
	@Mock
	private DoiCreator mockCreator1;
	@Mock
	private DoiCreator mockCreator2;
	@Mock
	private DoiCreator mockCreator3;
	@Mock
	private List<DoiTitle> mockDoiTitles;
	@Mock
	private DoiTitle mockTitle1;
	@Mock
	private DoiTitle mockTitle2;
	@Mock
	private DoiTitle mockTitle3;
	@Mock
	private DoiResourceType mockResourceType;

	@Before
	public void setup() {
		when(mockMetadata.getCreators()).thenReturn(mockDoiCreators);
		when(mockDoiCreators.get(0)).thenReturn(mockCreator1);
		when(mockCreator1.getCreatorName()).thenReturn("Creator");
		when(mockMetadata.getTitles()).thenReturn(mockDoiTitles);
		when(mockDoiTitles.get(0)).thenReturn(mockTitle1);
		when(mockTitle1.getTitle()).thenReturn("Title");
		when(mockMetadata.getPublicationYear()).thenReturn(Long.valueOf(2000));
		when(mockMetadata.getResourceType()).thenReturn(mockResourceType);
	}

	//Test a DataCite schema 2.2 XML String
	@Test
	public void Datacite2ToDoiMetadataTest() throws Exception{
		when(mockResourceType.getResourceTypeText()).thenReturn("");
		ClassLoader loader = this.getClass().getClassLoader();
		String path = loader.getResource("Datacite2Sample.xml").getFile();
		String xml = FileUtils.readFileToString(new File(path));
		DataciteTranslator translator = new DataciteTranslator();
		DoiMetadata metadata = translator.translate(xml);
		assertEquals(mockMetadata.getCreators().get(0).getCreatorName(), metadata.getCreators().get(0).getCreatorName());
		assertEquals(mockMetadata.getTitles().get(0).getTitle(), metadata.getTitles().get(0).getTitle());
		assertEquals(mockMetadata.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(mockMetadata.getResourceType().getResourceTypeText(), metadata.getResourceType().getResourceTypeText());
		assertNull(metadata.getResourceType().getResourceTypeGeneral());
	}

	//Test a v3 XML String
	@Test
	public void Datacite3ToDoiMetadataTest() throws Exception {
		when(mockResourceType.getResourceTypeText()).thenReturn("");
		ClassLoader loader = this.getClass().getClassLoader();
		String path = loader.getResource("Datacite3Sample.xml").getFile();
		String xml = FileUtils.readFileToString(new File(path));
		DataciteTranslator translator = new DataciteTranslator();
		DoiMetadata metadata = translator.translate(xml);
		assertEquals(mockMetadata.getCreators().get(0).getCreatorName(), metadata.getCreators().get(0).getCreatorName());
		assertEquals(mockMetadata.getTitles().get(0).getTitle(), metadata.getTitles().get(0).getTitle());
		assertEquals(mockMetadata.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(mockMetadata.getResourceType().getResourceTypeText(), metadata.getResourceType().getResourceTypeText());
		assertNull(metadata.getResourceType().getResourceTypeGeneral());
	}

	//Test a v4 XML String
	@Test
	public void Datacite4ToDoiMetadataTest() throws Exception{
		when(mockResourceType.getResourceTypeText()).thenReturn("(:unav)");
		ClassLoader loader = this.getClass().getClassLoader();
		String path = loader.getResource("Datacite4Sample.xml").getFile();
		String xml = FileUtils.readFileToString(new File(path));
		DataciteTranslator translator = new DataciteTranslator();
		DoiMetadata metadata = translator.translate(xml);
		assertEquals(mockMetadata.getCreators().get(0).getCreatorName(), metadata.getCreators().get(0).getCreatorName());
		assertEquals(mockMetadata.getTitles().get(0).getTitle(), metadata.getTitles().get(0).getTitle());
		assertEquals(mockMetadata.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(mockMetadata.getResourceType().getResourceTypeText(), metadata.getResourceType().getResourceTypeText());
		assertEquals(DoiResourceTypeGeneral.Other, DoiResourceTypeGeneral.valueOf(metadata.getResourceType().getResourceTypeGeneral().name()));
	}

	//Test a v4 XML String
	@Test
	public void MultipleAuthorsTitlesTest() throws Exception{
		when(mockDoiCreators.size()).thenReturn(3);
		when(mockDoiTitles.size()).thenReturn(3);
		when(mockDoiCreators.get(1)).thenReturn(mockCreator2);
		when(mockDoiCreators.get(2)).thenReturn(mockCreator3);
		when(mockCreator1.getCreatorName()).thenReturn("Creator 1");
		when(mockCreator2.getCreatorName()).thenReturn("Creator 2");
		when(mockCreator3.getCreatorName()).thenReturn("Creator 3");
		when(mockDoiTitles.get(1)).thenReturn(mockTitle2);
		when(mockDoiTitles.get(2)).thenReturn(mockTitle3);
		when(mockTitle1.getTitle()).thenReturn("Title 1");
		when(mockTitle2.getTitle()).thenReturn("Title 2");
		when(mockTitle3.getTitle()).thenReturn("Title 3");

		when(mockResourceType.getResourceTypeText()).thenReturn("(:unav)");
		ClassLoader loader = this.getClass().getClassLoader();
		String path = loader.getResource("Datacite4SampleMultiAuthor.xml").getFile();
		String xml = FileUtils.readFileToString(new File(path));
		DataciteTranslator translator = new DataciteTranslator();
		DoiMetadata metadata = translator.translate(xml);
		assertEquals(mockMetadata.getCreators().size(), metadata.getCreators().size());
		assertEquals(mockMetadata.getTitles().size(), metadata.getTitles().size());
		for (int i = 0; i < metadata.getCreators().size(); i++) {
			assertEquals(mockMetadata.getCreators().get(i).getCreatorName(),
					metadata.getCreators().get(i).getCreatorName());
		}
		for (int i = 0; i < metadata.getTitles().size(); i++) {
			assertEquals(mockMetadata.getTitles().get(i).getTitle(),
					metadata.getTitles().get(i).getTitle());
		}

		assertEquals(mockMetadata.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(mockMetadata.getResourceType().getResourceTypeText(), metadata.getResourceType().getResourceTypeText());
		assertEquals(DoiResourceTypeGeneral.Other, DoiResourceTypeGeneral.valueOf(metadata.getResourceType().getResourceTypeGeneral().name()));
	}

	@Test
	public void TestNoResourceText() throws Exception{
		when(mockResourceType.getResourceTypeText()).thenReturn("");
		ClassLoader loader = this.getClass().getClassLoader();
		String path = loader.getResource("Datacite4SampleNoResource.xml").getFile();
		String xml = FileUtils.readFileToString(new File(path));
		DataciteTranslator translator = new DataciteTranslator();
		DoiMetadata metadata = translator.translate(xml);
		assertEquals(mockMetadata.getCreators().get(0).getCreatorName(), metadata.getCreators().get(0).getCreatorName());
		assertEquals(mockMetadata.getTitles().get(0).getTitle(), metadata.getTitles().get(0).getTitle());
		assertEquals(mockMetadata.getPublicationYear(), metadata.getPublicationYear());
		assertEquals(mockMetadata.getResourceType().getResourceTypeText(), metadata.getResourceType().getResourceTypeText());
		assertEquals(DoiResourceTypeGeneral.Other, DoiResourceTypeGeneral.valueOf(metadata.getResourceType().getResourceTypeGeneral().name()));
	}


}
