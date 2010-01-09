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
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
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
     * Optional extension of language fo the inline script if given.
     *
     * @parameter
     */

    private String language;
    
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
    private Map<String, ScriptEngine> engines;

    // ---------------------------------------------------------- Public methods

    /**
     * Returns the ScripEngine for the given script extension. Engines are
     * stored and reused so that only one engine per extension will be returned.
     * If the engine for a given extension does not exist, EngineNotFoundException
     * is thrown.
     *
     * @return the engine
     *
     * @throws EngineNotFoundException if no engine is found for the given extension
     */
    public ScriptEngine getEngine(String extension)
    throws EngineNotFoundException {
        //
        // Let's initialize engines if necessary...
        //
        if (engines == null) {
            engines = new HashMap<String, ScriptEngine>();
        }

        ScriptEngine engine = engines.get(extension);

        if (engine == null) {
            ScriptEngineManager engineManager = new ScriptEngineManager();

            if ((engine = engineManager.getEngineByExtension(extension)) == null) {
                throw new EngineNotFoundException(extension);
            }

            if (passProjectAsProperty) {
                if (nameOfProjectProperty == null) {
                    nameOfProjectProperty = DEFAULT_NAME_OF_PROJECT_PROPERTY;
                }
                engine.put(nameOfProjectProperty, project);
            }
            engines.put(extension, engine);
        }
        return engine;
    }

    /**
     * Loads and executes a script.
     *
     * @throws MojoExecutionException if script cannot be loaded or executed.
     *
     * @Override
     */
    public void execute() throws MojoExecutionException {
        try {
            executeScripts();
            executeInlineScript();
        } catch (Throwable t) {
            throw new MojoExecutionException(t.getMessage(), t);
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

        for (Object d : project.getScriptSourceRoots()) {
            if (!new File((String) d).isDirectory()) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("Script directory " + d + " not found, ignoring.");
                }
                continue;
            }
            ds.setBasedir((String) d);
            ds.scan();

            for (String f : ds.getIncludedFiles()) {
                scriptFileNames.add(d + File.separator + f);
            }
        }

        for (String fileName : scriptFileNames) {
            String extension = FilenameUtils.getExtension(fileName);

            try {
                getEngine(extension).eval(new FileReader(fileName));
            } catch (EngineNotFoundException e) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("Script engine for " + fileName + " not found, ignoring.");
                }
            }
        }

    }

    /**
     * Executes the inline scripts (if given).
     *
     * @throws EngineNotFoundException if no engine is available for the given
     *         language extension
     * @throws ScriptException if there is an error in the execution of a scripts
     *
     */
    private void executeInlineScript()
    throws EngineNotFoundException, ScriptException {
        if (script == null) {
            return;
        }

        if (language == null) {
            throw new EngineNotFoundException("language cannot be null if an inline script is provided");
        }
        getEngine(language).eval(script);
    }

    /**
     * @param project the project to set
     */
    public void setProject(MavenProject project) {
        this.project = project;
    }

}
