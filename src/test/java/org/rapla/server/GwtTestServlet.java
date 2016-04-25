package org.rapla.server;

public class GwtTestServlet extends TestServlet
{
    @Override protected String getPrefix()
    {
        return "/org.rapla.GwtTest.JUnit";
    }
}
