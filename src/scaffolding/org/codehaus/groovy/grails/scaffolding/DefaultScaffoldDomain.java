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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.scaffolding.exceptions.ScaffoldingException;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.InstantiationException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * The default implementation of a Scaffolding application that uses Hibernate as the persistence mechanism.
 * Provides simple CRUD facilities onto a Hibernate domain model.
 *
 * 
 * @author Graeme Rocher
 * @since 0.1
 *
 * Created 30 Nov 2005
 */
public class DefaultScaffoldDomain implements ScaffoldDomain {

	private static final Log LOG  = LogFactory.getLog(DefaultScaffoldDomain.class);
	private static final String LIST_SUFFIX = "List";
	private Class persistentClass;
	private HibernateTemplate template;

	private BeanWrapper bean;
	private Class identityClass;
	private Validator validator;
	private String identityPropertyName;
    private SimpleTypeConverter typeConverter = new SimpleTypeConverter();

    public DefaultScaffoldDomain(Class persistentClass,
			SessionFactory sessionFactory) {
		
		setPersistentClass(persistentClass);
		setSessionFactory(sessionFactory);		
	}
	

	/**
	 * @param validator The validator to set.
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}


	public void setIdentityPropertyName(String identityPropertyName) {
		PropertyDescriptor identityProp = this.bean.getPropertyDescriptor(identityPropertyName);
		identityClass = identityProp.getPropertyType();
		this.identityPropertyName = identityPropertyName;
	}

	/**
	 * @param sessionFactory The sessionFactory to set.
	 */
	protected void setSessionFactory(SessionFactory sessionFactory) {
        this.template = new HibernateTemplate(sessionFactory);
		this.template.setFlushMode(HibernateTemplate.FLUSH_AUTO);
	}

	public List list() {
		return template.loadAll(persistentClass);
	}

	public List list(final int max) {
		return list(max, -1,null,null);
	}

	public List list(final int max, final int offset) {
		return list(max, offset,null,null);
	}
	
	public List list(int max, int offset, String sort) {
		return list(max, offset,sort,null);
	}


