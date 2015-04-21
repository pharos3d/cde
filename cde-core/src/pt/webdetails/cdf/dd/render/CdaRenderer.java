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

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.ri.model.beans.NullPointer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pt.webdetails.cdf.dd.render.cda.*;
import pt.webdetails.cdf.dd.util.CdeEnvironment;
import pt.webdetails.cdf.dd.util.Utils;

/**
 * Creates the .CDA file XML. TODO: this should be changed to a ThingWriter of DataSourceComponents?
 */
public class CdaRenderer {
  private final JSON cdaDefinitions;
  private final JXPathContext doc;

  private static String CDA_ELEMENTS_JXPATH = "/datasources/rows[meta='CDA']";

  public CdaRenderer( String docJson ) {
    // JSONSerializer doesn't like newlines at the head of the file
    this( JXPathContext.newContext( JSONSerializer.toJSON( docJson.replaceAll( "^\n*", "" ) ) ) );
  }

  public CdaRenderer( JXPathContext doc ) {
    this.cdaDefinitions = getCdaDefinitions();
    this.doc = doc; // NOTE: may be null!
  }

  public boolean isEmpty() {
    if ( doc == null ) {
      return true;
    }

    Pointer pointer = doc.getPointer( CDA_ELEMENTS_JXPATH );
    return pointer == null || pointer instanceof NullPointer;
  }

  public String render() throws Exception {
    ByteArrayOutputStream result = new ByteArrayOutputStream();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document cdaFile = builder.newDocument();
    Element root = cdaFile.createElement( "CDADescriptor" );
    cdaFile.appendChild( root );
    Element connections = cdaFile.createElement( "DataSources" );
    root.appendChild( connections );
    Iterator<Pointer> pointers = doc.iteratePointers( CDA_ELEMENTS_JXPATH );
    while ( pointers.hasNext() ) {
      Pointer pointer = pointers.next();
      JXPathContext context = JXPathContext.newContext( pointer.getNode() );
      String connectionId = (String) context.getValue( "properties/.[name='name']/value", String.class );
      Element conn;
      try {
        conn = exportConnection( cdaFile, context, connectionId );
        connections.appendChild( conn );
      } catch ( Exception e ) {
        // things without connections end up here. All is fine,
        // we just need to make sure exportDataAccess doesn't try to generate a connection link
        connectionId = null;
      }

      Element dataAccess = exportDataAccess( cdaFile, context, connectionId );
      if ( dataAccess != null ) {
        root.appendChild( dataAccess );
      }
    }

    TransformerFactory tFactory = TransformerFactory.newInstance();
    Transformer transformer = tFactory.newTransformer();

    DOMSource source = new DOMSource( cdaFile );
    StreamResult res = new StreamResult( new OutputStreamWriter( result, "UTF-8" ) );
    transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
    transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
    transformer.transform( source, res );
    return result.toString();
  }

  private Element exportConnection( Document doc, JXPathContext context, String id ) throws Exception {
    JXPathContext cda = JXPathContext.newContext( cdaDefinitions );
    String type = ( (String) context.getValue( "type", String.class ) ).replaceAll( "Components(.*)", "$1" );
    String conntype = ( (String) context.getValue( "meta_conntype", String.class ) );
    if ( conntype.isEmpty() || conntype.equals( "null" ) ) {
      throw new Exception( "No connection here!" );
    }
    JXPathContext conn =
      JXPathContext.newContext( (JSONObject) cda.getValue( type + "/definition/connection", JSONObject.class ) );
    Element connection = doc.createElement( "Connection" );
    connection.setAttribute( "id", id );
    connection.setAttribute( "type", conntype );
    Iterator<Pointer> params = conn.iteratePointers( "*" );
    while ( params.hasNext() ) {
      Pointer pointer = params.next();
      //JSONObject param = (JSONObject) pointer.getNode();
      String paramName = pointer.asPath().replaceAll( ".*name='(.*?)'.*", "$1" );
      String placement = ( (String) conn.getValue( pointer.asPath() + "/placement", String.class ) ).toLowerCase();

      if ( placement.equals( "attrib" ) ) {
        if ( paramName.equals( "id" ) ) {
          continue;
        } else {
          String value = (String) context.getValue( "properties/.[name='" + paramName + "']/value", String.class );
          connection.setAttribute( paramName, value );
        }
      } else if ( paramName.equals( "variables" ) ) {
        Variables vars = new Variables();
        vars.setContext( context );
        renderProperty( vars, context, paramName, connection );
      } else if ( paramName.matches( "olap4j.*" ) ) {
        renderProperty( new Olap4jProperties( paramName ), context, paramName, connection );
      } else if ( paramName.equals( "dataFile" ) ) {
        renderProperty( new DataFile(), context, paramName, connection );
      }
      /*else if (paramName.equals("ktrFile"))
      {
      String value = (String) context.getValue("properties/.[name='" + paramName + "']/value", String.class);
      Element child = doc.createElement("KtrFile");
      child.appendChild(doc.createTextNode(value));
      connection.appendChild(child);
      }*/
      else if ( paramName.equals( "property" ) && isValidJsonArray( context, paramName ) ) {
        JSONArray jsonArray =
          JSONArray.fromObject( context.getValue( "properties/.[name='" + paramName + "']/value" ) );

        for ( int i = 0; i < jsonArray.size(); i++ ) {

          JSONArray json = (JSONArray) jsonArray.get( i );
          String name = json.getString( 0 ); // property name
          String value = json.getString( 1 ); // property value

          Element child = doc.createElement( Utils.toFirstUpperCase( paramName ) );
          child.setAttribute( "name", name );
          child.appendChild( doc.createTextNode( value ) );
          connection.appendChild( child );
        }
      } else {
        String value = (String) context.getValue( "properties/.[name='" + paramName + "']/value", String.class );
        Element child = doc.createElement( Utils.toFirstUpperCase( paramName ) );
        child.appendChild( doc.createTextNode( value ) );
        connection.appendChild( child );
      }
    }
    return connection;
  }

