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

package pt.webdetails.cdf.dd.editor;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DashboardEditorTest extends TestCase {

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testProcessDashboardSupportTag() {

    final String editorPage = "<script type=\"text/javascript\" "
      + "src=\"@CDE_RENDERER_API@/getComponentDefinitions?supports=@SUPPORT_TYPE@\"></script>";

    Assert.assertEquals(
      "<script type=\"text/javascript\" src=\"@CDE_RENDERER_API@/getComponentDefinitions?supports=legacy\"></script>",
      DashboardEditor.processDashboardSupportTag( editorPage, false ));

    Assert.assertEquals(
      "<script type=\"text/javascript\" src=\"@CDE_RENDERER_API@/getComponentDefinitions?supports=amd\"></script>",
      DashboardEditor.processDashboardSupportTag( editorPage, true ) );

  }

}