	public List list(final int max, final int offset, final String sort, final String order) {
		return template.executeFind( new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Criteria c = session.createCriteria(persistentClass);
				
				if(max > -1)
					c.setMaxResults(max);
				if(offset > -1)
					c.setFirstResult(offset);
				
				if(sort != null) {
					if(order != null) {
						if(order.equalsIgnoreCase("DESC")) {
							c.addOrder(Order.desc(sort));
						}
						else {
							c.addOrder(Order.asc(sort));	
						}
					}
					else {
						c.addOrder(Order.asc(sort));
					}

				}				
				return c.list();
			}			
		});
	}
	

	public List find(String by, Object q) {
		return find(by,q,-1,-1);
	}

	public List find(String by, Object q, int max) {
		return find(by,q,max,-1);
	}

	public List find(final String by, Object q, final int max, final int offset) {
		if(StringUtils.isBlank(by) || q == null)
			return new ArrayList();
		
		if(this.bean.isReadableProperty(by)) {
			Class propertyType = this.bean.getPropertyType(by);
			if(!propertyType.isAssignableFrom( q.getClass() )) {
				q = typeConverter.convertIfNecessary( q, propertyType);
			}
			
			final Object query = q;
			if(q != null) {
				return this.template.executeFind( new HibernateCallback() {
					public Object doInHibernate(Session session) throws HibernateException, SQLException {
						Criteria c = session.createCriteria(persistentClass);
						if(max > -1)
							c.setMaxResults(max);
						if(offset > -1)
							c.setFirstResult(offset);
						
						c.add( Restrictions.eq( by, query ) );
						
						return c.list();
					}					
				});
			}
		}
		return new ArrayList();
	}

	public List find(String[] by, Object[] q) {
		return find(by,q,-1,-1);
	}

	public List find(String[] by, Object[] q, int max) {
		return find(by,q,max,-1);
	}

	public List find(final String[] by, Object[] q, final int max, final int offset) {
		if(by == null || by.length == 0 || q == null || q.length == 0)
			return new ArrayList();
		
		
		final List ignoreList = new ArrayList();
		
		for (int i = 0; i < by.length; i++) {
			if(this.bean.isReadableProperty(by[i]) && i < q.length) {
				Class propertyType = this.bean.getPropertyType(by[i]);
				if(!propertyType.isAssignableFrom( q[i].getClass() )) {
					q[i] = typeConverter.convertIfNecessary(q[i], propertyType);
				}
				if(q[i] == null) {
					ignoreList.add(by[i]);
				}
			}
			else {
				ignoreList.add(by[i]);
			}
		}
		
		final Object[] query = q; 
		if(by.length < ignoreList.size()) {
			return this.template.executeFind( new HibernateCallback() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					Criteria c = session.createCriteria(persistentClass);
					if(max > -1)
						c.setMaxResults(max);
					if(offset > -1)
						c.setFirstResult(offset);
					
					for (int i = 0; i < by.length; i++) {
						if(!ignoreList.contains(by[i])) {
							c.add( Restrictions.eq( by[i], query[i] ) );
						}
					}										
					return c.list();
				}					
			});			
		}
		return new ArrayList();
	}

	public boolean save(Object domainObject, ScaffoldCallback callback) {
		
			if(validator != null) {
				Errors errors = new BindException(domainObject,persistentClass.getName());				
				validator.validate(domainObject,errors);
				if(errors.hasErrors()) {
					callback.setErrors(errors);
					callback.setInvoked(false);
					return false;
				}					
			}
			
			template.save(domainObject);			
		
		return true;
	}
	
	public boolean update(final Object domainObject, final ScaffoldCallback callback) {		
		
		template.execute( new HibernateCallback() {

			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				Transaction tx = session.beginTransaction();
				if(validator != null) {
					Errors errors = new BindException(domainObject,persistentClass.getName());
					validator.validate(domainObject,errors);
					if(errors.hasErrors()) {
						callback.setErrors(errors);
						callback.setInvoked(false);					
						return null;
					}					
				}	
							
				session.update(domainObject);
				tx.commit();
				
				if(LOG.isTraceEnabled()) {
					LOG.trace("[ScaffoldDomain] Updated ["+domainObject+"]. Setting invoked.");
				}
				callback.setInvoked(true);
				
				
				return domainObject;
			}
			
		});
		
		return callback.isInvoked();
	}	

	public Object delete(Serializable id) {
		if(id == null)
			throw new IllegalArgumentException("Argument 'id' cannot be null");
		
		id = (Serializable)typeConverter.convertIfNecessary(id,identityClass);
		
		Object instance = template.get(persistentClass,id);
		if(instance != null) {
			template.delete(instance);
		}
		return instance;
	}



	public void setPersistentClass(Class persistentClass) {
		if(persistentClass == null)
			throw new IllegalArgumentException("Argument 'persistentClass' cannot be null");
		
		this.persistentClass = persistentClass;
		try {
			this.bean = new BeanWrapperImpl(persistentClass.newInstance());
		} catch (InstantiationException e) {
			throw new ScaffoldingException("Unable to instantiate persistent class ["+persistentClass+"] for scaffolding: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new ScaffoldingException("Unable to instantiate persistent class ["+persistentClass+"] for scaffolding: " + e.getMessage(), e);
		}
	}

	public Object get(Serializable id) {
		if(id == null)
			throw new IllegalArgumentException("Argument 'id' cannot be null");
		if(identityClass != null)
		    id = (Serializable)typeConverter.convertIfNecessary(id,identityClass);

        return template.get(persistentClass,id);
	}

	public String getPluralName() {
		return GrailsClassUtils.getPropertyNameRepresentation(persistentClass) + LIST_SUFFIX;
	}

	public Class getPersistentClass() {
		return this.persistentClass;
	}

	public String getSingularName() {
        return GrailsClassUtils.getPropertyNameRepresentation(persistentClass);
	}


	public String getIdentityPropertyName() {
		return this.identityPropertyName;
	}


	public String getName() {
		return this.persistentClass.getName();
	}


	public Object newInstance() {
		try {
			return this.persistentClass.newInstance();		
		} catch (InstantiationException e) {
			throw new ScaffoldingException("Unable to instantiate persistent class ["+persistentClass+"] for scaffolding: " + e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new ScaffoldingException("Unable to instantiate persistent class ["+persistentClass+"] for scaffolding: " + e.getMessage(), e);
		} 
	}


	public void setSessionFactory(Object sessionFactory) {
		if(sessionFactory instanceof SessionFactory) {
			setSessionFactory((SessionFactory)sessionFactory);
		}
	}





}
