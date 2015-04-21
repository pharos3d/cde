/*!
* Copyright 2002 - 2014 Webdetails, a Pentaho company.  All rights reserved.
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

package pt.webdetails.cdf.dd;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;

import pt.webdetails.cdf.dd.api.RenderApi;
import pt.webdetails.cdf.dd.api.ResourcesApi;
import pt.webdetails.cdf.dd.util.CdeEnvironment;
import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.audit.CpfAuditHelper;
import pt.webdetails.cpf.utils.MimeTypes;

import java.util.UUID;


public class DashboardDesignerContentGenerator extends SimpleContentGenerator {
  private static final Log logger = LogFactory.getLog( DashboardDesignerContentGenerator.class );

  private boolean edit = false;
  private boolean create = false;
  private boolean resource = false;

  public DashboardDesignerContentGenerator() {
    super();
  }

  @Override
  public Log getLogger() {
    return logger;
  }

  @Override
  public void createContent() throws Exception {
    IParameterProvider requestParams = parameterProviders.get( MethodParams.REQUEST );
    IParameterProvider pathParams = parameterProviders.get( MethodParams.PATH );

    String solution = getRequestParameterAsString( MethodParams.SOLUTION, "" ),
      path = getRequestParameterAsString( MethodParams.PATH, "" ),
      file = getRequestParameterAsString( MethodParams.FILE, "" ),
      root = getRequestParameterAsString( MethodParams.ROOT, "" ),
      scheme = getRequestParameterAsString( MethodParams.SCHEME, "" );

    String viewId = getRequestParameterAsString( MethodParams.VIEWID, "" );

    String filePath = getPathParameterAsString( MethodParams.PATH, "" );

    String auditPath = filePath.length() > 0 ? filePath : "newDashboard";

    boolean inferScheme = requestParams.hasParameter( MethodParams.INFER_SCHEME )
      && getRequestParameterAsString( MethodParams.INFER_SCHEME, "" ).equals( "false" );
    boolean absolute = requestParams.hasParameter( MethodParams.ABSOLUTE )
      && getRequestParameterAsString( MethodParams.ABSOLUTE, "" ).equals( "true" );
    boolean bypassCacheRead = requestParams.hasParameter( MethodParams.BYPASS_CACHE )
      && getRequestParameterAsString( MethodParams.BYPASS_CACHE, "" ).equals( "true" );
    boolean debug = requestParams.hasParameter( MethodParams.DEBUG )
      && getRequestParameterAsString( MethodParams.DEBUG, "" ).equals( "true" );

    String style = getRequestParameterAsString( MethodParams.STYLE, "" );

    RenderApi renderer = new RenderApi();

    long start = System.currentTimeMillis();
    UUID uuid = CpfAuditHelper.startAudit( getPluginName(), auditPath, getObjectName(),
      this.userSession, this, requestParams );

    if ( create ) {
      String result = renderer.newDashboard( filePath, debug, true, getRequest(), getResponse() );
      IOUtils.write( result, getResponse().getOutputStream() );
    } else if ( edit ) {
      //TODO: file to path
      String result = renderer.edit( "", "", filePath, debug, true, getRequest(), getResponse() );
      IOUtils.write( result, getResponse().getOutputStream() );

    } else if ( resource ) {
      // TODO review later if there is a viable solution to making resources being
      // called via cde resources rest api (pentaho/plugin/pentaho-cdf-dd/api/resources?resource=)
      // this has to take into consideration:
      // 1 - token replacement (see cde-core#CdfRunJsDashboardWriteContext.replaceTokens())
      // 2 - resources being called from other resources (ex: resource plugin-samples/template.css calls resource
      // images/button-contact-png)

      new ResourcesApi().getResource( pathParams.getStringParameter( MethodParams.COMMAND, "" ), getResponse() );

    } else {
      String result = renderer.render( "", "", filePath, inferScheme, root, absolute, bypassCacheRead, debug, scheme,
        viewId, style, getRequest() );
      getResponse().setContentType( MimeTypes.HTML );

      IOUtils.write( result, getResponse().getOutputStream() );
      getResponse().getOutputStream().flush();
    }

    long end = System.currentTimeMillis();
    CpfAuditHelper.endAudit( getPluginName(), auditPath, getObjectName(), this.userSession,
      this, start, uuid, end );
  }

  @Override
  public String getPluginName() {
    return CdeEnvironment.getPluginId();
  }

  public String getObjectName() {
    return this.getClass().getName();
  }

  private class MethodParams {
    public static final String DEBUG = "debug";
    public static final String BYPASS_CACHE = "bypassCache";
    public static final String ROOT = "root";
    public static final String INFER_SCHEME = "inferScheme";
    public static final String ABSOLUTE = "absolute";
    public static final String SOLUTION = "solution";
    public static final String PATH = "path";
    public static final String FILE = "file";
    public static final String REQUEST = "request";
    public static final String VIEWID = "viewId";
    public static final String COMMAND = "cmd";
    public static final String STYLE = "style";
    public static final String SCHEME = "scheme";


    public static final String DATA = "data";
  }

  public boolean isEdit() {
    return edit;
  }

  public void setEdit( boolean edit ) {
    this.edit = edit;
  }

  public boolean isCreate() {
    return create;
  }

  public void setCreate( boolean create ) {
    this.create = create;
  }

  public boolean isResource() {
    return resource;
  }

  public void setResource( boolean resource ) {
    this.resource = resource;
  }

  public static String getPluginDir() {
    return CdeEnvironment.getSystemDir() + "/" + CdeEnvironment.getPluginId() + "/";
  }
}
