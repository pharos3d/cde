/*!
 * Copyright 2002 - 2015 Webdetails, a Pentaho company.  All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to  http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

package pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.components.amd;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cdf.dd.model.inst.GenericComponent;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteContext;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteOptions;
import pt.webdetails.cdf.dd.model.meta.*;

import static org.mockito.Mockito.when;

public class CdfRunJsGenericComponentWriterTest extends TestCase {

  private static final String NEWLINE = System.getProperty( "line.separator" );
  private static final String INDENT = "  ";

  private static CdfRunJsGenericComponentWriter genericComponentWriter;
  private static CdfRunJsDashboardWriteContext context;
  private static CdfRunJsDashboardWriteOptions options;
  private static GenericComponent genericComponent;
  private static GenericComponentType genericComponentType;

  @Before
  public void setUp() throws Exception {
    genericComponentWriter = new CdfRunJsGenericComponentWriter();
    context = Mockito.mock( CdfRunJsDashboardWriteContext.class );
    options = Mockito.mock( CdfRunJsDashboardWriteOptions.class );
    genericComponent = Mockito.mock( GenericComponent.class );
    genericComponentType = Mockito.mock( GenericComponentType.class );
  }

  @After
  public void tearDown() throws Exception {
    genericComponentWriter = null;
    context = null;
    options = null;
    genericComponent = null;
    genericComponentType = null;
  }

  @Test
  public void testGenericComponentWrite() {

    StringBuilder out = new StringBuilder();

    when( genericComponent.getMeta() ).thenReturn( genericComponentType );
    when( genericComponent.getId() ).thenReturn( "test" );

    when( options.getAliasPrefix() ).thenReturn( "" );
    when( context.getOptions() ).thenReturn( options );

    try {

      genericComponentWriter.write( out, context, genericComponent, "TestComponent" );

    } catch ( ThingWriteException e ) {
      e.printStackTrace();
    }

    StringBuilder expectedReturnValue = new StringBuilder();
    expectedReturnValue.append( "var test = new TestComponent({" ).append( NEWLINE )
        .append( INDENT ).append( "type: \"TestComponent\"," ).append( NEWLINE )
        .append( INDENT ).append( "name: \"test\"" ).append( NEWLINE )
        .append( "});" ).append( NEWLINE );

    Assert.assertEquals( out.toString(), expectedReturnValue.toString() );

  }

}
