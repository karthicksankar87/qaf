/*******************************************************************************
 * QMetry Automation Framework provides a powerful and versatile platform to
 * author
 * Automated Test Cases in Behavior Driven, Keyword Driven or Code Driven
 * approach
 * Copyright 2016 Infostretch Corporation
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 * You should have received a copy of the GNU General Public License along with
 * this program in the name of LICENSE.txt in the root folder of the
 * distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
 * See the NOTICE.TXT file in root folder of this source files distribution
 * for additional information regarding copyright ownership and licenses
 * of other open source software / files used by QMetry Automation Framework.
 * For any inquiry or need additional information, please contact
 * support-qaf@infostretch.com
 *******************************************************************************/

package com.qmetry.qaf.automation.step;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;
import static com.qmetry.qaf.automation.keys.ApplicationProperties.STEP_PROVIDER_PKG;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.qmetry.qaf.automation.ui.webdriver.QAFWebComponent;
import com.qmetry.qaf.automation.util.ClassUtil;

/**
 * com.qmetry.qaf.automation.step.JavaStepFinder.java
 * 
 * @author chirag.jayswal
 */
public final class JavaStepFinder {
	public static final String STEPS_PACKAGE = "com.qmetry.qaf.automation.step";
	private static final Log logger = LogFactoryImpl.getLog(JavaStepFinder.class);

	public static Map<String, TestStep> getAllJavaSteps() {
		Map<String, TestStep> stepMapping = new HashMap<String, TestStep>();
		Set<Class<?>> stepProviders = new LinkedHashSet<Class<?>>();;
		Set<Method> steps = new LinkedHashSet<Method>();

		List<String> pkgs = new ArrayList<String>();
		pkgs.add(STEPS_PACKAGE);

		if (getBundle().containsKey(STEP_PROVIDER_PKG.key)) {
			pkgs.addAll(Arrays.asList(getBundle().getStringArray(STEP_PROVIDER_PKG.key)));
		}
		for (String pkg : pkgs) {
			logger.info("pkg: " + pkg);

			ConfigurationBuilder configuration =
					new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(pkg))
							.setScanners(new TypeAnnotationsScanner(),
									new org.reflections.scanners.MethodAnnotationsScanner(),
									new SubTypesScanner(false))
							.filterInputsBy(new FilterBuilder().includePackage(pkg));
			Reflections reflections = new Reflections(configuration);

			try {
				Set<Class<? extends Object>> classes =
						reflections.getSubTypesOf(Object.class);
				steps.addAll(getAllMethodsWithAnnotation(classes, QAFTestStep.class));
			} catch (Exception e) {
				logger.error("Unable to scaning step through classes from package: " + pkg
						+ "\n Using direct step annotation scanning instead", e);
				steps.addAll(reflections.getMethodsAnnotatedWith(QAFTestStep.class));
			}
			stepProviders
					.addAll(reflections.getTypesAnnotatedWith(QAFTestStepProvider.class));
		}
		for (Class<?> stepProvider : stepProviders) {
			if (QAFWebComponent.class.isAssignableFrom(stepProvider)) {

			} else if (stepProvider.isInterface())
				continue;
			else {
				steps.addAll(Arrays.asList(stepProvider.getMethods()));
			}
		}

		for (Method step : steps) {
			if (!Modifier.isPrivate(step.getModifiers())) {
				// exclude private methods.
				// Case: step provided using QAFTestStepProvider at class level
				add(stepMapping, new JavaStep(step));
			}
		}

		return stepMapping;

	}

	private static void add(Map<String, TestStep> stepMapping, TestStep step) {
		TestStep oldStep = stepMapping.put(step.getName().toUpperCase(), step);

		if (oldStep != null) {

			// ensure the priority specified while providing step provider
			// package. If list of packages provided, last package has highest
			// priority.
			String[] pkgs = getBundle().getStringArray(STEP_PROVIDER_PKG.key);
			int oldStepPriority = getStepPriority(oldStep, pkgs);
			int curStepPriority = getStepPriority(step, pkgs);

			logger.debug(String.format(
					"Found duplicate step to load [%s] with [%s] prority then [%s]",
					oldStep.getSignature(),
					(oldStepPriority > curStepPriority ? "higher" : "lower"),
					step.getSignature()));

			if (oldStepPriority > curStepPriority) {
				step = oldStep;
				oldStep = stepMapping.put(step.getName().toUpperCase(), oldStep);
			}

		}

	}

	private static int getStepPriority(TestStep step, String[] pkgs) {
		String stepPackage = step.getFileName().replaceAll("/", ".");
		int i = 0;
		for (; i < pkgs.length; i++) {
			if (stepPackage.startsWith(pkgs[i]))
				return i;
		}
		return i;
	}

	private static Set<Method> getAllMethodsWithAnnotation(Set<Class<?>> classes,
			Class<? extends Annotation> annotation) {

		Set<Method> methods = new HashSet<Method>();
		for (Class<?> cls : classes) {
			if (cls.isInterface())
				continue;

			for (Method method : cls.getMethods()) {
				if (ClassUtil.hasAnnotation(method, annotation)) {
					methods.add(method);
				}
			}
		}

		return methods;
	}
}
