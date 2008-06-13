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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.metaclass.AbstractDynamicMethods;
import org.hibernate.SessionFactory;

import java.beans.IntrospectionException;

/**
 * A class responsible for dealing with matching and dispatching to dynamic finder methods
 *
 * @author Graeme Rocher
 * @author Steven Devijver
 *
 * @since 0.1
 *
 * Created: Aug 7, 2005
 */
public class DomainClassMethods extends AbstractDynamicMethods  {

    public static final String ERRORS_PROPERTY = "errors";
    
    public DomainClassMethods(GrailsApplication application, Class theClass, SessionFactory sessionFactory, ClassLoader classLoader)
            throws IntrospectionException {
        super(theClass);
        // dynamic methods

        addStaticMethodInvocation(new FindAllByPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new FindByPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new CountByPersistentMethod(application,sessionFactory, classLoader));
        addStaticMethodInvocation(new ListOrderByPersistentMethod(sessionFactory, classLoader));
    }

}
