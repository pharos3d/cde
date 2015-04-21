/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.meta.writer.cderunjs.legacy;

import org.apache.commons.lang.StringUtils;
import pt.webdetails.cdf.dd.model.meta.ComponentType;
import pt.webdetails.cdf.dd.model.meta.DataSourceComponentType;
import pt.webdetails.cdf.dd.model.core.Attribute;
import pt.webdetails.cdf.dd.model.core.Thing;
import pt.webdetails.cdf.dd.model.meta.PropertyType;
import pt.webdetails.cdf.dd.model.meta.PropertyTypeUsage;
import pt.webdetails.cdf.dd.model.core.UnsupportedThingException;
import pt.webdetails.cdf.dd.model.core.writer.IThingWriteContext;
import pt.webdetails.cdf.dd.model.core.writer.IThingWriter;
import pt.webdetails.cdf.dd.model.core.writer.IThingWriterFactory;
import pt.webdetails.cdf.dd.model.core.writer.ThingWriteException;
import pt.webdetails.cdf.dd.model.core.writer.js.JsWriterAbstract;
import pt.webdetails.cdf.dd.model.meta.writer.cderunjs.CdeRunJsHelper;
import pt.webdetails.cdf.dd.util.JsonUtils;

/**
 * @author dcleao
 */
