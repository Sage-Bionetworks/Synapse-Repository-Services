package org.sagebionetworks.javadoc.web.services;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import org.sagebionetworks.repo.web.rest.doc.CSVGeneratedExample;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.web.bind.annotation.RequestMapping;

import jdk.javadoc.doclet.DocletEnvironment;

/**
 * Utilities for filtering elements by annotations.
 * 
 * @author John
 *
 */
public class FilterUtils {
	/**
	 * Create an iterator that only includes @Controller
	 * 
	 * @param classes
	 * @return
	 */
	public static Iterator<TypeElement> controllerIterator(DocletEnvironment docletEnvironment) {
		Set<TypeElement> types = ElementFilter.typesIn(docletEnvironment.getIncludedElements());
		types.forEach(t -> {
			System.out.println(t.toString());
			t.getAnnotationMirrors().forEach(m -> {
				System.out.println("\t" + m.toString());
			});
			System.out.println("annotation:" + t.getAnnotation(ControllerInfo.class));

		});

		return types.stream().filter(e -> e.getAnnotation(ControllerInfo.class) != null)
				.filter(e -> e instanceof TypeElement).map(e -> (TypeElement) e).collect(Collectors.toList())
				.iterator();
	}

	/**
	 * Create an iterator that only includes @RequestMapping methods.
	 * 
	 * @param classes
	 * @return
	 */
	public static Iterator<ExecutableElement> requestMappingIterator(TypeElement type) {
		if (type == null) {
			throw new IllegalArgumentException("includedElements cannot be null");
		}
		List<ExecutableElement> types = ElementFilter.methodsIn(type.getEnclosedElements());
		return types.stream().filter(e -> e.getAnnotation(RequestMapping.class) != null)
				.filter(e -> e.getAnnotation(Deprecated.class) == null).filter(e -> e instanceof ExecutableElement)
				.map(e -> (ExecutableElement) e).collect(Collectors.toList()).iterator();
	}

	/**
	 * Find all classes with the CSVGeneratedExample annotation.
	 * 
	 * @param classes
	 * @return
	 */
	public static Iterator<TypeElement> csvExampleIterator(Set<? extends Element> includedElements) {
		if (includedElements == null) {
			throw new IllegalArgumentException("includedElements cannot be null");
		}
		return includedElements.stream().filter(e -> e.getAnnotation(CSVGeneratedExample.class) != null)
				.filter(e -> e instanceof TypeElement).map(e -> (TypeElement) e).collect(Collectors.toList())
				.iterator();
	}

}
