package org.sagebionetworks.javadoc;

import static org.mockito.Mockito.when;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONEntity;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

/**
 * Helper methods for mocking Javadoc objects.
 * 
 * @author John
 *
 */
public class JavadocMockUtils {
	/**
	 * Create a mock ClassDoc for testing.
	 * @param isController - True of the class represents a Spring controller.
	 * @return
	 */
	public static ClassDoc createMockClassDoc(String name, String[] annotations){
		ClassDoc mockDoc = createMockClassDoc(name);
		AnnotationDesc[] mockAnnotations = mockAnnotations(annotations);
		when(mockDoc.annotations()).thenReturn(mockAnnotations);
		return mockDoc;
	}
	
	/**
	 * Create a mock ClassDoc
	 * @param name
	 * @return
	 */
	public static ClassDoc createMockClassDoc(String name){
		ClassDoc mockDoc = Mockito.mock(ClassDoc.class);
		when(mockDoc.qualifiedName()).thenReturn(name);
		return mockDoc;
	}
	
	/**
	 * Create an array of ClassDocs from an array of names
	 * @param names
	 * @return
	 */
	public static ClassDoc[] createMockClassDocs(String[] names){
		ClassDoc[] array = new ClassDoc[names.length];
		for(int i=0; i<names.length; i++){
			array[i] = createMockClassDoc(names[i]);
		}
		return array;
	}
	
	/**
	 * Create a mock method for testing.
	 * @param name
	 * @param annotations
	 * @return
	 */
	public static MethodDoc createMockMethodDoc(String name, String[] annotations){
		MethodDoc mockDoc = createMockMethodDoc(name);
		AnnotationDesc[] mockAnnotations = mockAnnotations(annotations);
		when(mockDoc.annotations()).thenReturn(mockAnnotations);
		return mockDoc;
	}
	
	/**
	 * Create a mock method.
	 * @param name
	 * @return
	 */
	public static MethodDoc createMockMethodDoc(String name){
		MethodDoc mockDoc = Mockito.mock(MethodDoc.class);
		when(mockDoc.qualifiedName()).thenReturn(name);
		return mockDoc;
	}

	/**
	 * Create a mock annotations
	 * @param toStringValue
	 * @return
	 */
	public static AnnotationTypeDoc mockAnnotationType(String toStringValue) {
		AnnotationTypeDoc atd = Mockito.mock(AnnotationTypeDoc.class);
		when(atd.qualifiedName()).thenReturn(toStringValue);
		return atd;
	}
	
	/**
	 * Create a mock JSONEntity
	 * @param name
	 * @return
	 */
	public static ClassDoc createMockJsonEntity(String name){
		ClassDoc cd = createMockClassDoc(name);
		ClassDoc[] interfaces = createMockClassDocs(new String[]{JSONEntity.class.getName()});
		when(cd.interfaces()).thenReturn(interfaces);
		when(cd.qualifiedName()).thenReturn(name);
		return cd;
	}
	
	/**
	 * Create a mock Type.
	 * @param name
	 * @return
	 */
	public static Type createMockType(String name){
		return createMockType(name, createMockClassDoc(name));
	}
	
	/**
	 * Create a Type for the given name
	 * @param name
	 * @param mockDoc
	 * @return
	 */
	public static Type createMockType(String name, ClassDoc mockDoc){
		Type mockType = Mockito.mock(Type.class);
		when(mockType.qualifiedTypeName()).thenReturn(name);
		when(mockType.asClassDoc()).thenReturn(mockDoc);
		return mockType;
	}
	
	/**
	 * Create a mock Parameter.
	 * @param paramName - name of the parameter
	 * @param typeName - name of the parameter type.s
	 * @return
	 */
	public static Parameter createMockParameter(String paramName, String typeName){
		return createMockParameter(paramName, typeName, createMockType(typeName));
	}
	
	/**
	 * Create a mock Parameter
	 * @param paramName
	 * @param typeName
	 * @param mockType
	 * @return
	 */
	public static Parameter createMockParameter(String paramName, String typeName, Type mockType){
		Parameter mockParam = Mockito.mock(Parameter.class);
		when(mockParam.name()).thenReturn(paramName);
		when(mockParam.typeName()).thenReturn(typeName);
		when(mockParam.type()).thenReturn(mockType);
		return mockParam;
	}
	
	/**
	 * Mock an array of AnnotationDesc.
	 * @param names
	 * @return
	 */
	public static AnnotationDesc[] mockAnnotations(String[] names){
		if(names == null) return null;
		AnnotationDesc[] array = new AnnotationDesc[names.length];
		for(int i=0; i<names.length; i++){
			AnnotationDesc ad = Mockito.mock(AnnotationDesc.class);
			AnnotationTypeDoc atd = mockAnnotationType(names[i]);
			when(ad.annotationType()).thenReturn(atd);
			array[i] = ad;
		}
		return array;
	}
	
}
