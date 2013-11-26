/*******************************************************************************
 * Copyright (c) 2009, 2013 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Evgeny Mandrikov - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Creates a code coverage report for tests of a single project in multiple formats
 * (HTML, XML, and CSV).
 *
 * @phase verify
 * @goal report
 * @requiresProject true
 * @threadSafe
 * @since 0.5.3
 */
public class ReportMojo extends AbstractReportMojo {

	/**
	 * Output directory for the reports. Note that this parameter is only
	 * relevant if the goal is run from the command line or from the default
	 * build lifecycle. If the goal is run indirectly as part of a site
	 * generation, the output directory configured in the Maven Site Plugin is
	 * used instead.
	 *
	 * @parameter default-value="${project.reporting.outputDirectory}/jacoco"
	 */
	private File outputDirectory;

	/**
	 * File with execution data.
	 *
	 * @parameter default-value="${project.build.directory}/jacoco.exec"
	 */
	private File dataFile;

	/**
	 * A pattern matching directories with compiled classes.
	 *
	 * @parameter
	 */
	private String classDirectoryPattern;

	@Override
	protected String getOutputDirectory() {
		return outputDirectory.getAbsolutePath();
	}

	@Override
	protected BundleCreator createBundleCreator(final FileFilter fileFilter)
	{
		if(this.classDirectoryPattern == null)
		{
			return super.createBundleCreator(fileFilter);
		}
		else
		{
			DirectoryScanner scanner = new DirectoryScanner();

			scanner.setBasedir(project.getBasedir());
			scanner.setIncludes(new String[] { this.classDirectoryPattern });

			scanner.scan();

			String[] includeds = scanner.getIncludedDirectories();
			List<File> files = new ArrayList<File>();
			getLog().error("TOTAL PATHS FOUND::: " + includeds.length);
			for(String included: includeds)
			{
				getLog().error("INCLUDED PATH::: " + included);
				files.add(new File(included));
			}
			return new BundleCreator(project, fileFilter, files);
		}
	}

	@Override
	public void setReportOutputDirectory(final File reportOutputDirectory) {
		if (reportOutputDirectory != null
				&& !reportOutputDirectory.getAbsolutePath().endsWith("jacoco")) {
			outputDirectory = new File(reportOutputDirectory, "jacoco");
		} else {
			outputDirectory = reportOutputDirectory;
		}
	}

	@Override
	protected File getDataFile() {
		return dataFile;
	}

	@Override
	protected File getOutputDirectoryFile() {
		return outputDirectory;
	}

	public String getOutputName() {
		return "jacoco/index";
	}

	public String getName(final Locale locale) {
		return "JaCoCo Test";
	}
}
