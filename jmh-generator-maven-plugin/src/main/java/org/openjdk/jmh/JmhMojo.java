package org.openjdk.jmh;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.openjdk.jmh.generators.bytecode.ASMGeneratorSource;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;
import org.openjdk.jmh.generators.core.FileSystemDestination;
import org.openjdk.jmh.generators.core.SourceError;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @requiresDependencyResolution compile+runtime
 * @goal generate
 * @phase process-sources
 */
public class JmhMojo extends AbstractMojo {

    /**
     * Classes dir
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File compiledBytecodeDirectory;

    /**
     * Classes dir
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputResourceDirectory;

    /**
     * Classes dir
     *
     * @parameter expression="${project.build.directory}/generated-sources/jmh/"
     * @required
     */
    private File outputSourceDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    public void extendPluginClasspath() throws MojoExecutionException {
        try {
            Set<URL> urls = new HashSet<URL>();
            List<String> elements = new ArrayList<String>();
            elements.addAll(project.getTestClasspathElements());
            elements.addAll(project.getRuntimeClasspathElements());
            elements.addAll(project.getCompileClasspathElements());
            elements.addAll(project.getSystemClasspathElements());
            for (String element : elements) {
                urls.add(new File(element).toURI().toURL());
            }

            ClassLoader contextClassLoader = URLClassLoader.newInstance(
                    urls.toArray(new URL[urls.size()]),
                    Thread.currentThread().getContextClassLoader());

            Thread.currentThread().setContextClassLoader(contextClassLoader);
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Internal error", e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Internal error", e);
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        extendPluginClasspath();

        ASMGeneratorSource source = new ASMGeneratorSource();
        FileSystemDestination destination = new FileSystemDestination(outputResourceDirectory, outputSourceDirectory);

        try {
            Collection<File> classes = getClasses(compiledBytecodeDirectory);
            getLog().info("Processing " + classes.size() + " classes from " + compiledBytecodeDirectory);
            getLog().info("Writing out Java source to "  + outputSourceDirectory + " and resources to " + outputResourceDirectory);
            source.processClasses(classes);
        } catch (IOException ioe) {
            throw new MojoExecutionException("IOException", ioe);
        }

        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source, destination);
        gen.complete(source, destination);

        if (destination.hasErrors()) {
            for (SourceError e : destination.getErrors()) {
                getLog().error(e.toString() + "\n");
            }
            throw new MojoFailureException("Errors detected.");
        }
    }

    public Collection<File> getClasses(File root) {
        Collection<File> result = new ArrayList<File>();

        List<File> newDirs = new ArrayList<File>();
        newDirs.add(root);
        while (!newDirs.isEmpty()) {
            List<File> add = new ArrayList<File>();
            for (File dir : newDirs) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            add.add(f);
                        } else {
                            if (f.getName().endsWith(".class")) {
                                result.add(f);
                            }
                        }
                    }
                }
            }
            newDirs.clear();
            newDirs = add;
        }

        return result;
    }

}
