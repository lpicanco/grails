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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.MissingPropertyException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A generic dyanmic property for any type
 * 
 * @author Graeme Rocher
 * @since Oct 27, 2005
 */
public class GenericDynamicProperty extends AbstractDynamicProperty {

	private Class type;
	private boolean readyOnly;
	private Map propertyToInstanceMap = Collections.synchronizedMap(new HashMap());
	private Object initialValue;
    private FunctionCallback initialValueGenerator;

    /**
	 * 
	 * @param propertyName The name of the property
	 * @param type The type of the property
	 * @param initialValue The initial value of the property
	 * @param readOnly True for read-only property
	 */
	public GenericDynamicProperty(String propertyName, Class type,Object initialValue,boolean readOnly) {
		super(propertyName);
		if(type == null)
			throw new IllegalArgumentException("Constructor argument 'type' cannot be null");
		this.readyOnly = readOnly;
		this.type = type;
		this.initialValue = initialValue;
	}
	/**
	 * 
	 * @param propertyName The name of the property
	 * @param type The type of the property
	 * @param readOnly True for read-only property
	 */
	public GenericDynamicProperty(String propertyName, Class type,boolean readOnly) {
		super(propertyName);
		if(type == null)
			throw new IllegalArgumentException("Constructor argument 'type' cannot be null");
		this.readyOnly = readOnly;
		this.type = type;;
	}
	

	/**
     * <p>Variant that allows supply of a lazy-initialization function for the initial value.</p>
     * <p>This function is called only on the first access to the property for a given object, unless
     * the function returns null in which case it will be called again if another request is made.</p>
	 * 
	 * @param propertyName The name of the property
	 * @param type The type of the property
	 * @param readOnly True for read-only property
	 */
	public GenericDynamicProperty(String propertyName, Class type, FunctionCallback initialValueGenerator,
            boolean readOnly) {
		this(propertyName, type, readOnly);
        this.initialValueGenerator = initialValueGenerator;
    }


	public Object get(Object object) {
		String propertyKey = System.identityHashCode(object) + getPropertyName();
		if(propertyToInstanceMap.containsKey(propertyKey)) {
			return propertyToInstanceMap.get(propertyKey);
		}
        else if (this.initialValueGenerator != null) {
            final Object value = this.initialValueGenerator.execute(object);
            propertyToInstanceMap.put(propertyKey, value);
            return value;
        }
        else if (this.initialValue != null) {
            propertyToInstanceMap.put(propertyKey, this.initialValue);
			return this.initialValue;
		}
		return null;
	}

	public void set(Object object, Object newValue) {		
		if(!readyOnly) {
            if(newValue == null) {
                propertyToInstanceMap.put(String.valueOf(System.identityHashCode(object)) + getPropertyName(), null );
            }
            else if(this.type.isInstance(newValue))
				propertyToInstanceMap.put(String.valueOf(System.identityHashCode(object)) + getPropertyName(), newValue );
			else
				throw new MissingPropertyException("Property '"+this.getPropertyName()+"' for object '"+object.getClass()+"' cannot be set with value '"+newValue+"'. Incorrect type.",object.getClass());	
		}
		else {
			throw new MissingPropertyException("Property '"+this.getPropertyName()+"' for object '"+object.getClass()+"' is read-only!",object.getClass());
		}
	}
}
