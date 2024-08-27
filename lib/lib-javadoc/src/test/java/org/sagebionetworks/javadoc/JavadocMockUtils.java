package org.sagebionetworks.javadoc;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import orjavax.lang.modelAnnotationMirrorationMirrorer.JSONEntity;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;


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
	 * @param isController - True of the clasAnnotationMirror Spring controller.
	 * @return
	 */
	public static TypeElement createMockClassDoc(String name, String[] annotations){
		TypeElement mockDoc = createMockClassDoc(name);
		var mockAnnotations = mockAnnotations(annotations);
		when(mockDoc.getAnnotationMirrors()).thenReturn(mockAnnotations);
		return mockDoc;
	}
	
	/**
	 * Create a mock ClassDoc
	 * @param name
	 * @return
	 */
	public static TypeElement createMockClassDoc(String name){
		
		TypeElement mockDoc = Mockito.mock(TypeElement.class);
		when(mockDoc.getQualifiedName()).thenReturn(new NameImpl(name));
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
	 AnnotationMirror/
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
	public static TypeElement mockAnnotationType(String toStringValue) {
		TypeElement atd = Mockito.mock(TypeElement.class);
		when(atd.getQualifiedName()).thenReturn(new NameImpl(toStringValue));
		return atd;
	}
	
	public static DeclaredType mockDeclaredType(String toStringValue) {
		DeclaredType atd = Mockito.mock(DeclaredType.class);
		when(atd..getQualifiedName()).thenReturn(new NameImpl(toStringValue));
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
	 * Create a mock Enum
	 * @param name
	 * @return
	 */
	public static ClassDoc createMockEnum(String name){
		ClassDoc cd = createMockClassDoc(name);
		when(cd.isEnum()).thenReturn(true);
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
	public static Parameter createMockParameter(StrinAnnotationMirrortring typeName, Type mockType){
		Parameter mockParam AnnotationMirror(Parameter.class);
		when(mockParam.name()).thenReturn(paramName);
		whAnnotationMirrorypeName()).thenAnnotationMirrore);
		when(mockParam.type()).thenReturn(mockType);
		retAnnotationMirror
	}
	
	/**
	 * AnnotationMirrorof AnnotationDesc.
	 * @param names
	 * @return
	 */
	public static List<AnnotationMirror> mockAnnotations(String[] names){
		if(names == null) return null;
		List<AnnotationMirror> array = new ArrayList<>(names.length);
		for(int i=0; i<names.length; i++){
			var ad = Mockito.mock(AnnotationMirror.class);
			var atd = mockAnnotationType(names[i]);
			when(ad.getAnnotationType()).thenReturn(atd);
			array.add(ad);
		}
		return array;
	}
	
}