  private void renderProperty( CdaElementRenderer renderer, JXPathContext context, String propertyName,
                               Element element ) {
    renderer.setDefinition( (JSONObject) context.getValue( "properties/.[name='" + propertyName + "']" ) );
    renderer.renderInto( element );
  }

  private Element exportDataAccess( Document doc, JXPathContext context, String connectionId ) {
    String tagName = "DataAccess";
    // Boolean compound = false;
    JXPathContext cda = JXPathContext.newContext( cdaDefinitions );
    String type = ( (String) context.getValue( "type", String.class ) ).replaceAll( "Components(.*)", "$1" );
    String daType = ( (String) context.getValue( "meta_datype", String.class ) );
    if ( type.equals( "join" ) || type.equals( "union" ) ) {
      tagName = "CompoundDataAccess";
      //  compound = true;
    }
    String name = (String) context.getValue( "properties/.[name='name']/value", String.class );
    JXPathContext conn =
      JXPathContext.newContext( (JSONObject) cda.getValue( type + "/definition/dataaccess", JSONObject.class ) );
    Element dataAccess = doc.createElement( tagName );
    dataAccess.setAttribute( "id", name );
    dataAccess.setAttribute( "type", daType );
    if ( connectionId != null && !connectionId.equals( "" ) ) {
      dataAccess.setAttribute( "connection", connectionId );
    }

    @SuppressWarnings( "unchecked" )
    Iterator<Pointer> params = conn.iteratePointers( "*" );
    while ( params.hasNext() ) {
      Pointer pointer = params.next();
      String paramName = pointer.asPath().replaceAll( ".*name='(.*?)'.*", "$1" );
      String placement = ( (String) conn.getValue( pointer.asPath() + "/placement", String.class ) ).toLowerCase();
      if ( placement.equals( "attrib" ) ) {
        if ( paramName.equals( "id" ) || paramName.equals( "connection" ) || paramName.equals( "cacheDuration" ) ) {
          continue;
        } else {
          String value = (String) context.getValue( "properties/.[name='" + paramName + "']/value", String.class );
          dataAccess.setAttribute( paramName, value );
        }
      } else if ( paramName.equals( "parameters" ) ) {
        renderProperty( new Parameters(), context, paramName, dataAccess );
      } else if ( paramName.equals( "output" ) ) {
        Output output = new Output( context );
        renderProperty( output, context, paramName, dataAccess );
      } else if ( paramName.equals( "variables" ) ) {
        Variables vars = new Variables( context );
        renderProperty( vars, context, paramName, dataAccess );
      } else if ( paramName.equals( "outputMode" ) ) {
        // Skip over outputMode, it's handled by output.
        continue;
      } else if ( paramName.equals( "cacheKeys" ) ) {
        // Skip over cacheKeys, it's handled by cache.
        continue;
      } else if ( paramName.equals( "cache" ) ) {
        Cache cache = new Cache( context );
        renderProperty( cache, context, paramName, dataAccess );
      } else if ( paramName.equals( "columns" ) ) {
        Element cols = dataAccess.getOwnerDocument().createElement( "Columns" );
        renderProperty( new Columns(), context, "cdacolumns", cols );
        renderProperty( new CalculatedColumns(), context, "cdacalculatedcolumns", cols );
        dataAccess.appendChild( cols );
      } else if ( paramName.equals( "top" ) || paramName.equals( "bottom" )
        || paramName.equals( "left" ) || paramName.equals( "right" ) ) {
        Element compoundElem = dataAccess.getOwnerDocument().createElement( Utils.toFirstUpperCase( paramName ) );
        renderProperty( new CompoundComponent(), context, paramName, compoundElem );
        dataAccess.appendChild( compoundElem );
        if ( paramName.equals( "left" ) ) {
          renderProperty( new Keys(), context, "leftkeys", compoundElem );
        } else if ( paramName.equals( "right" ) ) {
          renderProperty( new Keys(), context, "rightkeys", compoundElem );
        }
      } else {
        String value = (String) context.getValue( "properties/.[name='" + paramName + "']/value", String.class );
        if ( paramName.equals( "query" ) ) {
          value = value.trim();
        }
        Element child = doc.createElement( Utils.toFirstUpperCase( paramName ) );
        child.appendChild( doc.createTextNode( value ) );
        dataAccess.appendChild( child );
      }
    }
    return dataAccess;
  }

  public JSON getCdaDefinitions() {
    return CdeEnvironment.getDataSourceManager().getProviderJsDefinition( "cda" );
  }

  public boolean isValidJsonArray( JXPathContext context, String paramName ) {
    return context != null
      && context.getValue( "properties/.[name='" + paramName + "']/value" ) != null
      && Utils.isValidJsonArray( context.getValue( "properties/.[name='" + paramName + "']/value" ).toString() );
  }
}
