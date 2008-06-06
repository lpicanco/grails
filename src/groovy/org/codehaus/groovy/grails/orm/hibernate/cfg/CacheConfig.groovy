/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.cfg
/**
 * <p> A class that defines the cache configuration
 
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Sep 27, 2007
 */

class CacheConfig {
    static final USAGE_OPTIONS = ['read-only', 'read-write','nonstrict-read-write','transactional']
    static final INCLUDE_OPTIONS = ['all', 'non-lazy']

    String usage = "read-write"
    boolean enabled = false
    String include = "all"
}