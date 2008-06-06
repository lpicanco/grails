package org.codehaus.groovy.grails.web.pages;

/**
 * NOTE: Based on work done by on the GSP standalone project (https://gsp.dev.java.net/)
 *
 * Interface defining an enumeration of tokens for the different scriptlet types
 *
 * @author Troy Heninger
 * @author Graeme Rocher
 *
 * Date: Jan 10, 2004
 *
 */
interface Tokens {
	static final int EOF = -1;
	static final int HTML = 0;
	static final int JEXPR = 1;   // <%= ... %>
	static final int JSCRIPT = 2; // <% .... %>
	static final int JDIRECT = 3; // <%@ ... %>
	static final int JDECLAR = 4; // <%! ... %>
	static final int GEXPR = 11;   // ${ ... }
	static final int GSCRIPT = 12; // %{ ... }%
	static final int GDIRECT = 13; // @{ ... }
	static final int GDECLAR = 14; // !{ ... }!
    static final int GSTART_TAG = 15; // <g:..>
    static final int GEND_TAG = 16; // </g:..>
    static final int GTAG_EXPR = 17; // ${..}
}
