/*
 * Copyright 2010 Stefano Fornari.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.mojo.script;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.project.MavenProject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * A Maven 2 plugin runs a script using javax.script.
 *
 * @goal execute
 * @requiresDependencyResolution
 * @description Runs scripts supported by JSR223
 * @author Stefano Fornari
 */
public class ScriptMojo extends AbstractMojo {

    public static final String DEFAULT_NAME_OF_PROJECT_PROPERTY = "project";

    /**
     * The project to create a build for.
     *
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The location in-line script to be executed. Note that this is executed
     * after the scripts included in src/main/scripts.
     *
     * @parameter
     */
    private String script;

    /**
     * Pass the project object in as a property to your script.
     *
     * @parameter default-value="false"
     */
    private boolean passProjectAsProperty;

    /**
     * Name for project object as a property in your script.
     *
     * @parameter default-value="project"
     */
    private String nameOfProjectProperty;

    /**
     * Optional name of language.
     *
     * @parameter
     * @required
     */
    private String language;

    /**
     * Optional extension of language.
     *
     * <tt>extension</tt>
     *
     * @parameter
     */
    private String extension;

    /**
     * Optional mimeType of language.
     *
     * <tt>mimeType</tt>
     *
     * @parameter
     */
    private String mimeType;

    /**
     * Patterns of the script files to exclude from execution
     *
     * @parameter
     */
    private String[] excludes;

    /**
     * Patterns of the script files to include for execution
     *
     * @parameter
     */
    private String[] includes;


    /**
     * The engine created when the mojo is executed.
     */
    private ScriptEngine engine;


    /**
     * Loads and executes a script.
     *
     * @throws MojoExecutionException if script cannot be loaded or executed.
     */
    public void execute() throws MojoExecutionException {
        ScriptEngineManager engineManager = new ScriptEngineManager();

        if (language != null) {
            if ((engine = engineManager.getEngineByName(language)) == null) {
                throw new MojoExecutionException("No engine with language " + language + " has been found");
            }
        } else if (extension != null) {
            if ((engine = engineManager.getEngineByExtension(extension)) == null) {
                throw new MojoExecutionException("No engine with extension " + extension + " has been found");
            }
        } else if (mimeType != null) {
            if ((engine = engineManager.getEngineByMimeType(mimeType)) == null) {
                throw new MojoExecutionException("No engine with mimeType " + mimeType + " has been found");
            }
        } else {
            throw new MojoExecutionException("One of language|extension|mimeType must be specified");
        }

        if ( passProjectAsProperty ) {
            if (nameOfProjectProperty == null) {
                nameOfProjectProperty = DEFAULT_NAME_OF_PROJECT_PROPERTY;
            }
            engine.put(nameOfProjectProperty, project);
        }

        try {
            executeScripts();
            executeInlineScript();
        } catch ( Exception e ) {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        
    }

    // --------------------------------------------------------- Private methods

    /**
     * Executes the scripts in the script directory. Includes and excludes
     * patterns are used to filter the scripts to execute.
     *
     * @throws ScriptException if there is an error in the execution of a scripts
     */
    private void executeScripts() throws ScriptException, IOException {
        ArrayList<String> scriptFileNames = new ArrayList<String>();

        DirectoryScanner ds = new DirectoryScanner();
        ds.setCaseSensitive(true);

        if (includes != null) {
            ds.setIncludes(includes);
        }
        if (excludes != null) {
            ds.setExcludes(excludes);
        }
        
        for (Object d: project.getScriptSourceRoots()) {
            ds.setBasedir((String)d);
            ds.scan();

            for (String f: ds.getIncludedFiles()) {
                scriptFileNames.add(d + File.separator + f);
            }
        }

        for (String fileName: scriptFileNames) {
            engine.eval(new FileReader(fileName));
        }

    }

    /**
     * Executes the inline scripts (if given).
     *
     * @throws ScriptException if there is an error in the execution of a scripts
     *
     */
    private void executeInlineScript() throws ScriptException {
        if (script != null) {
            engine.eval(script);
        }
    }
}
