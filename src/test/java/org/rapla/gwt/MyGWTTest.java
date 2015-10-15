package org.rapla.gwt;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;

public class MyGWTTest extends GWTTestCase
{

  /**
   * Specifies a module to use when running this test case. The returned
   * module must include the source for this class.
   * 
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "org.rapla.gwt.GwtTest";
  }
 
  public void testUpperCasingLabel() {
    Element bodyElem = RootPanel.getBodyElement();
    System.out.println(bodyElem);
  }
}