package org.rapla.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.rapla.common.ExampleSimpleService;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

@DefaultImplementation(context = InjectionContext.server, of = ExampleSimpleService.class)
public class ExampleSimpleServiceImpl implements ExampleSimpleService
{
    @Inject
    RemoteSession session;
    private final HttpServletRequest request;

    @Inject
    public ExampleSimpleServiceImpl(@Context HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public String sayHello(String param)
    {
        return param + session.toString(request);
    }

    @Override
    public String postHello(String param)
    {
        return param + session.toString(request);
    }

    @Override
    public List<String> translations(String id)
    {
        final ArrayList<String> result = new ArrayList<String>();
        result.add(id);
        result.add(id + "_de");
        result.add(id + "_fr");
        return result;
    }

    @Override public Date addDay(Date date) throws Exception
    {
        final Date dateNextDay = new Date(date.getTime() + 1000l * 60l * 60l * 24l);
        return dateNextDay;
    }

    @Override
    public List<String> exception()
    {
        throw new RuntimeException("Something went wrong");
    }

    @Override
    public Boolean sendBool(Boolean param)
    {
        return param != null ? !param : null;
    }

    @Override
    public Double sendDouble(Double param)
    {
        return param != null ? -1 * param : null;
    }

    @Override
    public Integer sendInt(Integer param)
    {
        return param != null ? -1 * param : null;
    }

    @Override
    public Integer postInt(Integer param)
    {
        return param != null ? -1 * param : null;
    }

    @Override
    public boolean sendPrimBool(boolean param)
    {
        return !param;
    }

    @Override
    public double sendPrimDouble(double param)
    {
        return -1 * param;
    }

    @Override
    public int sendPrimInt(int param)
    {
        return -1 * param;
    }

    @Override
    public String sendString(String param)
    {
        return param;
    }
    
    @Override
    public Character sendChar(Character param)
    {
        if(param == null)
        {
            return param;
        }
        final char charValue = param.charValue();
        int charInt = (int)charValue;
        int nextCharInt = charInt + 1;
        Character nextChar = Character.toChars(nextCharInt)[0];
        return nextChar;
    }
}