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

import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cdf.dd.model.inst.ParameterComponent;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteContext;
import pt.webdetails.cdf.dd.util.JsonUtils;

public class CdfRunJsExpressionParameterComponentWriter extends CdfRunJsParameterComponentWriter {
  @Override
  public void write( StringBuilder out, CdfRunJsDashboardWriteContext context, ParameterComponent comp )
    throws ThingWriteException {
    String name = JsonUtils.toJsString( context.getId( comp ) );
    String value = sanitizeExpression( comp.tryGetPropertyValue( "javaScript", "" ) );
    Boolean isBookmarkable = "true".equalsIgnoreCase( comp.tryGetPropertyValue( "bookmarkable", null ) );

    addSetParameterAssignment( out, name, value );
    if ( isBookmarkable ) {
      addBookmarkable( out, name );
    }
  }

  protected static String sanitizeExpression( String expr ) {
    expr = expr.replaceAll( "[;\\s]+$", "" );
    if ( expr.startsWith( "function" ) ) {
      return expr;
    } else {
      return "function() { return " + expr + NEWLINE + "}()";
    }
  }
}
