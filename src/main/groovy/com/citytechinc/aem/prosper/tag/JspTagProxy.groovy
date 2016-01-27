package com.citytechinc.aem.prosper.tag

import groovy.transform.TupleConstructor

import javax.servlet.jsp.PageContext
import javax.servlet.jsp.tagext.TagSupport

/**
 * Composite class containing the JSP tag instance and mocked page context that exposes the tag writer output.
 */
@TupleConstructor
class JspTagProxy {

    /**
     * Tag instance under test.
     */
    TagSupport tag

    /**
     * Mock page context for tag under test.
     */
    PageContext pageContext

    /**
     * Writer for capturing tag output.
     */
    Writer writer

    /**
     * Get the output value for the JSP writer.
     *
     * @return output string
     */
    final String getOutput() {
        writer.toString()
    }
}