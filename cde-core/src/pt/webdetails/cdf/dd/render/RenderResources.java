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

package pt.webdetails.cdf.dd.render;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import pt.webdetails.cdf.dd.model.inst.writer.cdfrunjs.dashboard.CdfRunJsDashboardWriteContext;
import pt.webdetails.cdf.dd.render.layout.Render;
import pt.webdetails.cdf.dd.render.layout.ResourceCodeRender;
import pt.webdetails.cdf.dd.util.XPathUtils;

import java.text.MessageFormat;
import java.util.Iterator;

public class RenderResources extends Renderer {
  private static final String RESOURCES = " and (type='LayoutResourceFile' or type='LayoutResourceCode')";

  private static final String PROPERTY_RESOURCE_NAME = "properties[name='name']/value";
  private static final String PROPERTY_RESOURCE_TYPE = "properties[name='resourceType']/value";
  private static final String PROPERTY_RESOURCE_CODE_CONTENT = "properties/value[../name='resourceCode']";
  private static final String PROPERTY_RESOURCE_PATH = "properties/value[../name='resourceFile']";

  public RenderResources( JXPathContext doc, CdfRunJsDashboardWriteContext context ) {
    super( doc, context );
  }

  @Override
  public String render( final String alias ) throws Exception {
    return "";
  }

  public ResourceMap renderResources( final String alias ) throws Exception {
    final Iterator resourceRows = getResourcesRows();

    ResourceMap resources = new ResourceMap();

    while ( resourceRows.hasNext() ) {
      JXPathContext context = getRelativeContext( (Pointer) resourceRows.next() );
      String rowName = getResourceName( context ),
        rowKind = getResourceType( context );

      String type = (String) context.getValue( "type" );
      String processedResource = processResource( context, alias, 4 ),
        resourcePath = getResourcePath( context );

      ResourceMap.ResourceKind resourceKind = rowKind.equals( "Javascript" ) ?
        ResourceMap.ResourceKind.JAVASCRIPT : ResourceMap.ResourceKind.CSS;

      ResourceMap.ResourceType resourceType = type.equals( "LayoutResourceFile" ) ?
        ResourceMap.ResourceType.FILE : ResourceMap.ResourceType.CODE;

      resources.add( resourceKind, resourceType, rowName, resourcePath, processedResource );
    }

    return resources;
  }

  protected Iterator getResourcesRows() {
    return doc.iteratePointers( MessageFormat.format( XPATH_FILTER, UNIQUEID, RESOURCES ) );
  }

  protected JXPathContext getRelativeContext( Pointer p ) {
    return doc.getRelativeContext( p );
  }

  protected String processResource( JXPathContext context, final String alias, final int indent ) throws Exception {
    StringBuffer buffer = new StringBuffer();

    Render renderer = (Render) getRender( context );
    renderer.processProperties();
    renderer.aliasId( alias );
    buffer.append( NEWLINE ).append( getIndent( indent ) );
    if ( getContext().getDashboard().getWcdf().isRequire() ) {
      String resourceType = getResourceType( context );
      if ( renderer instanceof ResourceCodeRender && resourceType.equals( "Javascript" ) ) {
        buffer.append( getResourceCodeContent( context ) );
      } else {
        buffer.append( renderer.renderStart() );
      }
      return buffer.toString();
    } else {
      buffer.append( renderer.renderStart() );
      return buffer.toString();
    }
  }

  protected String getResourceName( JXPathContext context ) throws Exception {
    return XPathUtils.getStringValue( context, PROPERTY_RESOURCE_NAME );
  }

  protected String getResourcePath( JXPathContext context ) throws Exception {
    return XPathUtils.getStringValue( context, PROPERTY_RESOURCE_PATH );
  }

  protected String getResourceType( JXPathContext context ) throws Exception {
    return XPathUtils.getStringValue( context, PROPERTY_RESOURCE_TYPE );
  }

  protected String getResourceCodeContent( JXPathContext context ) throws Exception {
    return XPathUtils.getStringValue( context, PROPERTY_RESOURCE_CODE_CONTENT );
  }

  @Override
  protected String getRenderClassName( String type ) {
    return "pt.webdetails.cdf.dd.render.layout." + type.replace( "Layout", "" ) + "Render";
  }
}
