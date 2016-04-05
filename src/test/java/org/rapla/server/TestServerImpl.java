package org.rapla.server;

import com.google.gson.GsonBuilder;
import org.rapla.common.AnnotationProcessingTest;
import org.rapla.inject.DefaultImplementation;
import org.rapla.inject.InjectionContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.PrintWriter;
import java.util.ArrayList;

@DefaultImplementation(of=TestServer.class,context = InjectionContext.server,export = true)
@Singleton
public class TestServerImpl implements  TestServer
{
    private final StartupParams params;
    @Inject
    public TestServerImpl(StartupParams params){
        this.params = params;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        System.out.println("Request " + request.toString());
        response.setContentType(MediaType.APPLICATION_JSON);
        final PrintWriter writer = response.getWriter();
        final ArrayList<Object> list = new ArrayList<>();
        final AnnotationProcessingTest.Result result = new AnnotationProcessingTest.Result();
        result.setIds(new ArrayList<>());
        result.getIds().add("1");
        result.getIds().add("2");
        list.add(result);
        writer.println(new GsonBuilder().create().toJson(list));
        writer.close();
    }

}