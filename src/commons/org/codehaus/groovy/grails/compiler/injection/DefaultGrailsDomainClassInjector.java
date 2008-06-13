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
package org.codehaus.groovy.grails.compiler.injection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;

import java.lang.reflect.Modifier;
import java.util.*;
import java.net.URL;

/**
 * Default implementation of domain class injector interface that adds the 'id'
 * and 'version' properties and other previously boilerplate code
 * 
 * @author Graeme Rocher
 *
 * @since 0.2
 * 
 * Created: 20th June 2006
 */
public class DefaultGrailsDomainClassInjector implements
		GrailsDomainClassInjector {

	private static final Log LOG  = LogFactory.getLog(DefaultGrailsDomainClassInjector.class);
	
	public void performInjection(SourceUnit source, GeneratorContext context,
			ClassNode classNode) {

        if(shouldInject(classNode)) {
			injectIdProperty(classNode);
			
			injectVersionProperty(classNode);
			
			injectToStringMethod(classNode);
			
			injectAssociations(classNode);

        }
	}

    public boolean shouldInject(URL url) {
        return GrailsResourceUtils.isDomainClass(url);
    }

    private boolean shouldInject(ClassNode classNode) {
		String fullName = GrailsASTUtils.getFullName(classNode);
		String mappingFile = GrailsDomainConfigurationUtil.getMappingFileName(fullName);
		
		if(getClass().getResource(mappingFile)!=null) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("[GrailsDomainInjector] Mapping file ["+mappingFile+"] found. Skipping property injection.");
			}
			return false;
		}
		return true;
	}

	private void injectAssociations(ClassNode classNode) {
		
		List properties = classNode.getProperties();
		List propertiesToAdd = new ArrayList();
		for (Iterator p = properties.iterator(); p.hasNext();) {
			PropertyNode pn = (PropertyNode) p.next();
			final boolean isHasManyProperty = pn.getName().equals(GrailsDomainClassProperty.RELATES_TO_MANY) || pn.getName().equals(GrailsDomainClassProperty.HAS_MANY);
			if(isHasManyProperty) {
				Expression e = pn.getInitialExpression();
				propertiesToAdd.addAll(createPropertiesForHasManyExpression(e,classNode));
			}
            final boolean isBelongsTo = pn.getName().equals(GrailsDomainClassProperty.BELONGS_TO);
            if(isBelongsTo) {
                Expression e = pn.getInitialExpression();
                propertiesToAdd.addAll(createPropertiesForBelongsToExpression(e,classNode));
            }
        }
		injectAssociationProperties(classNode,propertiesToAdd);
	}

    private Collection createPropertiesForBelongsToExpression(Expression e, ClassNode classNode)
    {
        List properties = new ArrayList();
        if(e instanceof MapExpression) {
            MapExpression me = (MapExpression)e;
            List mapEntries = me.getMapEntryExpressions();
            for (Iterator i = mapEntries.iterator(); i.hasNext();) {
                MapEntryExpression mme = (MapEntryExpression) i.next();
                String key = mme.getKeyExpression().getText();

                String type = mme.getValueExpression().getText();

                properties.add(new PropertyNode(key,Modifier.PUBLIC, ClassHelper.make(type) , classNode, null,null,null));
            }
        }

        return properties;
    }

    private void injectAssociationProperties(ClassNode classNode, List propertiesToAdd) {
		for (Iterator i = propertiesToAdd.iterator(); i.hasNext();) {
			PropertyNode pn = (PropertyNode) i.next();
			if(!GrailsASTUtils.hasProperty(classNode, pn.getName())) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("[GrailsDomainInjector] Adding property [" + pn.getName() + "] to class [" + classNode.getName() + "]");
				}
				classNode.addProperty(pn);				
			}
		}
	}

	private List createPropertiesForHasManyExpression(Expression e, ClassNode classNode) {
		List properties = new ArrayList();
		if(e instanceof MapExpression) {
			MapExpression me = (MapExpression)e;
			List mapEntries = me.getMapEntryExpressions();
			for (Iterator j = mapEntries.iterator(); j.hasNext();) {
				MapEntryExpression mee = (MapEntryExpression) j.next();
				Expression keyExpression = mee.getKeyExpression();
				String key = keyExpression.getText();
				addAssociationForKey(key,properties,classNode);
			}
		}
		return properties;
	}

	private void addAssociationForKey(String key, List properties, ClassNode classNode) {
			properties.add(new PropertyNode(key, Modifier.PUBLIC, new ClassNode(Set.class), classNode,null, null, null));
	}

	private void injectToStringMethod(ClassNode classNode) {
		final boolean hasToString = GrailsASTUtils.implementsZeroArgMethod(classNode, "toString");
		
		if(!hasToString) {
			GStringExpression ge = new GStringExpression(classNode.getName() + " : ${id}");
			ge.addString(new ConstantExpression(classNode.getName()+" : "));
			ge.addValue(new VariableExpression("id"));			
			Statement s = new ReturnStatement(ge);			
			MethodNode mn = new MethodNode("toString",Modifier.PUBLIC,new ClassNode(String.class), new Parameter[0],new ClassNode[0],s);
			if(LOG.isDebugEnabled()) {
				LOG.debug("[GrailsDomainInjector] Adding method [toString()] to class [" + classNode.getName() + "]");
			}				
			classNode.addMethod(mn);
		}
	}

	private void injectVersionProperty(ClassNode classNode) {
		final boolean hasVersion = GrailsASTUtils.hasProperty(classNode, GrailsDomainClassProperty.VERSION);
		
		if(!hasVersion) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("[GrailsDomainInjector] Adding property [" + GrailsDomainClassProperty.VERSION + "] to class [" + classNode.getName() + "]");
			}			
			classNode.addProperty( GrailsDomainClassProperty.VERSION, Modifier.PUBLIC, new ClassNode(Long.class), null, null, null);
		}
	}

	private void injectIdProperty(ClassNode classNode) {
		final boolean hasId = GrailsASTUtils.hasProperty(classNode,GrailsDomainClassProperty.IDENTITY);
		
		if(!hasId) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("[GrailsDomainInjector] Adding property [" + GrailsDomainClassProperty.IDENTITY + "] to class [" + classNode.getName() + "]");
			}						
			classNode.addProperty( GrailsDomainClassProperty.IDENTITY, Modifier.PUBLIC, new ClassNode(Long.class), null, null, null);
		}
	}

}
