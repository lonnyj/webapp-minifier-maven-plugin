package com.github.webapp_minifier.options;

import static junitparams.JUnitParamsRunner.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * This class tests {@link OptionsParser}.
 *
 * @author Lonny
 */
@RunWith(JUnitParamsRunner.class)
public class OptionsParserTest {
   private OptionsParser parser;

   /**
    * Creates the instance to be tested.
    */
   @Before
   public void beforeTest() {
      Log log = new SilentLog();
      log = new SystemStreamLog();
      this.parser = new OptionsParser(log);
   }

   /**
    * Tests {@link OptionsParser#containsOptionsHeader(String)}.
    *
    * @param text
    *           the text to test.
    * @param expected
    *           the expected result.
    */
   @Test
   @Parameters(method = "containsOptionsHeaderTestData")
   public void testContainsOptionsHeader(final String text, final boolean expected) {
      final boolean actual = this.parser.containsOptionsHeader(text);
      assertEquals(expected, actual);
   }

   /**
    * Provides the test data for {@link #testContainsOptionsHeader(String, boolean)}.
    *
    * @return the test data.
    */
   public Object containsOptionsHeaderTestData() {
      return new Object[][] { $(null, false), new Object[] { "", false },
            new Object[] { OptionsParser.OPTION_HEADER.substring(1), false },
            new Object[] { OptionsParser.OPTION_HEADER, true },
            new Object[] { ' ' + OptionsParser.OPTION_HEADER, true },
            new Object[] { 'x' + OptionsParser.OPTION_HEADER, true }, };
   }

   /**
    * Tests {@link OptionsParser#containsOptionsHeader(String)}.
    *
    * @param text
    *           the text to test.
    * @param properties
    *           the property being tested.
    * @param expects
    *           the expected results.
    * @throws Exception
    *            if the test fails.
    */
   @Test
   @Parameters(method = "parseTestData")
   public void testParse(final String text, final Object properties, final Object expects)
         throws Exception {
      if (properties == null) {
         final InlineConfigurationHandler inlineConfigHandler = mock(InlineConfigurationHandler.class);
         final DirectiveHandler directiveHandler = mock(DirectiveHandler.class);
         this.parser.parse(text, inlineConfigHandler, directiveHandler);
         if (text.contains("split-css")) {
            verify(directiveHandler).splitCss();
         } else if (text.contains("split-javascript")) {
            verify(directiveHandler).splitJavaScript();
         } else {
            verifyZeroInteractions(inlineConfigHandler);
         }
      } else {
         final OverridablePluginOptions options = new DefaultOverridablePluginOptions();
         final DefaultInlineConfigurationHandler inlineConfigHandler = new DefaultInlineConfigurationHandler(
               options);
         final DirectiveHandler directiveHandler = mock(DirectiveHandler.class);
         this.parser.parse(text, inlineConfigHandler, directiveHandler);
         verify(directiveHandler, never()).splitCss();
         verify(directiveHandler, never()).splitJavaScript();
         if (properties.getClass().isArray()) {
            final String[] keys = (String[]) properties;
            assertEquals(keys.length, ((Object[]) expects).length);
            for (int ii = 0; ii < keys.length; ii++) {
               final String key = keys[ii];
               final Object expect = ((Object[]) expects)[ii];
               final Object actual = PropertyUtils.getProperty(options, key);
               assertEquals(expect.getClass(), actual.getClass());
               assertEquals(properties + " should match", expect, actual);
            }
         } else {
            final String key = (String) properties;
            final Object actual = PropertyUtils.getProperty(options, key);
            assertNotNull(actual);
            assertEquals(expects.getClass(), actual.getClass());
            assertEquals(properties + " should match", expects, actual);
         }
      }
   }

   /**
    * Provides the test data for {@link #testContainsOptionsHeader(String, boolean)}.
    *
    * @return the test data.
    */
   public Object parseTestData() {
      return $(
            $("", null, null),
            generateParsePropertyTestCase("closureCompilationLevel",
                  CompilationLevel.SIMPLE_OPTIMIZATIONS),
                  generateParsePropertyTestCase("jsCompressorEngine", JavaScriptCompressor.CLOSURE),
                  generateParsePropertyTestCase("yuiCssLineBreak", 38),
                  generateParsePropertyTestCase("skipCssMinify", true),
                  generateParseDirectiveTestCase("split-javascript"),
                  generateParseDirectiveTestCase("split-css"));
   }

   /**
    * Generates a single test case for {@link #parseTestData()}.
    *
    * @param property
    *           the property to test.
    * @param value
    *           the property's value.
    * @return the test case.
    */
   private Object[] generateParsePropertyTestCase(final String property, final Object value) {
      return $(OptionsParser.OPTION_HEADER + ' ' + property + '=' + value, property, value);
   }

   /**
    * Generates a single test case for {@link #parseTestData()}.
    *
    * @param property
    *           the property to test.
    * @param value
    *           the property's value.
    * @return the test case.
    */
   private Object[] generateParseDirectiveTestCase(final String directive) {
      return $(OptionsParser.OPTION_HEADER + ' ' + directive, null, null);
   }

   /**
    * Tests {@link OptionsParser#containsOptionsHeader(String)}.
    *
    * @param text
    *           the text to test.
    * @throws Exception
    *            if the test fails.
    */
   @Test(expected = ParseOptionException.class)
   @Parameters(method = "parseExceptionTestData")
   public void testParseException(final String text, final String property) throws Exception {
      final OverridablePluginOptions options = new DefaultOverridablePluginOptions();
      final DefaultInlineConfigurationHandler configHandler = new DefaultInlineConfigurationHandler(
            options);
      final DirectiveHandler directiveHandler = mock(DirectiveHandler.class);
      verifyNoMoreInteractions(directiveHandler);
      try {
         this.parser.parse(text, configHandler, directiveHandler);
      } catch (final RuntimeException e) {
         if (e.getCause() instanceof ParseOptionException) {
            throw (ParseOptionException) e.getCause();
         }
         throw e;
      }
      final Object actual = PropertyUtils.getProperty(options, property);
      fail("An exception should have been thrown.  The value of " + property + " was " + actual);
   }

   /**
    * Provides the test data for {@link #testContainsOptionsHeader(String, boolean)}.
    *
    * @return the test data.
    */
   public Object parseExceptionTestData() {
      return $(generateParseExceptionTestCase("closureCompilationLevel", "xSIMPLE_OPTIMIZATIONS"),
            generateParseExceptionTestCase("jsCompressorEngine", "xCLOSURE"),
            generateParseExceptionTestCase("jsCompressorEngine", "split-javascriptx"));
   }

   /**
    * Generates a single test case for {@link #parseTestData()}.
    *
    * @param property
    *           the property to test.
    * @param value
    *           the property's value.
    * @return the test case.
    */
   private Object[] generateParseExceptionTestCase(final String property, final Object value) {
      return $(OptionsParser.OPTION_HEADER + '\n' + property + '=' + value, property);
   }
}
