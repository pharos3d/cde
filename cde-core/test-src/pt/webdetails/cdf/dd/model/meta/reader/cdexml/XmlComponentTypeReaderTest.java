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

package pt.webdetails.cdf.dd.model.meta.reader.cdexml;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.dom4j.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pt.webdetails.cdf.dd.model.meta.ComponentType;
import pt.webdetails.cdf.dd.model.meta.CustomComponentType;
import pt.webdetails.cdf.dd.model.meta.reader.cdexml.fs.XmlFsPluginThingReaderFactory;

import static org.mockito.Mockito.*;

public class XmlComponentTypeReaderTest extends TestCase {

  private static XmlComponentTypeReader xmlComponentTypeReader;

  @Before
  public void setUp() throws Exception {
    xmlComponentTypeReader = new XmlAdhocComponentTypeReader(
      CustomComponentType.Builder.class,
      mock( XmlFsPluginThingReaderFactory.class ) );

  }

  @After
  public void tearDown() throws Exception {
    xmlComponentTypeReader = null;
  }

  @Test
  public void testReadComponentSupportType() {

    ComponentType.Builder builder;
    Element elem;

    // supports AMD
    builder = new CustomComponentType.Builder();
    elem = DocumentHelper.createDocument().addElement("DesignerComponent");
    elem.addElement( "Contents" )
      .addElement( "Implementation" )
      .addAttribute( "supportsAMD", "true" )
      .addAttribute( "supportsLegacy", "false" );
    xmlComponentTypeReader.readComponentSupportType( builder, elem );
    Assert.assertEquals( builder.isSupportsLegacy(), false );
    Assert.assertEquals( builder.isSupportsAMD(), true );

    builder = new CustomComponentType.Builder();
    elem = DocumentHelper.createDocument().addElement( "DesignerComponent" );
    elem.addElement( "Contents" )
      .addElement( "Implementation" )
      .addAttribute( "supportsAMD", "true" );
    xmlComponentTypeReader.readComponentSupportType( builder, elem );
    Assert.assertEquals( builder.isSupportsLegacy(), false );
    Assert.assertEquals( builder.isSupportsAMD(), true );

    // supports Legacy
    builder = new CustomComponentType.Builder();
    elem = DocumentHelper.createDocument().addElement( "DesignerComponent" );
    elem.addElement( "Contents" )
      .addElement( "Implementation" )
      .addAttribute( "supportsAMD", "false" )
      .addAttribute( "supportsLegacy", "true" );
    xmlComponentTypeReader.readComponentSupportType( builder, elem );
    Assert.assertEquals( builder.isSupportsLegacy(), true );
    Assert.assertEquals( builder.isSupportsAMD(), false );

    builder = new CustomComponentType.Builder();
    elem = DocumentHelper.createDocument().addElement( "DesignerComponent" );
    elem.addElement( "Contents" )
      .addElement( "Implementation" )
      .addAttribute( "supportsLegacy", "true" );
    xmlComponentTypeReader.readComponentSupportType( builder, elem );
    Assert.assertEquals( builder.isSupportsLegacy(), true );
    Assert.assertEquals( builder.isSupportsAMD(), false );

    // supports both AMD and Legacy
    builder = new CustomComponentType.Builder();
    elem = DocumentHelper.createDocument().addElement( "DesignerComponent" );
    elem.addElement( "Contents" )
      .addElement( "Implementation" )
      .addAttribute( "supportsAMD", "true" )
      .addAttribute( "supportsLegacy", "true" );
    xmlComponentTypeReader.readComponentSupportType( builder, elem );
    Assert.assertEquals( builder.isSupportsLegacy(), true );
    Assert.assertEquals( builder.isSupportsAMD(), true );

    // empty support type should use default values
    builder = new CustomComponentType.Builder();
    elem = DocumentHelper.createDocument().addElement( "DesignerComponent" );
    elem.addElement( "Contents" )
      .addElement( "Implementation" );
    xmlComponentTypeReader.readComponentSupportType( builder, elem );
    Assert.assertEquals( builder.isSupportsLegacy(), true );
    Assert.assertEquals( builder.isSupportsAMD(), false );
  }
}
