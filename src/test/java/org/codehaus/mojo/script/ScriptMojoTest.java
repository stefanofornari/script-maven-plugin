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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;


/**
 *
 * @author ste
 */
public class ScriptMojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    private ScriptMojo getScriptMojo(String pomFileName) throws Exception {
        File testPom = new File(getBasedir(), pomFileName);
        ScriptMojo mojo = (ScriptMojo) lookupMojo ("execute", testPom );

        MavenProjectStub project = new MavenProjectStub();
        project.setScriptSourceRoots(
            Arrays.asList(new String[] {"target/test-classes/scripts"})
        );

        Field f = mojo.getClass().getDeclaredField("project");

        f.setAccessible(true);

        f.set(mojo, project);

        return mojo;
    }

    private void _testConfigurationKo(String pomFileName) throws Exception {
        ScriptMojo mojo = getScriptMojo(pomFileName);
        try {
            mojo.execute();
            fail("The execution was supposed to fail, but it did not.");
        } catch (MojoExecutionException e) {
            // this is ok
        }
    }

    private void _testConfigurationOk(String pomFileName) throws Exception {
        ScriptMojo mojo = getScriptMojo(pomFileName);

        mojo.execute();
    }

    public void testConfiguration() throws Exception {
        _testConfigurationKo("target/test-classes/unit/basic-test/empty-config.xml");

        _testConfigurationOk("target/test-classes/unit/basic-test/language-config.xml");
        _testConfigurationOk("target/test-classes/unit/basic-test/extension-config.xml");
        _testConfigurationOk("target/test-classes/unit/basic-test/mime-config.xml");
        _testConfigurationKo("target/test-classes/unit/basic-test/invalid-language-config.xml");
        _testConfigurationKo("target/test-classes/unit/basic-test/invalid-extension-config.xml");
        _testConfigurationKo("target/test-classes/unit/basic-test/invalid-mime-config.xml");
    }

    public void testCreateEngine() throws Exception {
        ScriptMojo mojo = getScriptMojo("target/test-classes/unit/basic-test/language-config.xml");

        Method m = mojo.getClass().getDeclaredMethod("createEngine");
        m.setAccessible(true);
        m.invoke(mojo);
        assertNotNull(mojo.getEngine());
    }

    public void testInLineScript() throws Exception {
        ScriptMojo mojo = getScriptMojo("target/test-classes/unit/basic-test/inline-script-config.xml");

        mojo.execute();

        assertTrue((Boolean)mojo.getEngine().get("executed"));
    }

    public void testProjectProperty() throws Exception {
        ScriptMojo mojo
            = getScriptMojo("target/test-classes/unit/basic-test/project-no-config.xml");
        mojo.execute();
        assertNull(mojo.getEngine().get("project"));

        mojo = getScriptMojo("target/test-classes/unit/basic-test/project-default-config.xml");
        mojo.execute();
        assertNotNull(mojo.getEngine().get("project"));

        mojo = getScriptMojo("target/test-classes/unit/basic-test/project-property-config.xml");
        mojo.execute();
        assertNotNull(mojo.getEngine().get("newproject"));
    }

    public void testScriptsAll() throws Exception {
        ScriptMojo mojo = getScriptMojo("target/test-classes/unit/filter-test/l1-all-config.xml");

        mojo.execute();

        assertEquals("undefined,l0-1.js,l1-1.js,l2-1.js,l2-2.js", mojo.getEngine().get("result"));
    }

    public void testOneScript() throws Exception {
        ScriptMojo mojo =
            getScriptMojo("target/test-classes/unit/filter-test/one-script-config.xml");

        mojo.execute();

        assertEquals("undefined,l0-1.js", mojo.getEngine().get("result"));
    }

    public void testInlineScriptOnly() throws Exception {
        ScriptMojo mojo =
            getScriptMojo("target/test-classes/unit/filter-test/inline-only-config.xml");

        mojo.execute();

        assertNull(mojo.getEngine().get("result"));
        assertTrue((Boolean)mojo.getEngine().get("executed"));
    }

    public void testExcludeOne() throws Exception {
        ScriptMojo mojo =
            getScriptMojo("target/test-classes/unit/filter-test/exclude-one-config.xml");

        mojo.execute();

        assertEquals("undefined,l0-1.js,l1-1.js,l2-2.js", mojo.getEngine().get("result"));
    }

}
