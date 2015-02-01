package org.htmlcleaner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class HtmlCleanerTest extends AbstractHtmlCleanerTest {
	
	/**
	 * This is to test issue #93
	 */
	@Test
	public void closingDiv(){
		//
		// Check that when a tag is self-closing, we close it and start again rather than
		// let it remain open and enclose the following tags
		//
		String initial = "<div id=\"y\"/><div id=\"z\">something</div>";
		String expected = "<html>\n<head />\n<body><div id=\"y\"></div><div id=\"z\">something</div></body></html>";
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);
        
        //
        // This should also result in the same output
        //
        initial = "<div id=\"y\"></div><div id=\"z\">something</div>";
        cleaned = cleaner.clean(initial);
        output = serializer.getAsString(cleaned);
        assertEquals(expected, output);
	}


    /**
     * This is to test issue #67
     */
    @Test
    public void testXmlNoExtraWhitesapce(){
        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(true);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
 
    	HtmlCleaner theCleaner = new HtmlCleaner(cleanerProperties);

        String initial = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html><head /><body><p>test</p></body></html>\n";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html><head /><body><p>test</p></body></html>";

        TagNode cleaned = theCleaner.clean(initial);
                
        Serializer theSerializer = new SimpleXmlSerializer(theCleaner.getProperties());
        String output = theSerializer.getAsString(cleaned);
        assertEquals(expected, output);
    }
    
    /**
     * Test for #2901.
     */
    @Test
	public void testWhitespaceInHead() throws IOException {
		String initial = readFile("src/test/resources/Real_1.html");
		String expected = readFile("src/test/resources/Expected_1.html");
		assertCleaned(initial, expected);
	}

	/**
	 * Mentioned in #2901 - we should eliminate the first <tr>
	 * TODO: Passes but not with ideal result.
	 */
    @Test
	public void testUselessTr() throws IOException {
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		String start = "<html><head /><body><table>";
		String end = "</body></html>";
		assertCleaned(start + "<tr><tr><td>stuff</td></tr>" + end,
				//start+"<tbody><tr><td>stuff</td></tr></tbody></table>" + end // "ideal" output
				start + "<tbody><tr /><tr><td>stuff</td></tr><tr></tr></tbody></table>" + end // actual
		);
	}

	/**
	 * Collapsing empty tr to <tr />
	 */
    @Test
	public void testUselessTr2() throws IOException {
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		String start = "<html><head /><body><table>";
		String end = "</table></body></html>";
		assertCleaned(start + "<tr> </tr><tr><td>stuff</td></tr>" + end,
				start + "<tbody><tr /><tr><td>stuff</td></tr></tbody>" + end);
	}

	/**
	 * For #2940
	 */
    @Test
	public void testCData() throws IOException {
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		String start = "<html><head>";
		String end = "</head><body>1</body></html>";
		assertCleaned(start + "<style type=\"text/css\">/*<![CDATA[*/\n#ampmep_188 { }\n/*]]>*/</style>" + end,
				start + "<style type=\"text/css\">/*<![CDATA[*/\n#ampmep_188 { }\n/*]]>*/</style>" + end);
	}

	/**
	 * Report in issue #64 as causing issues.
	 * @throws Exception
	 */
    @Test
	public void testChineseParsing() throws Exception {
	    String initial = readFile("src/test/resources/test-chinese-issue-64.html");
	    TagNode node = cleaner.clean(initial);
	    final TagNode[] imgNodes = node.getElementsByName("img", true);
	    assertEquals(5, imgNodes.length);
	}
    
    /**
     * Report in issue #70 as causing issues.
     * @throws Exception
     */
    @Test
    public void testOOME_70() throws Exception {
        String initial = readFile("src/test/resources/oome_70.html");
        TagNode node = cleaner.clean(initial);
        final TagNode[] imgNodes = node.getElementsByName("img", true);
        assertEquals(17, imgNodes.length);
    }

    @Test
    public void testOOME_59() throws Exception {
        String in = "<html><body><table><fieldset><legend>";
        CleanerProperties cp = new CleanerProperties();
        cp.setOmitUnknownTags(true);
        HtmlCleaner c = new HtmlCleaner(cp);
        TagNode root = c.clean(in);
        assertEquals(1, root.getElementsByName("legend", true).length);
    }
    
    /**
     * Check that we no longer require block-level restrictions for anchors, as per HTML5. See issue #82
     * @throws IOException
     */
	@Test
	public void noAnchorBlockLevelRestriction() throws IOException{
        
		String initial = readFile("src/test/resources/test24.html");
		String expected = readFile("src/test/resources/test24_expected.html"); 
		
		assertCleaned(initial,expected);
	}
}
