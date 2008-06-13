/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;

import groovy.lang.GroovyClassLoader;
import junit.framework.TestCase;

/**
 * @author Marc Palmer
 * @since 22-Feb-2007
 */
public class ControllerArtefactHandlerTests extends TestCase {

    public void testIsControllerClass() throws Exception
    {
        GroovyClassLoader gcl = new GroovyClassLoader();

        Class c = gcl.parseClass("class TestController { }\n");

        ArtefactHandler handler = new ControllerArtefactHandler();
        assertTrue(handler.isArtefact(c));
    }

}
