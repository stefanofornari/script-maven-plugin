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

    // ---------------------------------------------------------- Public methods

    /**
     * @return the project
     */
    public MavenProject getProject() {
        return project;
    }

    /**
     * @return the script
     */
    public String getScript() {
        return script;
    }

    /**
     * @return the passProjectAsProperty
     */
    public boolean isPassProjectAsProperty() {
        return passProjectAsProperty;
    }

    /**
     * @return the nameOfProjectProperty
     */
    public String getNameOfProjectProperty() {
        return nameOfProjectProperty;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @return the excludes
     */
    public String[] getExcludes() {
        return excludes;
    }

    /**
     * @return the includes
     */
    public String[] getIncludes() {
        return includes;
    }

    /**
     * @return the engine
     */
    public ScriptEngine getEngine() {
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
            createEngine();
            executeScripts();
            executeInlineScript();
        } catch (Throwable t) {
            throw new MojoExecutionException( t.getMessage(), t );
        }
        
    }

    // ------------------------------------------------------- Protected methods

    /**
     * Executes the scripts in the script directory. Includes and excludes
     * patterns are used to filter the scripts to execute.
     *
     * @throws ScriptException if there is an error in the execution of a scripts
     */
    protected void executeScripts() throws ScriptException, IOException {
        ArrayList<String> scriptFileNames = new ArrayList<String>();

        DirectoryScanner ds = new DirectoryScanner();
        ds.setCaseSensitive(true);

        if (getIncludes() != null) {
            ds.setIncludes(getIncludes());
        }
        if (getExcludes() != null) {
            ds.setExcludes(getExcludes());
        }
        
        for (Object d: getProject().getScriptSourceRoots()) {
            if (!new File((String)d).isDirectory()) {
                if (getLog().isWarnEnabled()) {
                    getLog().warn("Script directory " + d + " not found, ignoring.");
                }
                continue;
            }
            ds.setBasedir((String)d);
            ds.scan();

            for (String f: ds.getIncludedFiles()) {
                scriptFileNames.add(d + File.separator + f);
            }
        }

        for (String fileName: scriptFileNames) {
            getEngine().eval(new FileReader(fileName));
        }

    }

    /**
     * Executes the inline scripts (if given).
     *
     * @throws ScriptException if there is an error in the execution of a scripts
     *
     */
    protected void executeInlineScript() throws ScriptException {
        if (getScript() != null) {
            getEngine().eval(getScript());
        }
    }

    /**
     * Creates the script engine given the configured parameters.
     *
     * @throws RuntimeException in case the engine cannot be created
     */
    protected void createEngine() throws RuntimeException {
        ScriptEngineManager engineManager = new ScriptEngineManager();

        if (getLanguage() != null) {
            if ((engine = engineManager.getEngineByName(getLanguage())) == null) {
                throw new RuntimeException("No engine with language " + getLanguage() + " has been found");
            }
        } else if (getExtension() != null) {
            if ((engine = engineManager.getEngineByExtension(getExtension())) == null) {
                throw new RuntimeException("No engine with extension " + getExtension() + " has been found");
            }
        } else if (getMimeType() != null) {
            if ((engine = engineManager.getEngineByMimeType(getMimeType())) == null) {
                throw new RuntimeException("No engine with mimeType " + getMimeType() + " has been found");
            }
        } else {
            throw new RuntimeException("One of language|extension|mimeType must be specified");
        }

        if ( isPassProjectAsProperty() ) {
            if (getNameOfProjectProperty() == null) {
                nameOfProjectProperty = DEFAULT_NAME_OF_PROJECT_PROPERTY;
            }
            engine.put(getNameOfProjectProperty(), getProject());
        }
    }
}
