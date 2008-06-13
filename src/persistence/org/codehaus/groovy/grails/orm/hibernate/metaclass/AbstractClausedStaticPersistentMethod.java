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
package org.codehaus.groovy.grails.orm.hibernate.metaclass;

import groovy.lang.GString;
import groovy.lang.MissingMethodException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.*;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Expression;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Graeme Rocher
 * @since 31-Aug-2005
 *
 */
public abstract class AbstractClausedStaticPersistentMethod extends
		AbstractStaticPersistentMethod {

	private static final Log LOG = LogFactory.getLog(AbstractClausedStaticPersistentMethod.class);
	/**
	 *
	 * @author Graeme Rocher
	 *
	 */
	protected abstract static class GrailsMethodExpression {
		private static final String LESS_THAN = "LessThan";
		private static final String LESS_THAN_OR_EQUAL = "LessThanEquals";
		private static final String GREATER_THAN = "GreaterThan";
		private static final String GREATER_THAN_OR_EQUAL = "GreaterThanEquals";
		private static final String LIKE = "Like";
        private static final String ILIKE = "Ilike";
		private static final String BETWEEN = "Between";
		private static final String IS_NOT_NULL = "IsNotNull";
		private static final String IS_NULL = "IsNull";
		private static final String NOT = "Not";
		private static final String EQUAL = "Equal";
		private static final String NOT_EQUAL = "NotEqual";


		protected String propertyName;
		protected Object[] arguments;
		protected int argumentsRequired;
		protected boolean negation;
		protected String type;
		protected Class targetClass;
		private GrailsApplication application;
		private TypeConverter converter = new SimpleTypeConverter();


		GrailsMethodExpression(GrailsApplication application,Class targetClass,String propertyName, String type,int argumentsRequired,boolean negation) {
			this.application = application;
			this.targetClass = targetClass;
			this.propertyName = propertyName;
			this.type = type;
			this.argumentsRequired = argumentsRequired;
			this.negation = negation;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer("[GrailsMethodExpression] ");
			buf.append(propertyName)
				.append(" ")
				.append(type)
				.append(" ");

			for (int i = 0; i < arguments.length; i++) {
				buf.append(arguments[i]);
				if(i != arguments.length)
					buf.append(" and ");
			}
			return buf.toString();
		}

		void setArguments(Object[] args)
			throws IllegalArgumentException {
			if(args.length != argumentsRequired)
				throw new IllegalArgumentException("Method expression '"+this.type+"' requires " + argumentsRequired + " arguments");

			GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, targetClass.getName());
			GrailsDomainClassProperty prop = dc.getPropertyByName(propertyName);

			if(prop == null)
				throw new IllegalArgumentException("Property "+propertyName+" doesn't exist for method expression '"+this.type+"'");

			for (int i = 0; i < args.length; i++) {
                if(args[i] == null) continue;
                // convert GStrings to strings
				if(prop.getType() == String.class && (args[i] instanceof GString)) {
					args[i] = args[i].toString();
				}
				else if(!prop.getType().isAssignableFrom( args[i].getClass() ) && !(GrailsClassUtils.isMatchBetweenPrimativeAndWrapperTypes(prop.getType(), args[i].getClass()))) {
					try {
						args[i] = converter.convertIfNecessary( args[i], prop.getType());
					} catch ( TypeMismatchException tme ) {
						// if we cannot perform direct conversion and argument is subclass of Number
						// we can try to convert it through it's String representation
						if(Number.class.isAssignableFrom(args[i].getClass())) {
							try {
								args[i] = converter.convertIfNecessary( args[i].toString(), prop.getType());
							} catch( TypeMismatchException tme1 ) {

                                throw new IllegalArgumentException("Cannot convert value " + args[i] + " of property '"+propertyName+"' to required type " + prop.getType() + ": " + tme1.getMessage());
							}
						} else {
							throw new IllegalArgumentException("Cannot convert value " + args[i] + " of property '"+propertyName+"' to required type " + prop.getType());
						}
					}
				}
			}

			this.arguments = args;
		}

		abstract Criterion createCriterion();
		protected Criterion getCriterion() {
			if(arguments == null)
				throw new IllegalStateException("Parameters array must be set before retrieving Criterion");

			if(negation) {
				return Expression.not( createCriterion() );
			}
			else {
				return createCriterion();
			}
		}

		protected static GrailsMethodExpression create(final GrailsApplication application,Class clazz, String queryParameter) {
			if(queryParameter.endsWith( LESS_THAN_OR_EQUAL )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, LESS_THAN_OR_EQUAL),
						LESS_THAN_OR_EQUAL,
						1,
						isNegation(queryParameter, LESS_THAN_OR_EQUAL) ) {
					Criterion createCriterion() {
						return Expression.le( this.propertyName, arguments[0] );
					}
				};
			}
			else if(queryParameter.endsWith( LESS_THAN )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, LESS_THAN),
						LESS_THAN,
						1, // argument count
						isNegation(queryParameter, LESS_THAN) ) {
					Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNull(this.propertyName);
                        return Expression.lt( this.propertyName, arguments[0] );
					}
				};
			}
			else if(queryParameter.endsWith( GREATER_THAN_OR_EQUAL )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, GREATER_THAN_OR_EQUAL),
						GREATER_THAN_OR_EQUAL,
						1,
						isNegation(queryParameter, GREATER_THAN_OR_EQUAL) ) {
					Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNull(this.propertyName);
                        return Expression.ge( this.propertyName, arguments[0] );
					}
				};
			}
			else if(queryParameter.endsWith( GREATER_THAN )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, GREATER_THAN),
						GREATER_THAN,
						1,
						isNegation(queryParameter, GREATER_THAN) ) {
					Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNull(this.propertyName);
                        return Expression.gt( this.propertyName, arguments[0] );
					}

				};
			}
			else if(queryParameter.endsWith( LIKE )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, LIKE),
						LIKE,
						1,
						isNegation(queryParameter, LIKE) ) {
					Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNull(this.propertyName);
                        return Expression.like( this.propertyName, arguments[0] );
					}

				};
			}
            else if(queryParameter.endsWith( ILIKE )) {
                return new GrailsMethodExpression(
                        application,
                        clazz,
                        calcPropertyName(queryParameter, ILIKE),
                        ILIKE,
                        1,
                        isNegation(queryParameter, ILIKE) ) {
                    Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNull(this.propertyName);
                        return Expression.ilike( this.propertyName, arguments[0] );
                    }

                };
            }
			else if(queryParameter.endsWith( IS_NOT_NULL )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, IS_NOT_NULL),
						IS_NOT_NULL,
						0,
						isNegation(queryParameter, IS_NOT_NULL) ) {
					Criterion createCriterion() {
                            return Expression.isNotNull( this.propertyName );
					}

				};
			}
			else if(queryParameter.endsWith( IS_NULL )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, IS_NULL),
						IS_NULL,
						0,
						isNegation(queryParameter, IS_NULL) ) {
					Criterion createCriterion() {
						return Expression.isNull( this.propertyName );
					}

				};
			}
			else if(queryParameter.endsWith( BETWEEN )) {

				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, BETWEEN),
						BETWEEN,
						2,
						isNegation(queryParameter, BETWEEN) ) {
					Criterion createCriterion() {
						return Expression.between( this.propertyName,this.arguments[0], this.arguments[1] );
					}

				};
			}
			else if(queryParameter.endsWith( NOT_EQUAL )) {
				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, NOT_EQUAL),
						NOT_EQUAL,
						1,
						isNegation(queryParameter, NOT_EQUAL) ) {
					Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNotNull(this.propertyName);
                        return Expression.ne( this.propertyName,this.arguments[0]);
					}

				};
			}
			else {

				return new GrailsMethodExpression(
						application,
						clazz,
						calcPropertyName(queryParameter, null),
						EQUAL,
						1,
						isNegation(queryParameter, EQUAL) ) {
					Criterion createCriterion() {
                        if(arguments[0] == null) return Expression.isNull(this.propertyName);
                        return Expression.eq( this.propertyName,this.arguments[0]);
					}
				};
			}
		}
		private static boolean isNegation(String queryParameter, String clause) {
			String propName;
			if(clause != null && !clause.equals( EQUAL )) {
				int i = queryParameter.indexOf(clause);
				propName = queryParameter.substring(0,i);
			}
			else {
				propName = queryParameter;
			}
            return propName.endsWith(NOT);
        }
		private static String calcPropertyName(String queryParameter, String clause) {
			String propName;
			if(clause != null && !clause.equals( EQUAL )) {
				int i = queryParameter.indexOf(clause);
				propName = queryParameter.substring(0,i);
			}
			else {
				propName = queryParameter;
			}
			if(propName.endsWith(NOT)) {
				int i = propName.lastIndexOf(NOT);
				propName = propName.substring(0, i);
			}
			return propName.substring(0,1).toLowerCase(Locale.ENGLISH)
				+ propName.substring(1);
		}
	}

	private final String[] operators;
	private final Pattern[] operatorPatterns;
	protected final GrailsApplication application;

	public AbstractClausedStaticPersistentMethod(GrailsApplication application, SessionFactory sessionFactory, ClassLoader classLoader, Pattern pattern, String[] operators) {
		super(sessionFactory, classLoader, pattern);
		this.application = application;
		this.operators = operators;
		this.operatorPatterns = new Pattern[this.operators.length];
		for (int i = 0; i < operators.length; i++) {
			this.operatorPatterns[i] = Pattern.compile("(\\w+)("+this.operators[i]+")(\\p{Upper})(\\w+)");
		}
	}

	/* (non-Javadoc)
	 * @see org.codehaus.groovy.grails.orm.hibernate.metaclass.AbstractStaticPersistentMethod#doInvokeInternal(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	protected Object doInvokeInternal(final Class clazz, String methodName,
			Object[] arguments) {
		List expressions = new ArrayList();
        if(arguments == null) arguments = new Object[0];
        Matcher match = super.getPattern().matcher( methodName );
		// find match
		match.find();

		String[] queryParameters;
		int totalRequiredArguments = 0;
		// get the sequence clauses
		String querySequence = match.group(2);
		// if it contains operator and split
		boolean containsOperator = false;
        String operatorInUse = null;

        for (int i = 0; i < operators.length; i++) {
			Matcher currentMatcher = operatorPatterns[i].matcher( querySequence );
			if(currentMatcher.find()) {
				containsOperator = true;
				operatorInUse = this.operators[i];

				queryParameters = new String[2];
				queryParameters[0] = currentMatcher.group(1);
				queryParameters[1] = currentMatcher.group(3) + currentMatcher.group(4);

				// loop through query parameters and create expressions
				// calculating the numBer of arguments required for the expression
				int argumentCursor = 0;
				for (int j = 0; j < queryParameters.length; j++) {
					GrailsMethodExpression currentExpression = GrailsMethodExpression.create(this.application,clazz,queryParameters[j]);
					totalRequiredArguments += currentExpression.argumentsRequired;
					// populate the arguments into the GrailsExpression from the argument list
					Object[] currentArguments = new Object[currentExpression.argumentsRequired];
					if((argumentCursor + currentExpression.argumentsRequired) > arguments.length)
						throw new MissingMethodException(methodName,clazz,arguments);

					for (int k = 0; k < currentExpression.argumentsRequired; k++,argumentCursor++) {
						currentArguments[k] = arguments[argumentCursor];
					}
					try {
						currentExpression.setArguments(currentArguments);
					}catch(IllegalArgumentException iae) {
						LOG.debug(iae.getMessage(),iae);
						throw new MissingMethodException(methodName,clazz,arguments);
					}
					// add to list of expressions
					expressions.add(currentExpression);
				}
				break;
			}
		}

		// otherwise there is only one expression
		if(!containsOperator) {
			GrailsMethodExpression solo = GrailsMethodExpression.create(this.application, clazz,querySequence );

			if(solo.argumentsRequired > arguments.length)
				throw new MissingMethodException(methodName,clazz,arguments);

			totalRequiredArguments += solo.argumentsRequired;
			Object[] soloArgs = new Object[solo.argumentsRequired];

            System.arraycopy(arguments, 0, soloArgs, 0, solo.argumentsRequired);
			try {
				solo.setArguments(soloArgs);
			}
			catch(IllegalArgumentException iae) {
				LOG.debug(iae.getMessage(),iae);
				throw new MissingMethodException(methodName,clazz,arguments);
			}
			expressions.add(solo);
		}

		// if the total of all the arguments necessary does not equal the number of arguments
		// throw exception
		if(totalRequiredArguments > arguments.length)
			throw new MissingMethodException(methodName,clazz,arguments);
		
		// calculate the remaining arguments
		Object[] remainingArguments = new Object[arguments.length - totalRequiredArguments];
		if(remainingArguments.length > 0) {		
			for (int i = 0, j = totalRequiredArguments; i < remainingArguments.length; i++,j++) {
				remainingArguments[i] = arguments[j];
			}
		}
		
		if(LOG.isTraceEnabled())
			LOG.trace("Calculated expressions: " + expressions);
		
		return doInvokeInternalWithExpressions(clazz, methodName, remainingArguments, expressions, operatorInUse);
	}
	
	protected abstract Object doInvokeInternalWithExpressions(Class clazz, String methodName, Object[] arguments, List expressions, String operatorInUse);

}
