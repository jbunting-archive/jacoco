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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Predicate;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * Creates a code coverage report for tests of a single project in multiple formats
 * (HTML, XML, and CSV).
 *
 * @phase verify
 * @goal aggregate
 * @requiresProject true
 * @threadSafe
 * @aggregator
 * @execute phase="verify"
 * @since 0.5.3
 */
public class AggregateReportMojo extends AbstractReportMojo
{

	private static final String MSG_SKIPPING = "Skipping JaCoCo merge execution due to missing execution data files";

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
	 * The projects in the reactor for aggregation report.
	 *
	 * @parameter expression="${reactorProjects}"
	 * @readonly
	 */
	protected List reactorProjects;

	/**
	 * @parameter
	 */
	private List<String> skippedModules = new ArrayList<String>();

	@Override
	protected BundleCreator createBundleCreator(final FileFilter fileFilter)
	{
		final List<File> moduleDirs = new ArrayList<File>();

		for (MavenProject module : getIncludedModules())
		{
			final File file = new File(module.getBuild().getOutputDirectory());
			if (file.exists())
			{
				moduleDirs.add(file);
			}
		}

		return new BundleCreator(project, fileFilter, moduleDirs);
	}

	private List<MavenProject> getIncludedModules()
	{
		List<MavenProject> included = new ArrayList<MavenProject>();
		for(MavenProject module: (List<MavenProject>)reactorProjects)
		{
			if(!skippedModules.contains(module.getArtifactId()))
			{
				included.add(module);
			}
		}
		return included;
	}

	@Override
	protected boolean isPackagingInvalid()
	{
		return !"pom".equals(project.getPackaging());
	}

	@Override
	protected String getOutputDirectory()
	{
		return outputDirectory.getAbsolutePath();
	}

	@Override
	protected List<File> getCompileSourceRoots()
	{
		final List<File> result = new ArrayList<File>();
		for (MavenProject module : getIncludedModules())
		{
			for (final Object path : module.getCompileSourceRoots())
			{
				result.add(resolvePath((String) path));
			}
		}
		return result;
	}

	@Override
	public void setReportOutputDirectory(final File reportOutputDirectory)
	{
		if (reportOutputDirectory != null
		    && !reportOutputDirectory.getAbsolutePath().endsWith("jacoco"))
		{
			outputDirectory = new File(reportOutputDirectory, "jacoco");
		}
		else
		{
			outputDirectory = reportOutputDirectory;
		}
	}

	@Override
	public void execute() throws MojoExecutionException
	{
		if (!dataFile.exists())
		{
			merge();
		}
		super.execute();
	}

	private void merge() throws MojoExecutionException
	{
		final ExecFileLoader loader = new ExecFileLoader();

		load(loader);
		save(loader);
	}

	private void load(final ExecFileLoader loader)
			throws MojoExecutionException
	{
		for (MavenProject module : getIncludedModules())
		{
			// TODO: hack to find exec file, isn't quite accurate if it's been moved out of the target directory
			File inputFile = new File(module.getBuild().getDirectory(), dataFile.getName());

			if (inputFile.exists())
			{
				try
				{
					getLog().info(
							"Loading execution data file "
							+ inputFile.getAbsolutePath());
					loader.load(inputFile);
				}
				catch (final IOException e)
				{
					throw new MojoExecutionException("Unable to read "
					                                 + inputFile.getAbsolutePath(), e);
				}
			}

		}
	}

	private void save(final ExecFileLoader loader)
			throws MojoExecutionException
	{
		if (loader.getExecutionDataStore().getContents().isEmpty())
		{
			getLog().info(MSG_SKIPPING);
			return;
		}
		getLog().info(
				"Writing merged execution data to "
				+ dataFile.getAbsolutePath());
		try
		{
			loader.save(dataFile, false);
		}
		catch (final IOException e)
		{
			throw new MojoExecutionException("Unable to write merged file "
			                                 + dataFile.getAbsolutePath(), e);
		}
	}

	@Override
	protected File getDataFile()
	{
		return dataFile;
	}

	@Override
	protected File getOutputDirectoryFile()
	{
		return outputDirectory;
	}

	public String getOutputName()
	{
		return "jacoco/index";
	}

	public String getName(final Locale locale)
	{
		return "JaCoCo Test";
	}
}
