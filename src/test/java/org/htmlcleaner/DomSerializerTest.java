/*  Copyright (c) 2006-2013, the HtmlCleaner Project
    All rights reserved.

    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of HtmlCleaner may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/

package org.htmlcleaner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class DomSerializerTest extends AbstractHtmlCleanerTest {

    @Test
    public void testCDATA() throws Exception
    {
    	String testData = ""
    	+ "<script type=\"text/javascript\">\n"
    	+ "//<![CDATA[\n"
    	+ "function escapeForXML(origtext) {\n"
    	+ " return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
    	+ " .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
    	+ "}\n"
    	+ "//]]>"
    	+ "</script>";
    	
    	TagNode cleaned = cleaner.clean(testData);
    	    	
    	DomSerializer ser = new DomSerializer(cleaner.getProperties());
    	Document output = ser.createDOM(cleaned);
    	
    	// We should have a script tag

    	assertEquals("script",         output.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().item(0).getNodeName());

    	// The content of the script tag should be CDATA
    	assertEquals("#cdata-section", output.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(2).getNodeName());
    	
    	// The value of the CDATA section should be as expected from the input
    	String content = output.getChildNodes().item(0).getChildNodes().item(1).getChildNodes().item(0).getChildNodes().item(2).getNodeValue();
    	assertEquals("\nfunction escapeForXML(origtext) {\n return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n .replace(/>/g,'&'+'gt;').replace(/'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');}\n", content);    	
    }
    
	/**
	 * See issue 108
	 * @throws IOException
	 */
    @Test
    @Ignore
    public void html5doctype() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = readFile("src/test/resources/test23.html");
    	TagNode tagNode = cleaner.clean(initial);
    	DomSerializer ser = new DomSerializer(cleaner.getProperties());
    	Document dom = ser.createDOM(tagNode);
    	assertNotNull(dom.getChildNodes().item(0).getChildNodes().item(0));
    	assertEquals("head", dom.getChildNodes().item(0).getChildNodes().item(0).getNodeName());
    }
    
	/**
	 * See issue 127
	 * @throws IOException
	 */
    @Test
    public void rootNodeAttributes() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = readFile("src/test/resources/test29.html");
    	TagNode tagNode = cleaner.clean(initial);
    	DomSerializer ser = new DomSerializer(cleaner.getProperties());
    	Document dom = ser.createDOM(tagNode);
    	assertNotNull(dom.getChildNodes().item(0).getChildNodes().item(0));
    	assertEquals("http://unknown.namespace.com", dom.getChildNodes().item(0).getAttributes().getNamedItem("xmlns").getNodeValue());
    	assertEquals("27", dom.getChildNodes().item(0).getAttributes().getNamedItem("id").getNodeValue());
    	//
    	// Check we have a real ID attribute in the DOM and not just a regular attribute
    	//
    	assertEquals("http://unknown.namespace.com", dom.getElementById("27").getAttribute("xmlns"));
    }

  
	
}
