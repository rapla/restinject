package org.rapla.server;

/**
 * Created by Christopher on 05.04.2016.
 */
public class GwtTestServlet extends TestServlet
{
    @Override protected String getPrefix()
    {
        return "/org.rapla.GwtTest.JUnit";
    }
}
