/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * JavaBeans的静态方法工具类，用于实例化bean，
 * 检查bean属性属性，复制bean属性
 *
 * Static convenience methods for JavaBeans: for instantiating beans,
 * checking bean property types, copying bean properties, etc.
 *
 * 主要在框架内使用，但是在某种程度上对应用程序也非常实用（来自Spring的暗示）。
 * <p>Mainly for use within the framework, but to some degree also
 * useful for application classes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 */
public abstract class BeanUtils {

	private static final Log logger = LogFactory.getLog(BeanUtils.class);

	private static final Set<Class<?>> unknownEditorTypes =
			Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

	/**
	 * 存储一些类型的默认值，如boolean、byte等
	 */
	private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

	static {
		Map<Class<?>, Object> values = new HashMap<>();
		values.put(boolean.class, false);
		values.put(byte.class, (byte) 0);
		values.put(short.class, (short) 0);
		values.put(int.class, 0);
		values.put(long.class, (long) 0);
		// 创建一个不能修改的map，赋值给DEFAULT_TYPE_VALUES
		DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
	}


	/**
	 * 方便的方法去实例化一个bean通过它的无参构造方法
	 * Convenience method to instantiate a class using its no-arg constructor.
	 * @param clazz class to instantiate 要实例化的类
	 * @return the new instance 创建的实例
	 * @throws BeanInstantiationException if the bean cannot be instantiated 如果bean不能被初始化则抛出异常
	 * @deprecated as of Spring 5.0, following the deprecation of spring5.0之后被废弃
	 * {@link Class#newInstance()} in JDK 9
	 * @see Class#newInstance()
	 */
	@Deprecated
	public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
		// clazz不能为空，如果为null抛出异常
		Assert.notNull(clazz, "Class must not be null");
		// 如果是一个接口则抛出异常
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			// 调用class方法的newInstance()方法，通过无参构造方法创建对象
			return clazz.newInstance();
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
		}
	}

	/**
	 * Instantiate a class using its 'primary' constructor (for Kotlin classes,
	 * potentially having default arguments declared) or its default constructor
	 * (for regular Java classes, expecting a standard no-arg setup).
	 * <p>Note that this method tries to set the constructor accessible
	 * if given a non-accessible (that is, non-public) constructor.
	 *
	 * 使用类的'主要'构造函数（对于Kotlin类，
	 * 可能声明了默认参数）或其默认构造函数
	 * （常规的java类，需要设置一个标准的无参构造函数）
	 * 注意，如果给定一个不可访问的（即非public）的构造函数，此方法尝试去设置构造函数是可访问的
	 * @param clazz the class to instantiate 要实例化的class
	 * @return the new instance 实例化的bean
	 * @throws BeanInstantiationException if the bean cannot be instantiated. 如果不能时候bean则抛出
	 *
	 * 如果没有一个主构造函数或者默认构造函数，会抛出异常
	 * The cause may notably indicate a {@link NoSuchMethodException} if no
	 * primary/default constructor was found, a {@link NoClassDefFoundError}
	 * or other {@link LinkageError} in case of an unresolvable class definition
	 * (e.g. due to a missing dependency at runtime), or an exception thrown
	 * from the constructor invocation itself.
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface()) {
			throw new BeanInstantiationException(clazz, "Specified class is an interface");
		}
		try {
			// 获取所有无参构造函数（因为没有指定类型）去实例化对象
			// getDeclaredConstructor(): 获取所有构造函数包含private
			// getConstructor()：获取所有public的构造函数
			return instantiateClass(clazz.getDeclaredConstructor());
		}
		catch (NoSuchMethodException ex) {
			// Kotlin的话去获取primary构造函数，同样调用方法实例化对象
			Constructor<T> ctor = findPrimaryConstructor(clazz);
			if (ctor != null) {
				return instantiateClass(ctor);
			}
			throw new BeanInstantiationException(clazz, "No default constructor found", ex);
		}
		catch (LinkageError err) {
			throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
		}
	}

	/**
	 * 实例化一个class类的对象通过无参构造方法，并且返回一个实例需要是assignableTo类型的子类
	 * Instantiate a class using its no-arg constructor and return the new instance
	 * as the specified assignable type.
	 * <p>Useful in cases where the type of the class to instantiate (clazz) is not
	 * available, but the type desired (assignableTo) is known.
	 * <p>Note that this method tries to set the constructor accessible if given a
	 * non-accessible (that is, non-public) constructor.
	 * @param clazz class to instantiate
	 * @param assignableTo type that clazz must be assignableTo
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated
	 * @see Constructor#newInstance
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
		Assert.isAssignable(assignableTo, clazz);
		return (T) instantiateClass(clazz);
	}

	/**
	 * 使用给定的方法去实例化一个类的对象
	 * 注意，这个方法尝试将给定的构造函数设置成可访问的
	 * 不可访问(即非公共)构造函数，并支持Kotlin类
	 * 可选参数和默认值。
	 *
	 * Convenience method to instantiate a class using the given constructor.
	 * <p>Note that this method tries to set the constructor accessible if given a
	 * non-accessible (that is, non-public) constructor, and supports Kotlin classes
	 * with optional parameters and default values.
	 * @param ctor the constructor to instantiate 实例化的构造函数
	 * @param args the constructor arguments to apply (use {@code null} for an unspecified 构造函数参数
	 * parameter, Kotlin optional parameters and Java primitive types are supported)
	 * @return the new instance
	 * @throws BeanInstantiationException if the bean cannot be instantiated 如果bean不能被实例化则抛出
	 * @see Constructor#newInstance
	 */
	public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
		Assert.notNull(ctor, "Constructor must not be null");
		try {
			// 设置可访问
			ReflectionUtils.makeAccessible(ctor);
			// 如果是Kotlin的类，按照Kotlin处理
			if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
				return KotlinDelegate.instantiateClass(ctor, args);
			}
			else {
				// 获取构造函数的参数类型
				Class<?>[] parameterTypes = ctor.getParameterTypes();
				// 判断提提供的参数和构造函数本身的参数是否相同
				Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
				// 创建参数数组，根据参数长度
				Object[] argsWithDefaultValues = new Object[args.length];
				for (int i = 0 ; i < args.length; i++) {
					if (args[i] == null) {
						// 如果为空，并且是基本类型，则按照默认值赋值
						Class<?> parameterType = parameterTypes[i];
						argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
					}
					else {
						// 如果不为空直接赋值
						argsWithDefaultValues[i] = args[i];
					}
				}
				// 使用构造函数创建对象
				return ctor.newInstance(argsWithDefaultValues);
			}
		}
		catch (InstantiationException ex) {
			throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
		}
		catch (IllegalAccessException ex) {
			throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
		}
		catch (IllegalArgumentException ex) {
			throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
		}
		catch (InvocationTargetException ex) {
			throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
		}
	}

	/**
	 * 针对Kotlin才有意义，普通的则返回null
	 *
	 * Return the primary constructor of the provided class. For Kotlin classes, this
	 * returns the Java constructor corresponding to the Kotlin primary constructor
	 * (as defined in the Kotlin specification). Otherwise, in particular for non-Kotlin
	 * classes, this simply returns {@code null}.
	 * @param clazz the class to check
	 * @since 5.0
	 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">Kotlin docs</a>
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
			Constructor<T> kotlinPrimaryConstructor = KotlinDelegate.findPrimaryConstructor(clazz);
			if (kotlinPrimaryConstructor != null) {
				return kotlinPrimaryConstructor;
			}
		}
		return null;
	}

	/**
	 * 给定方法名和参数类型，查找method，
	 * 在给定类或者其父类，会优先返回public方法，
	 * 也还会返回protected、包级别、或者私有方法
	 *
	 * 即使在Java安全设置受限的环境中也不会出现问题。
	 *
	 * Find a method with the given method name and the given parameter types,
	 * declared on the given class or one of its superclasses. Prefers public methods,
	 * but will return a protected, package access, or private method too.
	 * <p>Checks {@code Class.getMethod} first, falling back to
	 * {@code findDeclaredMethod}. This allows to find public methods
	 * without issues even in environments with restricted Java security settings.
	 * @param clazz the class to check class对象
	 * @param methodName the name of the method to find 方法名
	 * @param paramTypes the parameter types of the method to find 参数类型
	 * @return the Method object, or {@code null} if not found 返回发现的方法或者没有找到
	 * @see Class#getMethod
	 * @see #findDeclaredMethod
	 */
	@Nullable
	public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			// 根据方法名和参数类型查找public方法
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			// 根据方法名和参数类型查找所有方法包含private
			return findDeclaredMethod(clazz, methodName, paramTypes);
		}
	}

	/**
	 * 查找一个方法通过给定的方法名和参数类型
	 *
	 * 返回给定class或者父类的public,
	 * protected, package 级别的, private 方法.
	 * 查找方法会向上递归查找到所有父类
	 *
	 * Find a method with the given method name and the given parameter types,
	 * declared on the given class or one of its superclasses. Will return a public,
	 * protected, package access, or private method.
	 * <p>Checks {@code Class.getDeclaredMethod}, cascading upwards to all superclasses.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @param paramTypes the parameter types of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @see Class#getDeclaredMethod
	 */
	@Nullable
	public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		try {
			return clazz.getDeclaredMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			if (clazz.getSuperclass() != null) {
				return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
			}
			return null;
		}
	}

	/**
	 * 使用给定的方法名和最小参数找到一个方法
	 * 定义在给定类或者它的超类上，优先查找公有方法，
	 * 但是也会返回protected、包级别的、private级别的方法
	 *
	 * Find a method with the given method name and minimal parameters (best case: none),
	 * declared on the given class or one of its superclasses. Prefers public methods,
	 * but will return a protected, package access, or private method too.
	 * <p>Checks {@code Class.getMethods} first, falling back to
	 * {@code findDeclaredMethodWithMinimalParameters}. This allows for finding public
	 * methods without issues even in environments with restricted Java security settings.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 * @see Class#getMethods
	 * @see #findDeclaredMethodWithMinimalParameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {
		// 根据方法数组和方法名查询
		Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
		// 如果当前类的公有方法没有找到，则查找私有方法和父类的方法
		if (targetMethod == null) {
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
		}
		return targetMethod;
	}

	/**
	 * 在类中查找最小参数的方法，找不到再父类上面找
	 * Find a method with the given method name and minimal parameters (best case: none),
	 * declared on the given class or one of its superclasses. Will return a public,
	 * protected, package access, or private method.
	 * <p>Checks {@code Class.getDeclaredMethods}, cascading upwards to all superclasses.
	 * @param clazz the class to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 * @see Class#getDeclaredMethods
	 */
	@Nullable
	public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
			throws IllegalArgumentException {
		// 获取当前类的所有方法
		Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
		if (targetMethod == null && clazz.getSuperclass() != null) {
			// 递归调用，获取父类的class
			targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
		}
		return targetMethod;
	}

	/**
	 * 获取给定的方法数组里面查找对应方法名称，最小参数的方法
	 *
	 * Find a method with the given method name and minimal parameters (best case: none)
	 * in the given list of methods.
	 * @param methods the methods to check
	 * @param methodName the name of the method to find
	 * @return the Method object, or {@code null} if not found
	 * @throws IllegalArgumentException if methods of the given name were found but
	 * could not be resolved to a unique method with minimal parameters
	 */
	@Nullable
	public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
			throws IllegalArgumentException {

		Method targetMethod = null;
		// 符合要求的方法个数
		int numMethodsFoundWithCurrentMinimumArgs = 0;
		for (Method method : methods) {
			// 如果方法名相等
			if (method.getName().equals(methodName)) {
				// 获取方法参数
				int numParams = method.getParameterCount();
				// 如果目标方法为空 或者参数数量小于当前目标方法的参数数量就重新赋值
				if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
					targetMethod = method;
					numMethodsFoundWithCurrentMinimumArgs = 1;
				}
				// 不是桥接方法（为了擦除泛型自动生成的）且参数数量等于目标方法的参数个数相等
				else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
					if (targetMethod.isBridge()) {
						// 优先常规方法覆盖桥接方法
						// Prefer regular method over bridge...
						targetMethod = method;
					}
					else {
						// Additional candidate with same length
						// 如果长度相同则自增统计值
						numMethodsFoundWithCurrentMinimumArgs++;
					}
				}
			}
		}
		// 如果大于1个，则抛出异常，说明有两个最小参数长度方法不止一个
		if (numMethodsFoundWithCurrentMinimumArgs > 1) {
			throw new IllegalArgumentException("Cannot resolve method '" + methodName +
					"' to a unique method. Attempted to resolve to overloaded method with " +
					"the least number of parameters but there were " +
					numMethodsFoundWithCurrentMinimumArgs + " candidates.");
		}
		return targetMethod;
	}

	/**
	 * 解析方法签名，从方法名和参数列表，
	 * 参数列表是可选的，逗号分隔的全限定列表
	 * 只有名称和参数类型匹配的方法才返回
	 * 如果找不到方法则返回null
	 *
	 * Parse a method signature in the form {@code methodName[([arg_list])]},
	 * where {@code arg_list} is an optional, comma-separated list of fully-qualified
	 * type names, and attempts to resolve that signature against the supplied {@code Class}.
	 * <p>When not supplying an argument list ({@code methodName}) the method whose name
	 * matches and has the least number of parameters will be returned. When supplying an
	 * argument type list, only the method whose name and argument types match will be returned.
	 * <p>Note then that {@code methodName} and {@code methodName()} are <strong>not</strong>
	 * resolved in the same way. The signature {@code methodName} means the method called
	 * {@code methodName} with the least number of arguments, whereas {@code methodName()}
	 * means the method called {@code methodName} with exactly 0 arguments.
	 * <p>If no method can be found, then {@code null} is returned.
	 * @param signature the method signature as String representation
	 * @param clazz the class to resolve the method signature against
	 * @return the resolved Method
	 * @see #findMethod
	 * @see #findMethodWithMinimalParameters
	 */
	@Nullable
	public static Method resolveSignature(String signature, Class<?> clazz) {
		Assert.hasText(signature, "'signature' must not be empty");
		Assert.notNull(clazz, "Class must not be null");
		int startParen = signature.indexOf('(');
		int endParen = signature.indexOf(')');
		// 判断能否找到括号，开始括号存在，结束括号不存在
		if (startParen > -1 && endParen == -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected closing ')' for args list");
		}
		// 开始括号不存在，结束括号存在
		else if (startParen == -1 && endParen > -1) {
			throw new IllegalArgumentException("Invalid method signature '" + signature +
					"': expected opening '(' for args list");
		}
		// 开始括号不存在，说明没有传递参数列表，直接寻找最短参数的方法，直接将方法签名当做方法名传入
		else if (startParen == -1) {
			return findMethodWithMinimalParameters(clazz, signature);
		}
		else {
			// 走到这里说明传入了参数，并且说明括号匹配正确
			String methodName = signature.substring(0, startParen);
			// 截取括号开始的后一个位置到结束括号但是不包含结束括号，将参数取出来
			// 按照逗号进行拆分,拆分出来的应该都是入：java.lang.String这样的全限定名
			String[] parameterTypeNames =
					StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
			// 按照参数个数创建数组
			Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
			for (int i = 0; i < parameterTypeNames.length; i++) {
				String parameterTypeName = parameterTypeNames[i].trim();
				try {
					// 转成对应class对象
					parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
				}
				catch (Throwable ex) {
					throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
							parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
				}
			}
			// 真正干活的方法根据class类型， 方法名，参数类型列表，获取对应方法
			return findMethod(clazz, methodName, parameterTypes);
		}
	}


	/**
	 * 检索给定class对象的JavaBeans的PropertyDescriptor。
	 *  PropertyDescriptor描述Java Bean的一个属性通过一对访问器方法导出。
	 *
	 * Retrieve the JavaBeans {@code PropertyDescriptor}s of a given class.
	 * @param clazz the Class to retrieve the PropertyDescriptors for
	 * @return an array of {@code PropertyDescriptors} for the given class
	 * @throws BeansException if PropertyDescriptor look fails
	 */
	public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getPropertyDescriptors();
	}

	/**
	 * 检索给定属性的JavaBeans {@code PropertyDescriptors}。
	 *
	 * Retrieve the JavaBeans {@code PropertyDescriptors} for the given property.
	 * @param clazz the Class to retrieve the PropertyDescriptor for
	 * @param propertyName the name of the property
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 */
	@Nullable
	public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName)
			throws BeansException {

		CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
		return cr.getPropertyDescriptor(propertyName);
	}

	/**
	 * 为给定的方法找到一个JavaBeans {@code PropertyDescriptor}，
	 * 方法可以是读方法，也可以是写方法
	 * bean属性。
	 *
	 * Find a JavaBeans {@code PropertyDescriptor} for the given method,
	 * with the method either being the read method or the write method for
	 * that bean property.
	 *
	 * @param method the method to find a corresponding PropertyDescriptor for,
	 * introspecting its declaring class
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
		return findPropertyForMethod(method, method.getDeclaringClass());
	}

	/**
	 * 为给定的方法找到一个JavaBeans {@code PropertyDescriptor}，
	 * 方法可以是读方法，也可以是写方法
	 * bean属性。
	 * Find a JavaBeans {@code PropertyDescriptor} for the given method,
	 * with the method either being the read method or the write method for
	 * that bean property.
	 * @param method the method to find a corresponding PropertyDescriptor for
	 * @param clazz the (most specific) class to introspect for descriptors
	 * @return the corresponding PropertyDescriptor, or {@code null} if none
	 * @throws BeansException if PropertyDescriptor lookup fails
	 * @since 3.2.13
	 */
	@Nullable
	public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
		Assert.notNull(method, "Method must not be null");
		PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
		for (PropertyDescriptor pd : pds) {
			if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
				return pd;
			}
		}
		return null;
	}

	/**
	 * 找到一个遵循'Editor'后缀约定的JavaBeans PropertyEditor
	 *
	 * Find a JavaBeans PropertyEditor following the 'Editor' suffix convention
	 * (e.g. "mypackage.MyDomainClass" -> "mypackage.MyDomainClassEditor").
	 * <p>Compatible to the standard JavaBeans convention as implemented by
	 * {@link java.beans.PropertyEditorManager} but isolated from the latter's
	 * registered default editors for primitive types.
	 * @param targetType the type to find an editor for
	 * @return the corresponding editor, or {@code null} if none found
	 */
	@Nullable
	public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
		if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
			return null;
		}
		ClassLoader cl = targetType.getClassLoader();
		if (cl == null) {
			try {
				cl = ClassLoader.getSystemClassLoader();
				if (cl == null) {
					return null;
				}
			}
			catch (Throwable ex) {
				// e.g. AccessControlException on Google App Engine
				if (logger.isDebugEnabled()) {
					logger.debug("Could not access system ClassLoader: " + ex);
				}
				return null;
			}
		}
		String editorName = targetType.getName() + "Editor";
		try {
			Class<?> editorClass = cl.loadClass(editorName);
			if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
				if (logger.isInfoEnabled()) {
					logger.info("Editor class [" + editorName +
							"] does not implement [java.beans.PropertyEditor] interface");
				}
				unknownEditorTypes.add(targetType);
				return null;
			}
			return (PropertyEditor) instantiateClass(editorClass);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isTraceEnabled()) {
				logger.trace("No property editor [" + editorName + "] found for type " +
						targetType.getName() + " according to 'Editor' suffix convention");
			}
			unknownEditorTypes.add(targetType);
			return null;
		}
	}

	/**
	 * 如果可能的话，从给定的类/接口确定给定属性的bean属性类型。
	 *
	 * Determine the bean property type for the given property from the
	 * given classes/interfaces, if possible.
	 * @param propertyName the name of the bean property
	 * @param beanClasses the classes to check against
	 * @return the property type, or {@code Object.class} as fallback
	 */
	public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
		if (beanClasses != null) {
			for (Class<?> beanClass : beanClasses) {
				PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
				if (pd != null) {
					return pd.getPropertyType();
				}
			}
		}
		return Object.class;
	}

	/**
	 * 为指定属性的写入方法获取一个新的MethodParameter对象。
	 *
	 * Obtain a new MethodParameter object for the write method of the
	 * specified property.
	 * @param pd the PropertyDescriptor for the property
	 * @return a corresponding MethodParameter object
	 */
	public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
		if (pd instanceof GenericTypeAwarePropertyDescriptor) {
			return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
		}
		else {
			Method writeMethod = pd.getWriteMethod();
			Assert.state(writeMethod != null, "No write method available");
			return new MethodParameter(writeMethod, 0);
		}
	}

	/**
	 *
	 * 检查给定的类型是否表示“简单”属性:简单值类型或简单值类型数组。
	 *
	 * Check if the given type represents a "simple" property: a simple value
	 * type or an array of simple value types.
	 * <p>See {@link #isSimpleValueType(Class)} for the definition of <em>simple
	 * value type</em>.
	 * <p>Used to determine properties to check for a "simple" dependency-check.
	 * @param type the type to check
	 * @return whether the given type represents a "simple" property
	 * @see org.springframework.beans.factory.support.RootBeanDefinition#DEPENDENCY_CHECK_SIMPLE
	 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#checkDependencies
	 * @see #isSimpleValueType(Class)
	 */
	public static boolean isSimpleProperty(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
	}

	/**
	 * 检查给定类型是否表示“简单”值类型:基本类型或
	 * 基本类型包装类型，枚举，字符串或其他字符序列，数字，一个
	 * Date、时态、URI、URL、Locale或类。
	 *
	 * Check if the given type represents a "simple" value type: a primitive or
	 * primitive wrapper, an enum, a String or other CharSequence, a Number, a
	 * Date, a Temporal, a URI, a URL, a Locale, or a Class.
	 * <p>{@code Void} and {@code void} are not considered simple value types.
	 * @param type the type to check
	 * @return whether the given type represents a "simple" value type
	 * @see #isSimpleProperty(Class)
	 */
	public static boolean isSimpleValueType(Class<?> type) {
		return (Void.class != type && void.class != type &&
				(ClassUtils.isPrimitiveOrWrapper(type) ||
				Enum.class.isAssignableFrom(type) ||
				CharSequence.class.isAssignableFrom(type) ||
				Number.class.isAssignableFrom(type) ||
				Date.class.isAssignableFrom(type) ||
				Temporal.class.isAssignableFrom(type) ||
				URI.class == type ||
				URL.class == type ||
				Locale.class == type ||
				Class.class == type));
	}


	/**
	 * 将给定源bean的属性复制到目标bean中
	 * 注意：源类和目标类不必匹配，甚至不用存在继承关系
	 * 只要属性匹配的任何bean属性都能被复制
	 * 源bean暴露的属性而目标bean未暴露将被忽略。
	 * 这只是一个方便的方法。对于更复杂的转移需求考虑使用完整的BeanWrapper。
	 *
	 * Copy the property values of the given source bean into the target bean.
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a full BeanWrapper.
	 * @param source the source bean
	 * @param target the target bean
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target) throws BeansException {
		copyProperties(source, target, null, (String[]) null);
	}

	/**
	 * Copy the property values of the given source bean into the given target bean,
	 * only setting properties defined in the given "editable" class (or interface).
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a full BeanWrapper.
	 * @param source the source bean
	 * @param target the target bean
	 *
	 * @param editable 要将属性设置限制到的类(或接口) the class (or interface) to restrict property setting to
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
		copyProperties(source, target, editable, (String[]) null);
	}

	/**
	 * Copy the property values of the given source bean into the given target bean,
	 * ignoring the given "ignoreProperties".
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * <p>This is just a convenience method. For more complex transfer needs,
	 * consider using a full BeanWrapper.
	 * @param source the source bean
	 * @param target the target bean
	 * @param ignoreProperties array of property names to ignore
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
		copyProperties(source, target, null, ignoreProperties);
	}

	/**
	 * Copy the property values of the given source bean into the given target bean.
	 * <p>Note: The source and target classes do not have to match or even be derived
	 * from each other, as long as the properties match. Any bean properties that the
	 * source bean exposes but the target bean does not will silently be ignored.
	 * @param source the source bean
	 * @param target the target bean
	 * @param editable the class (or interface) to restrict property setting to
	 * @param ignoreProperties array of property names to ignore 忽略的属性名称数组
	 * @throws BeansException if the copying failed
	 * @see BeanWrapper
	 */
	private static void copyProperties(Object source, Object target, @Nullable Class<?> editable,
			@Nullable String... ignoreProperties) throws BeansException {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(target, "Target must not be null");

		// 目标类的class
		Class<?> actualEditable = target.getClass();
		// 判断限制是否为空
		if (editable != null) {
			// 判断目标类是否为限制类的对象
			if (!editable.isInstance(target)) {
				throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
						"] not assignable to Editable class [" + editable.getName() + "]");
			}
			// 赋值给目标类的class
			actualEditable = editable;
		}
		// 获取属性描述符
		PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
		// 忽略的字段
		List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

		// 循环属性
		for (PropertyDescriptor targetPd : targetPds) {
			// 获取写方法
			Method writeMethod = targetPd.getWriteMethod();
			// 写方法不为空 且忽略的字段列表不包含该字段
			if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
				// 获取目标类的属性
				PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
				if (sourcePd != null) {
					// 获取源类的读方法
					Method readMethod = sourcePd.getReadMethod();
					// 读方法不为空判断源类的读方法的返回值和写方法的参数类型是否匹配
					if (readMethod != null &&
							ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
						try {
							// 判断方法是不是public的，如果不是，则设置accessible为true
							if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
								readMethod.setAccessible(true);
							}

							// 调用源类的invoke方法，获取对应值
							Object value = readMethod.invoke(source);
							// 判断方法是不是public的，如果不是，则设置accessible为true
							if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
								writeMethod.setAccessible(true);
							}
							// 调用目标类的写方法，将对应值设置进去
							writeMethod.invoke(target, value);
						}
						catch (Throwable ex) {
							throw new FatalBeanException(
									"Could not copy property '" + targetPd.getName() + "' from source to target", ex);
						}
					}
				}
			}
		}
	}


	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 * 内部类来避免运行时对Kotlin的强依赖
	 */
	private static class KotlinDelegate {

		/**
		 * Retrieve the Java constructor corresponding to the Kotlin primary constructor, if any.
		 * @param clazz the {@link Class} of the Kotlin class
		 * @see <a href="https://kotlinlang.org/docs/reference/classes.html#constructors">
		 * https://kotlinlang.org/docs/reference/classes.html#constructors</a>
		 */
		@Nullable
		public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
			try {
				KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
				if (primaryCtor == null) {
					return null;
				}
				Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
				if (constructor == null) {
					throw new IllegalStateException(
							"Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
				}
				return constructor;
			}
			catch (UnsupportedOperationException ex) {
				return null;
			}
		}

		/**
		 * Instantiate a Kotlin class using the provided constructor.
		 * @param ctor the constructor of the Kotlin class to instantiate
		 * @param args the constructor arguments to apply
		 * (use {@code null} for unspecified parameter if needed)
		 */
		public static <T> T instantiateClass(Constructor<T> ctor, Object... args)
				throws IllegalAccessException, InvocationTargetException, InstantiationException {

			KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
			if (kotlinConstructor == null) {
				return ctor.newInstance(args);
			}
			List<KParameter> parameters = kotlinConstructor.getParameters();
			Map<KParameter, Object> argParameters = new HashMap<>(parameters.size());
			Assert.isTrue(args.length <= parameters.size(),
					"Number of provided arguments should be less of equals than number of constructor parameters");
			for (int i = 0 ; i < args.length ; i++) {
				if (!(parameters.get(i).isOptional() && args[i] == null)) {
					argParameters.put(parameters.get(i), args[i]);
				}
			}
			return kotlinConstructor.callBy(argParameters);
		}

	}

}
