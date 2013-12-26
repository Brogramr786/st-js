/**
 *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.stjs.generator.type;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.STATIC;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.stjs.generator.GeneratorConstants;
import org.stjs.generator.JavascriptClassGenerationException;
import org.stjs.generator.STJSRuntimeException;
import org.stjs.generator.utils.ClassUtils;
import org.stjs.generator.utils.Option;
import org.stjs.generator.utils.PreConditions;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author acraciun,ekaspi
 */
@Immutable
public class ClassWrapper implements TypeWrapper {
	private static final Function<Class<?>, ClassWrapper> WRAP_CLASS = new Function<Class<?>, ClassWrapper>() {
		@Override
		public ClassWrapper apply(Class<?> clazz) {
			return new ClassWrapper(clazz);
		}
	};

	private final Class<?> clazz;

	private Map<String, FieldWrapper> fields;
	private Map<String, TypeWrapper> types;
	private Multimap<String, MethodWrapper> methods;
	private List<MethodWrapper> constructors;

	public ClassWrapper(@Nonnull Class<?> clazz) {
		Preconditions.checkNotNull(clazz);
		this.clazz = clazz;
	}

	private FieldWrapper buildFieldWrapper(String name, TypeWrapper type, int modifiers, Class<?> ownerClass, TypeWrapper[] actualTypeArgs) {
		// substitute the field's type with the actual type if the field has a generic type
		return new FieldWrapper(name, substituteType(type, ownerClass, actualTypeArgs), modifiers, this, ownerClass.equals(clazz));
	}

	private TypeWrapper substituteTypeInParameterizedType(ParameterizedTypeWrapper origParamType, Class<?> ownerClass,
			@Nullable TypeWrapper[] typeArguments) {
		TypeWrapper[] argumentTypes = origParamType.getActualTypeArguments();
		TypeWrapper[] modifiedArgumentTypes = new TypeWrapper[argumentTypes.length];
		boolean modified = false;
		for (int i = 0; i < argumentTypes.length; ++i) {
			modifiedArgumentTypes[i] = substituteType(argumentTypes[i], ownerClass, typeArguments);
			modified = modified || modifiedArgumentTypes[i] != argumentTypes[i];
		}
		return modified ? origParamType.withArguments(modifiedArgumentTypes) : origParamType;
	}

	/**
	 * @return a TypeWrapper with the parameters replaced by the given arguments
	 */
	private TypeWrapper substituteType(TypeWrapper origType, Class<?> ownerClass, @Nullable TypeWrapper[] typeArguments) {
		if (typeArguments == null) {
			return origType;
		}
		PreConditions.checkState(ownerClass.getTypeParameters().length == typeArguments.length,
				"Difference length between class parameters (%d) and arguments list (%d)", ownerClass.getTypeParameters().length,
				typeArguments.length);
		if (origType instanceof ParameterizedTypeWrapper) {
			ParameterizedTypeWrapper origParamType = (ParameterizedTypeWrapper) origType;
			return substituteTypeInParameterizedType(origParamType, ownerClass, typeArguments);
		}
		for (int i = 0; i < ownerClass.getTypeParameters().length; ++i) {
			TypeVariable<?> typeVar = ownerClass.getTypeParameters()[i];
			if (origType.getName().equals(typeVar.getName())) {
				return fixWildcardBounds(typeArguments[i], typeVar);
			}
		}
		return origType;
	}

	/**
	 * when the a type variable T is replaced by a simple wildcard (?), it in fact inherits the extends of the type var
	 * 
	 * @param typeWrapper
	 * @param typeVar
	 * @return
	 */
	private TypeWrapper fixWildcardBounds(TypeWrapper typeWrapper, TypeVariable<?> typeVar) {
		if (!(typeWrapper instanceof WildcardTypeWrapper)) {
			return typeWrapper;
		}
		if (typeVar.getBounds().length == 0) {
			return typeWrapper;
		}
		WildcardType wctype = (WildcardType) typeWrapper.getType();
		boolean unbounded = wctype.getUpperBounds().length == 0 || wctype.getUpperBounds().length == 1
				&& wctype.getUpperBounds()[0] == Object.class;
		if (!unbounded) {
			// already bounded with something else
			return typeWrapper;
		}
		return TypeWrappers.wrap(new WildcardTypeImpl(wctype.getLowerBounds(), typeVar.getBounds()));
	}

