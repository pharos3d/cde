/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.components;

import pt.webdetails.cdf.dd.model.core.Thing;
import pt.webdetails.cdf.dd.model.core.writer.IThingWriteContext;
import pt.webdetails.cdf.dd.model.core.writer.IThingWriter;
import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cdf.dd.model.core.writer.js.JsWriterAbstract;
import pt.webdetails.cdf.dd.model.inst.CodeComponent;

/**
 * @author dcleao
 */
public class CdfRunJsCodeComponentWriter extends JsWriterAbstract implements IThingWriter {
  public void write( Object output, IThingWriteContext context, Thing t ) throws ThingWriteException {
    this.write( (StringBuilder) output, (IThingWriteContext) context, (CodeComponent) t );
  }

  public void write( StringBuilder out, IThingWriteContext context, CodeComponent comp ) throws ThingWriteException {
    out.append( comp.tryGetPropertyValue( "javaScript", "" ) );
    out.append( NEWLINE );
  }
}