public class CdeRunJsComponentTypeWriter extends JsWriterAbstract implements IThingWriter
{
  public void write(Object output, IThingWriteContext context, Thing t) throws ThingWriteException
  {
    ComponentType comp = (ComponentType)t;
    StringBuilder out  = (StringBuilder)output;
  
    Attribute cdeModelIgnoreAttr = comp.tryGetAttribute("cdeModelIgnore");
    
    if(cdeModelIgnoreAttr != null && "true".equals(cdeModelIgnoreAttr.getValue())) { return; }
    
    String name = comp.getName();
    
    // the name in cdefdejs/components/rows/type
    String modelPrefix  = CdeRunJsHelper.getComponentTypeModelPrefix( comp );
    String modelName    = CdeRunJsHelper.getComponentTypeModelId(comp, modelPrefix);
    String modelVarName = modelName + "Model";
    
    String label = comp.getLabel();
    String jsTooltip = JsonUtils.toJsString(comp.getTooltip());

    // --------------
    // ENTRY
    if(comp.getVisible()) 
    {
      String entryName = name + "Entry";
      String entryId   = name.toUpperCase() + "_ENTRY";
      String baseEntryType = comp.tryGetAttributeValue("cdePalleteType", "PalleteEntry");
      out.append(NEWLINE);
      out.append("var ");
      out.append(entryName);
      out.append(" = ");
      out.append(baseEntryType);
      out.append(".extend({");
      out.append(NEWLINE);

      addJsProperty(out, "id",           JsonUtils.toJsString(entryId), INDENT1, true);
      addJsProperty(out, "name",         JsonUtils.toJsString(label  ), INDENT1, false);
      addJsProperty(out, "description",  jsTooltip, INDENT1, false);
      addJsProperty(out, "category",     JsonUtils.toJsString(comp.getCategory()), INDENT1, false);
      addJsProperty(out, "categoryDesc", JsonUtils.toJsString(comp.getCategoryLabel()), INDENT1, false);
      addCommaAndLineSep(out);
      out.append(INDENT1);
      out.append("getStub: function() {");
      out.append(NEWLINE);
      out.append(INDENT2);
      out.append("return ");
      out.append(modelVarName);
      out.append(".getStub();");
      out.append(NEWLINE);
      out.append(INDENT1);
      out.append("}");
      out.append(NEWLINE);
      out.append("});");
      out.append(NEWLINE);
      
      // TODO: maybe asbtract this into some explicit ComponentType «Class» concept/field or something?
      String collectionName = (comp instanceof DataSourceComponentType) ?
              "CDFDDDatasourcesArray" : 
              "CDFDDComponentsArray";

      out.append(collectionName);
      out.append(".push(new ");
      out.append(entryName);
      out.append("());");
      out.append(NEWLINE);
    }
    
    // --------------
    // OWN PROPERTIES
    if(comp.getPropertyUsageCount() > 0)
    {
      IThingWriterFactory factory = context.getFactory();
      for(PropertyTypeUsage propUsage : comp.getPropertyUsages())
      {
        if(propUsage.isOwned())
        {
          PropertyType prop = propUsage.getProperty();
          IThingWriter writer;
          try
          {
            writer = factory.getWriter(prop);
          }
          catch (UnsupportedThingException ex)
          {
            throw new ThingWriteException(ex);
          }

          writer.write(out, context, prop);
        }
      }
    }
    
    // --------------
    // MODEL
    //
    // Models aren't instantiated. 
    // Their class is registered and only its static methods are used.
    // It's a static factory:
    //    AModelClass.getStub() --> creates a new model of given type
    // Own properties
    // Aliased properties
    out.append(NEWLINE);
    out.append("var "); out.append(modelVarName); out.append(" = BaseModel.create({"); out.append(NEWLINE);
    
    addJsProperty(out, "name",        JsonUtils.toJsString(modelName), INDENT1, true);
    addJsProperty(out, "description", jsTooltip, INDENT1, false);
    if(comp.getLegacyNameCount() > 0) 
    {
      addCommaAndLineSep(out); out.append(INDENT1);
      out.append("legacyNames: [");
      boolean isFirstLegacyName = true;
      for(String legacyName : comp.getLegacyNames()) 
      {
        if(isFirstLegacyName) 
        {
          isFirstLegacyName = false;
        }
        else
        {
          out.append(", ");
        } 

        out.append(JsonUtils.toJsString(legacyName));
      }
      out.append("]");
    }
    
    boolean isFirstAttr = true;
    for(Attribute attribute : comp.getAttributes())
    {
      String attName = attribute.getName();
      
      if(!"cdeModelIgnore".equals(attName) &&
         !"cdeModelPrefix".equals(attName) &&
         !"cdePalleteType".equals(attName))
      {
        if(isFirstAttr)
        {
          addJsProperty(out, "metas", "{", INDENT1, false);
          out.append(NEWLINE);
        }
        
        String jsAttrName = attribute.getName();
        if(StringUtils.isEmpty(jsAttrName))
        {
          jsAttrName = "meta";
        }
        else
        {
          jsAttrName = "meta_" + jsAttrName;
        }
        
        addJsProperty(
            out,
            JsonUtils.toJsString(jsAttrName),
            JsonUtils.toJsString(attribute.getValue()),
            INDENT2,
            isFirstAttr);
        
        if(isFirstAttr) { isFirstAttr = false; }
      }
    }
    
    if(!isFirstAttr)
    {
      out.append(NEWLINE);
      out.append(INDENT1);
      out.append("}");
    }
    
    addJsProperty(out, "properties", "[", INDENT1, false);
    if(comp.getPropertyUsageCount() > 0)
    {
      boolean isFirstProp = true;
      for(PropertyTypeUsage propUsage : comp.getPropertyUsages())
      {
        if(isFirstProp) { isFirstProp = false; }
        else            { out.append(","); }
        out.append(NEWLINE);
        out.append(INDENT2);
        
        // NOTE: the use of camelName to obtain the property in the client.
        String camelName = propUsage.getProperty().getCamelName();
        String alias     = propUsage.getAlias();
        
        boolean isAliased = !camelName.equals(alias);
        boolean isOwned   = propUsage.isOwned();
        
        String jsName = JsonUtils.toJsString(camelName);
        if(isAliased || isOwned) 
        {
          out.append("{name: "); out.append(jsName);
          
          if(isAliased) 
          {
            out.append(", alias: "); out.append(JsonUtils.toJsString(alias));
          }
          
          if(isOwned)
          {
            out.append(", owned: true");
          }
          out.append("}");
        }
        else
        {
          out.append(jsName);
        }
      }
      
      out.append(NEWLINE);
      out.append(INDENT1);
      out.append("]");
      out.append(NEWLINE);
    }
    
    out.append("});"); out.append(NEWLINE); // .create({
  }
}
