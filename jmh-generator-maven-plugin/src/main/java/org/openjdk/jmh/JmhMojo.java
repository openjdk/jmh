package org.openjdk.jmh;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.openjdk.jmh.generators.bytecode.ASMGeneratorSource;
import org.openjdk.jmh.generators.bytecode.SourceError;
import org.openjdk.jmh.generators.core.BenchmarkGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
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

    public void execute() throws MojoExecutionException, MojoFailureException {
        ASMGeneratorSource source = new ASMGeneratorSource(outputResourceDirectory, outputSourceDirectory);

        try {
            Collection<File> classes = getClasses(compiledBytecodeDirectory);
            getLog().info("Processing " + classes.size() + " classes from " + compiledBytecodeDirectory);
            getLog().info("Writing out Java source to "  + outputSourceDirectory + " and resources to " + outputResourceDirectory);
            source.processClasses(classes);
        } catch (IOException ioe) {
            throw new MojoExecutionException("IOException", ioe);
        }

        BenchmarkGenerator gen = new BenchmarkGenerator();
        gen.generate(source);
        gen.complete(source);

        if (source.hasErrors()) {
            for (SourceError e : source.getErrors()) {
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
