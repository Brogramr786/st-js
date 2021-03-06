package org.stjs.generator.javac;

/*>>>
 import checkers.nullness.quals.Nullable;
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import com.sun.tools.javac.code.Symbol;

/**
 * A Utility class for analyzing {@code Element}s.
 *
 * @author acraciun
 * @version $Id: $Id
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(justification = "copied code", value = "BC_UNCONFIRMED_CAST")
@SuppressWarnings("PMD")
public final class ElementUtils {

	// Class cannot be instantiated.
	private ElementUtils() {
		throw new AssertionError("Class ElementUtils cannot be instantiated.");
	}

	/**
	 * Returns the innermost type element enclosing the given element
	 *
	 * @param elem the enclosed element of a class
	 * @return the innermost type element
	 */
	public static TypeElement enclosingClass(final Element elem) {
		Element result = elem;
		while (result != null && !result.getKind().isClass() && !result.getKind().isInterface()) {
			/* @Nullable */Element encl = result.getEnclosingElement();
			result = encl;
		}
		return (TypeElement) result;
	}

	/**
	 * Returns the innermost package element enclosing the given element. The same effect as
	 * {@link javax.lang.model.util.Elements#getPackageOf(Element)}. Returns the element itself if it is a package.
	 *
	 * @param elem the enclosed element of a package
	 * @return the innermost package element
	 */
	public static PackageElement enclosingPackage(final Element elem) {
		Element result = elem;
		while (result != null && result.getKind() != ElementKind.PACKAGE) {
			/* @Nullable */Element encl = result.getEnclosingElement();
			result = encl;
		}
		return (PackageElement) result;
	}

	/**
	 * Returns the "parent" package element for the given package element. For package "A.B" it gives "A". For package "A" it gives the default
	 * package. For the default package it returns null; Note that packages are not enclosed within each other, we have to manually climb the
	 * namespaces. Calling "enclosingPackage" on a package element returns the package element itself again.
	 *
	 * @param elem the package to start from
	 * @return the parent package element
	 * @param elem the package to start from
	 * @param e a {@link javax.lang.model.util.Elements} object.
	 */
	public static PackageElement parentPackage(final Elements e, final PackageElement elem) {
		String fqnstart = elem.getQualifiedName().toString();
		String fqn = fqnstart;
		if (!fqn.isEmpty() && fqn.contains(".")) {
			fqn = fqn.substring(0, fqn.lastIndexOf('.'));
			return e.getPackageElement(fqn);
		}
		return null;
	}

	/**
	 * Returns true if the element is a static element: whether it is a static field, static method, or static class
	 *
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @return true if element is static
	 */
	public static boolean isStatic(Element element) {
		return element.getModifiers().contains(Modifier.STATIC);
	}

	/**
	 * Returns true if the element is a final element: a final field, final method, or final class
	 *
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @return true if the element is final
	 */
	public static boolean isFinal(Element element) {
		return element.getModifiers().contains(Modifier.FINAL);
	}

	/**
	 * Returns true if the element is a effectively final element.
	 * @param element
	 * @return true if the element is effectively final
	 */
	// public static boolean isEffectivelyFinal(Element element) {
	// Symbol sym = (Symbol) element;
	// if (sym.getEnclosingElement().getKind() == ElementKind.METHOD &&
	// (sym.getEnclosingElement().flags() & ABSTRACT) != 0) {
	// return true;
	// }
	// return (sym.flags() & (FINAL | EFFECTIVELY_FINAL)) != 0;
	// }

	/**
	 * Returns the {@code TypeMirror} for usage of Element as a value. It returns the return type of a method element, the class type of a
	 * constructor, or simply the type mirror of the element itself.
	 *
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @return the type for the element used as a value
	 * @return the type for the element used as a value
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @return the type for the element used as a value
	 * @return the type for the element used as a value
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @return the type for the element used as a value
	 * @return the type for the element used as a value
	 */
	public static TypeMirror getType(Element element) {
		if (element.getKind() == ElementKind.METHOD) {
			return ((ExecutableElement) element).getReturnType();
		} else if (element.getKind() == ElementKind.CONSTRUCTOR) {
			return enclosingClass(element).asType();
		} else {
			return element.asType();
		}
	}

	/**
	 * Returns the qualified name of the inner most class enclosing the provided {@code Element}
	 *
	 * @param element an element enclosed by a class, or a {@code TypeElement}
	 * @return The qualified {@code Name} of the innermost class enclosing the element
	 */
	public static/* @Nullable */Name getQualifiedClassName(Element element) {
		if (element.getKind() == ElementKind.PACKAGE) {
			PackageElement elem = (PackageElement) element;
			return elem.getQualifiedName();
		}

		TypeElement elem = enclosingClass(element);
		if (elem == null) {
			return null;
		}

		return elem.getQualifiedName();
	}

	/**
	 * Returns a verbose name that identifies the element.
	 *
	 * @param elt a {@link javax.lang.model.element.Element} object.
	 * @return a {@link java.lang.String} object.
	 */
	public static String getVerboseName(Element elt) {
		if (elt.getKind() == ElementKind.PACKAGE || elt.getKind().isClass()) {
			return getQualifiedClassName(elt).toString();
		} else {
			return getQualifiedClassName(elt) + "." + elt.toString();
		}
	}

	/**
	 * Check if the element is an element for 'java.lang.Object'
	 *
	 * @param element the type element
	 * @return true iff the element is java.lang.Object element
	 */
	public static boolean isObject(TypeElement element) {
		return element.getQualifiedName().contentEquals("java.lang.Object");
	}

	/**
	 * Returns true if the element is a constant time reference
	 *
	 * @param elt a {@link javax.lang.model.element.Element} object.
	 * @return a boolean.
	 */
	public static boolean isCompileTimeConstant(Element elt) {
		return elt != null && elt.getKind() == ElementKind.FIELD && ((VariableElement) elt).getConstantValue() != null;
	}

	/**
	 * Returns true if the element is declared in ByteCode. Always return false if elt is a package.
	 *
	 * @param elt a {@link javax.lang.model.element.Element} object.
	 * @return a boolean.
	 */
	public static boolean isElementFromByteCode(Element elt) {
		if (elt == null) {
			return false;
		}

		if (elt instanceof Symbol.ClassSymbol) {
			Symbol.ClassSymbol clss = (Symbol.ClassSymbol) elt;
			if (null != clss.classfile) {
				// The class file could be a .java file
				return clss.classfile.getName().endsWith(".class");
			} else {
				return false;
			}
		}
		return isElementFromByteCode(elt.getEnclosingElement(), elt);
	}

	/**
	 * Returns true if the element is declared in ByteCode. Always return false if elt is a package.
	 */
	private static boolean isElementFromByteCode(Element elt, Element orig) {
		if (elt == null) {
			return false;
		}
		if (elt instanceof Symbol.ClassSymbol) {
			Symbol.ClassSymbol clss = (Symbol.ClassSymbol) elt;
			if (null != clss.classfile) {
				// The class file could be a .java file
				return clss.classfile.getName().endsWith(".class") || clss.classfile.getName().endsWith(".class)")
						|| clss.classfile.getName().endsWith(".class)]");
			} else {
				return false;
			}
		}
		return isElementFromByteCode(elt.getEnclosingElement(), elt);
	}

	/**
	 * Returns the field of the class
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @param name a {@link java.lang.String} object.
	 * @return a {@link javax.lang.model.element.VariableElement} object.
	 */
	public static VariableElement findFieldInType(TypeElement type, String name) {
		for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
			if (field.getSimpleName().toString().equals(name)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * <p>findFieldsInType.</p>
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @param names a {@link java.util.Collection} object.
	 * @return a {@link java.util.Set} object.
	 */
	public static Set<VariableElement> findFieldsInType(TypeElement type, Collection<String> names) {
		Set<VariableElement> results = new HashSet<VariableElement>();
		for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
			if (names.contains(field.getSimpleName().toString())) {
				results.add(field);
			}
		}
		return results;
	}

	/**
	 * <p>isError.</p>
	 *
	 * @param element a {@link javax.lang.model.element.Element} object.
	 * @return a boolean.
	 */
	public static boolean isError(Element element) {
		return "com.sun.tools.javac.comp.Resolve$SymbolNotFoundError".equals(element.getClass().getName());
	}

	/**
	 * Does the given element need a receiver for accesses? For example, an access to a local variable does not require a receiver.
	 *
	 * @param element The element to test.
	 * @return whether the element requires a receiver for accesses.
	 */
	public static boolean hasReceiver(Element element) {
		return element.getKind() != ElementKind.LOCAL_VARIABLE && element.getKind() != ElementKind.PARAMETER
				&& element.getKind() != ElementKind.PACKAGE && !ElementUtils.isStatic(element);
	}

	/**
	 * Determine all type elements for the classes and interfaces referenced in the extends/implements clauses of the given type element. TODO:
	 * can we learn from the implementation of com.sun.tools.javac.model.JavacElements.getAllMembers(TypeElement)?
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @return a {@link java.util.List} object.
	 */
	public static List<TypeElement> getSuperTypes(TypeElement type) {
		return getSuperTypes(type, true);
	}

	/**
	 * <p>getSuperTypes.</p>
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @param addInterfaces a boolean.
	 * @return a {@link java.util.List} object.
	 */
	public static List<TypeElement> getSuperTypes(TypeElement type, boolean addInterfaces) {

		List<TypeElement> superelems = new ArrayList<TypeElement>();
		if (type == null) {
			return superelems;
		}

		// Set up a stack containing type, which is our starting point.
		Deque<TypeElement> stack = new ArrayDeque<TypeElement>();
		stack.push(type);

		while (!stack.isEmpty()) {
			TypeElement current = stack.pop();

			// For each direct supertype of the current type element, if it
			// hasn't already been visited, push it onto the stack and
			// add it to our superelems set.
			TypeMirror supertypecls = current.getSuperclass();
			if (supertypecls.getKind() != TypeKind.NONE) {
				TypeElement supercls = (TypeElement) ((DeclaredType) supertypecls).asElement();
				if (!superelems.contains(supercls)) {
					stack.push(supercls);
					superelems.add(supercls);
				}
			}
			if (addInterfaces) {
				for (TypeMirror supertypeitf : current.getInterfaces()) {
					TypeElement superitf = (TypeElement) ((DeclaredType) supertypeitf).asElement();
					if (!superelems.contains(superitf)) {
						stack.push(superitf);
						superelems.add(superitf);
					}
				}
			}
		}

		return Collections.<TypeElement> unmodifiableList(superelems);
	}

	/**
	 * Return all fields declared in the given type or any superclass/interface. TODO: should this use
	 * javax.lang.model.util.Elements.getAllMembers(TypeElement) instead of our own getSuperTypes?
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @return a {@link java.util.List} object.
	 */
	public static List<VariableElement> getAllFieldsIn(TypeElement type) {
		List<VariableElement> fields = new ArrayList<VariableElement>();
		fields.addAll(ElementFilter.fieldsIn(type.getEnclosedElements()));
		List<TypeElement> alltypes = getSuperTypes(type);
		for (TypeElement atype : alltypes) {
			fields.addAll(ElementFilter.fieldsIn(atype.getEnclosedElements()));
		}
		return Collections.<VariableElement> unmodifiableList(fields);
	}

	/**
	 * Return all methods declared in the given type or any superclass/interface. Note that no constructors will be returned. TODO: should this
	 * use javax.lang.model.util.Elements.getAllMembers(TypeElement) instead of our own getSuperTypes?
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @return a {@link java.util.List} object.
	 */
	public static List<ExecutableElement> getAllMethodsIn(TypeElement type) {
		return getAllMethodsIn(type, true);
	}

	/**
	 * <p>getAllMethodsIn.</p>
	 *
	 * @param type a {@link javax.lang.model.element.TypeElement} object.
	 * @param addInterfaces a boolean.
	 * @return a {@link java.util.List} object.
	 */
	public static List<ExecutableElement> getAllMethodsIn(TypeElement type, boolean addInterfaces) {
		List<ExecutableElement> meths = new ArrayList<ExecutableElement>();
		meths.addAll(ElementFilter.methodsIn(type.getEnclosedElements()));

		List<TypeElement> alltypes = getSuperTypes(type, addInterfaces);
		for (TypeElement atype : alltypes) {
			meths.addAll(ElementFilter.methodsIn(atype.getEnclosedElements()));
		}
		return Collections.<ExecutableElement> unmodifiableList(meths);
	}

	/**
	 * <p>sameSignature.</p>
	 *
	 * @param m1 a {@link javax.lang.model.element.ExecutableElement} object.
	 * @param m2 a {@link javax.lang.model.element.ExecutableElement} object.
	 * @return a boolean.
	 */
	public static boolean sameSignature(ExecutableElement m1, ExecutableElement m2) {
		if (!m1.getSimpleName().equals(m2.getSimpleName())) {
			return false;
		}
		if (m1.getParameters().size() != m2.getParameters().size()) {
			return false;
		}
		for (int i = 0; i < m1.getParameters().size(); ++i) {
			if (!m1.getParameters().get(i).asType().equals(m2.getParameters().get(i).asType())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * <p>getSameMethodFromParents.</p>
	 *
	 * @param model a {@link javax.lang.model.element.ExecutableElement} object.
	 * @return the methods from the parent classes having the same signature as the given method. it's useful when llong for annotations.
	 */
	public static List<ExecutableElement> getSameMethodFromParents(ExecutableElement model) {
		List<ExecutableElement> allMethods = ElementUtils.getAllMethodsIn(ElementUtils.enclosingClass(model), false);
		List<ExecutableElement> similar = new ArrayList<ExecutableElement>();
		for (ExecutableElement method : allMethods) {
			if (sameSignature(model, method)) {
				similar.add(method);
			}
		}
		return similar;
	}

	/**
	 * <p>isTypeKind.</p>
	 *
	 * @param elem a {@link javax.lang.model.element.Element} object.
	 * @return a boolean.
	 */
	public static boolean isTypeKind(Element elem) {
		return elem.getKind().isClass() || elem.getKind().isInterface();
	}
}
