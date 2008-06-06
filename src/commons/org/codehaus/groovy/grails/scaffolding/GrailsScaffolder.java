/*
 * Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.scaffolding;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
/**
 * An interface that facilitates the required methods for using Scaffolding within GrailsControllers 
 * 
 * @author Graeme Rocher
 * @since 30 Nov 2005
 */
public interface GrailsScaffolder {

    String INDEX_ACTION = "index";
    String LIST_ACTION = "list";
    String SHOW_ACTION = "show";
    String EDIT_ACTION = "edit";
    String DELETE_ACTION = "delete";
    String CREATE_ACTION = "create";
    String SAVE_ACTION = "save";
    String UPDATE_ACTION = "update";
    // TODO: Implement search scaffolding
    String SEARCH_ACTION = "search";
    String FIND_ACTION = "find";

    public String[] ACTION_NAMES = new String[] {
        INDEX_ACTION,
        LIST_ACTION,
        SHOW_ACTION,
        EDIT_ACTION,
        DELETE_ACTION,
        CREATE_ACTION,
        SAVE_ACTION,
        UPDATE_ACTION
    };

    /**
	 * @param actionName The name of the action
	 * @return True if the action is supported by the scaffolder
	 */
	boolean supportsAction(String actionName);
	
	/**
	 * @return A String array of actions names supported by the scaffolder
	 */
	String[] getSupportedActionNames();
	/**
	 * @param actionName
	 * @return A Closure instance for the specified action name
	 */
	Closure getAction(GroovyObject controller,String actionName);
	
	/**
	 * Returns the action name for the specified closure instance
	 * 
	 * @param action The closure action
	 * @return The name of the action
	 */
	String getActionName(Closure action);

    /**
     *
     *  @return The ScaffoldRequestHandler instance
     */
    ScaffoldRequestHandler getScaffoldRequestHandler();

}
