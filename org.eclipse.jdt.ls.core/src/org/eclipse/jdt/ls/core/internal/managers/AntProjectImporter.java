/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.managers;

import org.apache.tools.ant.taskdefs.Javac;

import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.ls.core.internal.AbstractProjectImporter;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.managers.BasicFileDetector;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.ant.internal.ui.datatransfer.*;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntTaskNode;

/**
 * @author Poytr1
 *
 */
public class AntProjectImporter extends AbstractProjectImporter {

	public static final String IMPORTING_ANT_PROJECTS = "Importing Ant project(s)";

	public static final String ANT_FILE = "build.xml";

	private Collection<Path> directories;

	@Override
	public boolean applies(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (rootFolder == null) {
			return false;
		}
		PreferenceManager preferencesManager = JavaLanguageServerPlugin.getPreferencesManager();
		if (preferencesManager != null && !preferencesManager.getPreferences().isImportMavenEnabled()) {
			return false;
		}
		if (directories == null) {
			BasicFileDetector antDetector = new BasicFileDetector(rootFolder.toPath(), ANT_FILE).includeNested(false);
			directories = antDetector.scan(monitor);
		}
		return !directories.isEmpty();
	}

	@Override
	public void importToWorkspace(IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (!applies(monitor)) {
			return;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, 105);
		subMonitor.setTaskName(IMPORTING_ANT_PROJECTS);
		JavaLanguageServerPlugin.logInfo(IMPORTING_ANT_PROJECTS);

		ProjectCreator pc = new ProjectCreator();
		for (Path project: directories) {
			AntProjectNode antProjectNode = ANTUtils.getProjectNode(project.toString() + '/' + ANT_FILE);
			if (antProjectNode != null) {
				List<AntTaskNode> javacNodes = new ArrayList<>();
				ANTUtils.getJavacNodes(javacNodes, antProjectNode);
				List<?> javacTasks = ANTUtils.resolveJavacTasks(javacNodes);
				Iterator<?> iter = javacTasks.iterator();
				try {
				while (iter.hasNext()) {
					Javac javacTask = (Javac) iter.next();
					pc.createJavaProjectFromJavacNode(project.getParent().getFileName().toString(), javacTask, monitor);
				}
				} catch (Exception e) {
					//
				}
			}

		}
		subMonitor.done();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

}