	private TypeWrapper[] substituteTypes(TypeWrapper[] origTypes, Class<?> ownerClass, TypeWrapper[] actualTypeArgs) {
		if (origTypes.length == 0 || actualTypeArgs == null) {
			return origTypes;
		}
		TypeWrapper[] substTypes = new TypeWrapper[origTypes.length];
		for (int i = 0; i < origTypes.length; ++i) {
			substTypes[i] = substituteType(origTypes[i], ownerClass, actualTypeArgs);
		}
		return substTypes;
	}

	// CHECKSTYLE:OFF - too many params
	private MethodWrapper buildMethodWrapper(String name, TypeWrapper returnType, TypeWrapper[] parameterTypes, int modifiers,
			TypeVariableWrapper<Method>[] typeParameters, Class<?> ownerClass, TypeWrapper[] actualTypeArgs, Annotation[] annotations) {
		return new MethodWrapper(name, substituteType(returnType, ownerClass, actualTypeArgs), substituteTypes(parameterTypes, ownerClass,
				actualTypeArgs), modifiers, typeParameters, this, clazz.equals(ownerClass), annotations);
	}

	// CHECKSTYLE:ON - too many params

	@edu.umd.cs.findbugs.annotations.SuppressWarnings(
			value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", //
			justification = "null is actually for a raw class")
	private TypeWrapper[] getActualTypeArgs(Type type, Class<?> subTypeClass, TypeWrapper[] subTypeActualTypeArgs) {
		if (type instanceof Class<?>) {
			return null;
		}
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			TypeWrapper[] typeArguments = new TypeWrapper[ptype.getActualTypeArguments().length];
			for (int i = 0; i < typeArguments.length; ++i) {
				TypeWrapper actualType = TypeWrappers.wrap(ptype.getActualTypeArguments()[i]);
				typeArguments[i] = substituteType(actualType, subTypeClass, subTypeActualTypeArgs);
			}
			return typeArguments;
		}
		if (type instanceof GenericArrayType) {
			return null;
		}
		throw new JavascriptClassGenerationException(type.getClass().getName(), "Received unknown type:" + type + " of class:" + type.getClass());
	}

	private void prepareFieldsMethodsAndTypes() {
		if (fields != null) {
			return;
		}
		fields = new HashMap<String, FieldWrapper>();
		types = new HashMap<String, TypeWrapper>();
		methods = ArrayListMultimap.create();
		addFieldsMethodsAndTypes(getType(), null, null);

		// add constructors
		constructors = new ArrayList<MethodWrapper>();

	}

	private int getTotalNumberOfParamsInSource(ClassOrInterfaceDeclaration sourceDeclaration) {
		int totalParamsInSource = 0;
		if (sourceDeclaration.getMembers() != null) {
			for (BodyDeclaration member : sourceDeclaration.getMembers()) {
				if (member instanceof ConstructorDeclaration) {
					ConstructorDeclaration c = (ConstructorDeclaration) member;
					totalParamsInSource += c.getParameters() == null ? 0 : c.getParameters().size();
				}
			}
		}
		return totalParamsInSource;
	}

	private int getTotalNumberOfParamsInBinary() {
		int total = 0;
		for (Constructor<?> c : clazz.getDeclaredConstructors()) {
			if (!c.isSynthetic()) {
				total += c.getGenericParameterTypes().length;
			}
		}
		return total;
	}

	private int getConstructorCount() {
		int total = 0;
		for (Constructor<?> c : clazz.getDeclaredConstructors()) {
			if (!c.isSynthetic()) {
				total++;
			}
		}
		return total;
	}

	/**
	 * adds all the constructors of this class. It needs the source declaration in order to figure out how many
	 * parameters are added by the compiler to the constructors (for example for non-static inner classes). It does this
	 * by counting the parameters for the constructors in the source and also in the code (non synthetic ones). Only
	 * after that the constructors (from the source and from the binary class) may be matched.
	 * 
	 * @param sourceDeclaration
	 */
	public void addConstructors(ClassOrInterfaceDeclaration sourceDeclaration) {
		int totalParamsInSource = getTotalNumberOfParamsInSource(sourceDeclaration);
		int totalParamsInBinary = getTotalNumberOfParamsInBinary();
		int constructorCount = getConstructorCount();

		// enums receive label and ordinal as first arguments
		// for non-static inner classes the constructor contains as first parameter the type of the outer type
		int skipFirstParams = constructorCount == 0 ? 0 : (totalParamsInBinary - totalParamsInSource) / constructorCount;

		for (Constructor<?> c : clazz.getDeclaredConstructors()) {
			addConstructor(c, skipFirstParams);
		}
	}

	@SuppressWarnings("unchecked")
	private void addConstructor(Constructor<?> c, int skipFirstParams) {
		if (c.isSynthetic()) {
			return;
		}
		TypeWrapper[] paramTypes = TypeWrappers.wrap(c.getGenericParameterTypes());

		if (skipFirstParams > 0) {
			// remove the first unneeded params
			paramTypes = Arrays.copyOfRange(paramTypes, skipFirstParams, paramTypes.length);
		}
		TypeVariableWrapper<Method>[] typeParams = TypeWrappers.wrap(c.getTypeParameters());

		constructors.add(buildMethodWrapper("new", TypeWrappers.wrap(clazz), paramTypes, c.getModifiers(), typeParams, clazz,
				getActualTypeArgs(getType(), null, null), c.getAnnotations()));
	}

	private void addFieldsMethodsAndTypes(Type type, Class<?> aRawClass, TypeWrapper[] someActualTypeArgs) {
		TypeWrapper[] actualTypeArgs = someActualTypeArgs;
		Class<?> rawClass = aRawClass;
		boolean seenObjectClass = false;
		for (Type c = type; c != null; c = rawClass.getGenericSuperclass()) {
			actualTypeArgs = getActualTypeArgs(c, rawClass, actualTypeArgs);
			rawClass = ClassUtils.getRawClazz(c);
			seenObjectClass = seenObjectClass || rawClass == Object.class;
			addFields(rawClass, actualTypeArgs);
			addMethods(rawClass, actualTypeArgs);
			addTypes(rawClass);
			// add also the methods from interfaces (not really needed when the root is actual class, but need when the
			// root is interfaces)
			for (Type iface : rawClass.getGenericInterfaces()) {
				addFieldsMethodsAndTypes(iface, rawClass, actualTypeArgs);
			}
		}

		// add Object methods too for interfaces (for example "equals" method)
		if (!seenObjectClass) {
			addFieldsMethodsAndTypes(Object.class, Object.class, actualTypeArgs);
		}

	}

	private void addTypes(Class<?> rawClass) {
		for (Class<?> type : rawClass.getDeclaredClasses()) {
			types.put(type.getName(), TypeWrappers.wrap(type));
		}
	}

	private void addFields(Class<?> rawClass, TypeWrapper[] actualTypeArgs) {
		try {
			for (Field f : rawClass.getDeclaredFields()) {
				if (fields.get(f.getName()) == null) {
					// keep the version from the most specific class
					fields.put(f.getName(),
							buildFieldWrapper(f.getName(), TypeWrappers.wrap(f.getGenericType()), f.getModifiers(), rawClass, actualTypeArgs));
				}
			}
			if (rawClass.isArray()) {
				// add the "length" field not listed for generic arrays
				fields.put("length", buildFieldWrapper("length", TypeWrappers.wrap(int.class), Modifier.PUBLIC, rawClass, actualTypeArgs));
			}
		}
		catch (Error err) {
			throw new STJSRuntimeException("Error declaring class:" + rawClass.getName(), err);
		}
	}

	private void addMethods(Class<?> rawClass, TypeWrapper[] actualTypeArgs) {

		for (Method m : rawClass.getDeclaredMethods()) {
			if (m.isBridge() || m.isSynthetic()) {
				// skip the bridges as they don't correspond to actual code
				continue;
			}
			TypeWrapper[] paramTypes = TypeWrappers.wrap(m.getGenericParameterTypes());
			@SuppressWarnings("unchecked")
			TypeVariableWrapper<Method>[] typeParams = TypeWrappers.wrap(m.getTypeParameters());
			methods.put(
					m.getName(),
					buildMethodWrapper(m.getName(), TypeWrappers.wrap(m.getGenericReturnType()), paramTypes, m.getModifiers(), typeParams,
							rawClass, actualTypeArgs, m.getAnnotations()));
		}

	}

	@Override
	public Type getType() {
		return clazz;
	}

	public String getPackageName() {
		return clazz.getPackage().getName();
	}

	@Override
	public String getSimpleName() {
		return clazz.getSimpleName();
	}

	/**
	 * Returns the binary name of the wrapped classed (as defined in JLS §13.1) excluding the package name.
	 */
	public String getSimpleBinaryName() {
		String packageName = clazz.getPackage().getName();
		return clazz.getName().substring(packageName.length() + 1);
	}

	@Override
	public String getExternalName() {
		String name = clazz.getSimpleName();
		if (name.isEmpty()) {
			return GeneratorConstants.SPECIAL_INLINE_TYPE;
		}
		for (Class<?> c = clazz.getDeclaringClass(); c != null && !c.isAnonymousClass(); c = c.getDeclaringClass()) {
			name = c.getSimpleName() + "." + name;
		}
		return name;
	}

	public Option<ClassWrapper> getDeclaringClass() {
		Class<?> declaringClass = clazz.getDeclaringClass();
		return declaringClass == null ? Option.<ClassWrapper>none() : Option.some(new ClassWrapper(declaringClass));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TypeVariableWrapper<Class<?>>[] getTypeParameters() {
		TypeVariableWrapper[] typeVarWrappers = new TypeVariableWrapper[clazz.getTypeParameters().length];
		for (int i = 0; i < clazz.getTypeParameters().length; ++i) {
			typeVarWrappers[i] = TypeWrappers.wrap(clazz.getTypeParameters()[i]);
		}
		return typeVarWrappers;
	}

	public int getModifiers() {
		return clazz.getModifiers();
	}

	public Annotation[] getAnnotations() {
		return clazz.getAnnotations();
	}

	@Override
	public Option<FieldWrapper> findField(String name) {
		prepareFieldsMethodsAndTypes();
		FieldWrapper f = fields.get(name);
		return f == null ? Option.<FieldWrapper>none() : Option.some(f);
	}

	public List<MethodWrapper> findMethods(final String name) {
		return ImmutableList.copyOf(Iterables.filter(getDeclaredMethods(), new Predicate<MethodWrapper>() {
			@Override
			public boolean apply(MethodWrapper method) {
				return method.getName().equals(name);
			}
		}));
	}

	public <T> Option<ClassWrapper> getDeclaredClass(String name) {
		try {
			for (Class<?> innerClass : clazz.getDeclaredClasses()) {
				if (innerClass.getSimpleName().equals(name)) {
					return Option.some(new ClassWrapper(innerClass));
				}
			}
		}
		catch (SecurityException e) {
			throw new JavascriptClassGenerationException(name, e);
		}
		return Option.none();
	}

	@Override
	public Option<MethodWrapper> findMethod(final String name, TypeWrapper... paramTypes) {
		prepareFieldsMethodsAndTypes();
		Collection<MethodWrapper> wrappers = methods.get(name);
		if (wrappers == null) {
			return Option.none();
		}

		MethodWrapper w = MethodSelector.resolveMethod(wrappers, paramTypes);
		if (w != null) {
			return Option.some(w);
		}

		return Option.none();
	}

	public List<MethodWrapper> getDeclaredMethods() {
		prepareFieldsMethodsAndTypes();
		return Lists.newArrayList(methods.values());
	}

	@Override
	public Option<MethodWrapper> findConstructor(TypeWrapper... paramTypes) {
		MethodWrapper w = MethodSelector.resolveMethod(constructors, paramTypes);
		if (w != null) {
			return Option.some(w);
		}

		return Option.none();
	}

	public boolean hasDeclaredField(String fieldName) {
		return findField(fieldName).isDefined();
	}

	public Option<ClassWrapper> getSuperclass() {
		Class<?> superClass = clazz.getSuperclass();
		return superClass == null ? Option.<ClassWrapper>none() : Option.some(new ClassWrapper(superClass));
	}

	@Override
	public String getName() {
		return clazz.getName();
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public List<FieldWrapper> getDeclaredNonPrivateStaticFields() {
		return ImmutableList.copyOf(filter(getDeclaredFields(), new Predicate<FieldWrapper>() {
			@Override
			public boolean apply(FieldWrapper field) {
				return isStaticButNotPrivate(field.getModifiers());
			}
		}));
	}

	private boolean isStaticButNotPrivate(int modifiers) {
		return (modifiers & (PRIVATE | STATIC)) == STATIC;
	}

	public List<MethodWrapper> getDeclaredNonPrivateStaticMethods() {
		return ImmutableList.copyOf(filter(getDeclaredMethods(), new Predicate<MethodWrapper>() {

			@Override
			public boolean apply(MethodWrapper method) {
				return isStaticButNotPrivate(method.getModifiers());
			}
		}));
	}

	public List<ClassWrapper> getDeclaredNonPrivateStaticClasses() {

		return ImmutableList.copyOf(transform(filter(Arrays.asList(clazz.getDeclaredClasses()), new Predicate<Class<?>>() {
			@Override
			public boolean apply(Class<?> clazz) {
				return isStaticButNotPrivate(clazz.getModifiers());
			}
		}), WRAP_CLASS));
	}

	public List<TypeWrapper> getDeclaredClasses() {
		prepareFieldsMethodsAndTypes();
		return Lists.newArrayList(types.values());
	}

	public List<FieldWrapper> getDeclaredFields() {
		prepareFieldsMethodsAndTypes();
		return Lists.newArrayList(fields.values());
	}

	public List<MethodWrapper> findConstructors() {
		return ImmutableList.copyOf(constructors);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ClassWrapper other = (ClassWrapper) obj;
		return clazz == other.clazz;
	}

	@Override
	public int hashCode() {
		return clazz.hashCode();
	}

	public boolean isInnerType() {
		return clazz.getDeclaringClass() != null;
	}

	public boolean isAnonymousClass() {
		return clazz.isAnonymousClass();
	}

	public boolean hasAnonymousDeclaringClass() {
		if (clazz.getDeclaringClass() == null) {
			return false;
		}
		if (clazz.getDeclaringClass().isAnonymousClass()) {
			return true;
		}
		return this.getDeclaringClass().getOrNull().hasAnonymousDeclaringClass();
	}

	@Override
	public boolean isImportable() {
		return true;
	}

	@Override
	public boolean hasAnnotation(Class<? extends Annotation> class1) {
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			if (c.isAnnotationPresent(class1)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAssignableFrom(TypeWrapper typeWrapper) {
		if (typeWrapper == null) {
			// assignable from a null value
			return true;
		}
		return ClassUtils.isAssignableFromType(clazz, typeWrapper.getType());
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public TypeWrapper getComponentType() {
		prepareFieldsMethodsAndTypes();
		return TypeWrappers.wrap(clazz.getComponentType());
	}

	@Override
	public TypeWrapper getSuperClass() {
		return TypeWrappers.wrap(clazz.getGenericSuperclass());
	}

}
