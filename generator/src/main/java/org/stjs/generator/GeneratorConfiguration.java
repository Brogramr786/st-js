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
package org.stjs.generator;

import java.util.Collection;
import java.util.Set;

public class GeneratorConfiguration {
	private final Collection<String> allowedPackages;
	private final Set<String> allowedJavaLangClasses;
	private final boolean generateMainMethodCall;

	GeneratorConfiguration(Collection<String> allowedPackages, Set<String> allowedJavaLangClasses,
			boolean generateMainMethodCall) {
		this.allowedPackages = allowedPackages;
		this.allowedJavaLangClasses = allowedJavaLangClasses;
		this.generateMainMethodCall = generateMainMethodCall;
	}

	/**
	 * 
	 * @return the parent packages that contain the classes that can be called from the processed source file. Note that
	 *         sub-packages of a package from this collection are also allowed. java.lang is implicit
	 */
	public Collection<String> getAllowedPackages() {
		return allowedPackages;
	}

	public Set<String> getAllowedJavaLangClasses() {
		return allowedJavaLangClasses;
	}

	/**
	 * 
	 * @return true if the call to the main method should be generated. During unit tests this should not be done.
	 */
	public boolean isGenerateMainMethodCall() {
		return generateMainMethodCall;
	}

}
