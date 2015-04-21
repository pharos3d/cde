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

package pt.webdetails.cdf.dd.api;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import pt.webdetails.cdf.dd.CdeEngineForTests;
import pt.webdetails.cdf.dd.CdeEnvironmentForTests;
import pt.webdetails.cdf.dd.IPluginResourceLocationManager;
import pt.webdetails.cdf.dd.datasources.IDataSourceManager;
import pt.webdetails.cdf.dd.datasources.IDataSourceProvider;
import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cpf.context.api.IUrlProvider;
import pt.webdetails.cpf.repository.api.FileAccess;
import pt.webdetails.cpf.repository.api.IBasicFile;
import pt.webdetails.cpf.repository.api.IBasicFileFilter;
import pt.webdetails.cpf.repository.api.IReadAccess;
import pt.webdetails.cpf.repository.api.IUserContentAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class RenderApiTest {

  private static RenderApi renderApi;
  private static final String USER_DIR = System.getProperty( "user.dir" );
  private static final String TEST_RESOURCES = USER_DIR + File.separator + "test-resources";
  private static final String DUMMY_WCDF =
      TEST_RESOURCES + File.separator + "dummyDashboard" + File.separator + "dummy.wcdf";
  private static final String PROPERTY_NAME =
      TEST_RESOURCES + File.separator + "resources" + File.separator + "base" + File.separator + "properties"
      + File.separator + "Name.xml";
  private static final String STYLE_CLEAN =
      TEST_RESOURCES + File.separator + "resources" + File.separator + "styles" + File.separator + "Clean.html";
  private static final String DEFAULT_ROOT = "http://localhost:8080/";

  @BeforeClass
  public static void setUp() throws Exception {

    final File propertyName = new File( PROPERTY_NAME );
    List<IBasicFile> baseProperties = new ArrayList<IBasicFile>();
    baseProperties.add( getBasicFileFromFile( propertyName ) );

    //mock IUserContentAccess
    IUserContentAccess mockedUserContentAccess = mock( IUserContentAccess.class );
    when( mockedUserContentAccess.fileExists( anyString() ) ).thenReturn( true );
    when( mockedUserContentAccess.fetchFile( anyString() ) )
        .thenAnswer( new Answer<IBasicFile>() {
          @Override
        public IBasicFile answer( InvocationOnMock invocationOnMock ) throws Throwable {
              File file = new File( (String) invocationOnMock.getArguments()[ 0 ] );
            return getBasicFileFromFile( file );
          }
        } );
    when( mockedUserContentAccess.hasAccess( anyString(), any( FileAccess.class ) ) )
      .thenReturn( true );
    when( mockedUserContentAccess.getFileInputStream( anyString() ) )
        .thenAnswer( new Answer<InputStream>() {
          @Override
        public InputStream answer( InvocationOnMock invocationOnMock ) throws Throwable {
            return getInputStreamFromFileName( (String) invocationOnMock.getArguments()[ 0 ] );
          }
        } );

    //mock IReadAccess
    IReadAccess mockedReadAccess = mock( IReadAccess.class );
    when( mockedReadAccess.listFiles( anyString(), any( IBasicFileFilter.class ), anyInt() ) )
      .thenReturn( baseProperties );
    when( mockedReadAccess.getFileInputStream( anyString() ) ).thenAnswer( new Answer<InputStream>() {
      @Override
      public InputStream answer( InvocationOnMock invocationOnMock ) throws Throwable {
        return getInputStreamFromFileName( (String) invocationOnMock.getArguments()[ 0 ] );
      }
    } );

    //mock IPluginResourceLocationManager
    IPluginResourceLocationManager mockedPluginResourceLocationManager =
        mock( IPluginResourceLocationManager.class );
    when( mockedPluginResourceLocationManager.getStyleResourceLocation( anyString() ) )
      .thenReturn( STYLE_CLEAN );

    //mock IDataSourceManager
    IDataSourceManager mockedDataSourceManager = mock( IDataSourceManager.class );
    when( mockedDataSourceManager.getProviders() ).thenReturn( new ArrayList<IDataSourceProvider>() );

    //mock IUrlProvider
    IUrlProvider mockedUrlProvider = mock( IUrlProvider.class );
    when( mockedUrlProvider.getWebappContextRoot() ).thenReturn( DEFAULT_ROOT );

    CdeEnvironmentForTests cdeEnvironmentForTests = new CdeEnvironmentForTests();
    cdeEnvironmentForTests.setMockedContentAccess( mockedUserContentAccess );
    cdeEnvironmentForTests.setMockedReadAccess( mockedReadAccess );
    cdeEnvironmentForTests.setMockedPluginResourceLocationManager( mockedPluginResourceLocationManager );
    cdeEnvironmentForTests.setMockedDataSourceManager( mockedDataSourceManager );
    cdeEnvironmentForTests.setMockedUrlProvider( mockedUrlProvider );

    renderApi = new RenderApiForTesting( cdeEnvironmentForTests );
    new CdeEngineForTests( cdeEnvironmentForTests );

  }

  @Test
  public void testGetHeaders() throws IOException, ThingWriteException {
    //case1 -> absolute=true&root=(empty)
    String case1 =
        doCase( "", true, "http" );

    //case2 -> absolute=false&root=localhost:8080
    String case2 =
        doCase( "testRoot", false, "http" );

    //case3 -> absolute=true&root=localhost:8080
    String case3 =
        doCase( "testRoot", true, "http" );

    //case4 -> absolute=false&root=(empty)
    String case4 =
        doCase( "", false, "http" );

    //case5 -> absolute=true&root=localhost:8080&scheme=https
    String case5 =
        doCase( "testRoot", true, "https" );

    Assert.assertTrue( case1.contains( "http://localhost:8080/js/CDF.js" )
        && case1.contains( "http://localhost:8080/css/CDF-CSS.css" ) );
    Assert.assertTrue( case2.contains( "/js/CDF.js" ) && case2.contains( "/css/CDF-CSS.css" ) );
    Assert.assertTrue( case3.contains( "http://testRoot/js/CDF.js" )
        && case3.contains( "http://testRoot/css/CDF-CSS.css" ) );
    Assert.assertTrue( case4.contains( "/js/CDF.js" ) && case4.contains( "/css/CDF-CSS.css" ) );
    Assert.assertTrue( case5.contains( "https://testRoot/js/CDF.js" )
        && case5.contains( "https://testRoot/css/CDF-CSS.css" ) );
  }

  private String doCase( String root,
                         boolean absolute, String scheme )
    throws IOException, ThingWriteException {
    return renderApi
      .getHeaders( "", "", DUMMY_WCDF, false, root, absolute, true, false, scheme, null, null );
  }

  public static InputStream getInputStreamFromFileName( String fileName ) throws FileNotFoundException {
    File file = new File( fileName );
    if ( !file.isAbsolute() ) {
      file = new File( TEST_RESOURCES + File.separator + file.getPath() );
    }
    return new FileInputStream( file );
  }

  public static IBasicFile getBasicFileFromFile( final File file ) {
    return new IBasicFile() {
      @Override
      public InputStream getContents() throws IOException {
        return new FileInputStream( file );
      }

      @Override
      public String getName() {
        return file.getName();
      }

      @Override
      public String getFullPath() {
        return file.getAbsolutePath();
      }

      @Override
      public String getPath() {
        return file.getPath();
      }

      @Override
      public String getExtension() {
        if ( !file.isDirectory() ) {
          String name = file.getName();
          int extBegin = name.lastIndexOf( "." ) + 1;
          return name.substring( extBegin );
        }
        return "";
      }

      @Override
      public boolean isDirectory() {
        return file.isDirectory();
      }
    };

  }

}
