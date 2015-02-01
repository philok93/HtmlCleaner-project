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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Before;

import static org.junit.Assert.assertEquals;


/**
 * Abstract test class with utility methods
 */
public abstract class AbstractHtmlCleanerTest {
	
	protected HtmlCleaner cleaner;
	protected Serializer serializer;
	
	@Before
	public void setup(){
        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(true);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setAdvancedXmlEscape(true);
        cleanerProperties.setTranslateSpecialEntities(false);
        cleanerProperties.setOmitComments(false);
        cleanerProperties.setIgnoreQuestAndExclam(false);

        cleaner = new HtmlCleaner(cleanerProperties);
        serializer = new SimpleXmlSerializer(cleanerProperties);	
	}

	protected void assertCleaned(String initial, String expected) throws IOException {
        TagNode node = cleaner.clean(initial);
        StringWriter writer = new StringWriter();
        serializer.write(node, writer, "UTF-8");
        assertEquals(expected, writer.toString());
	}

	protected String readFile(String filename) throws IOException {
		File file = new File(filename);
		CharSequence content = Utils.readUrl(file.toURI().toURL(), "UTF-8");
		return content.toString();
	}
	
	public static final String HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"; 
		    //+ "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            //+ "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
    private static final String HEADER_FULL = HEADER + "<html><head /><body>";
    private static final String FOOTER = "</body></html>";

    protected void assertHTML(String expected, String input) throws IOException {
        StringWriter writer = new StringWriter();
        serializer.write(cleaner.clean(input), writer, "UTF-8");
    	String actual = writer.toString();
    	
        Assert.assertEquals(HEADER_FULL + expected + FOOTER, actual);
    }

}
