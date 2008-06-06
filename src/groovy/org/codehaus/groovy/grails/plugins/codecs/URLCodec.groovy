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

package org.codehaus.groovy.grails.plugins.codecs

import java.net.URLEncoder
import java.net.URLDecoder
import org.springframework.web.context.request.RequestContextHolder

/**
 * A code that encodes and decodes Objects to and from URL encoded strings
 *
 * @author Marc Palmer
 * @since 0.5
 */
class URLCodec {
    static encode = { obj ->
        URLEncoder.encode(obj.toString(), URLCodec.getEncoding())
    }

    static decode = { obj ->
        URLDecoder.decode(obj.toString(), URLCodec.getEncoding())
    }

	private static def getEncoding() {
		def request = RequestContextHolder.getRequestAttributes()?.request
		def encoding = "UTF-8"
		if (request?.characterEncoding) {
			encoding = request?.characterEncoding
		}
		return encoding
	}
}
