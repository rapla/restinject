package org.rapla.common;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("ExampleService")
public interface ExampleService
{
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("dontSayHello")
    void dontSayHello();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sayHello6")
    Collection<Result> sayHello6() throws Exception;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sayHello")
    Collection<Result> sayHello(Parameter param);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sayHello2")
    Result sayHello2(Parameter param);

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sayHello3")
    List<Result> sayHello3(Parameter param);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("complex")
    Map<String, Set<String>> complex(@QueryParam("param") Map<String,String> test);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("collections")
    String collections(@QueryParam("param") Collection<String> test, @QueryParam("complex") Collection<Parameter> complex);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("list")
    String list(@QueryParam("param") List<String> test, @QueryParam("complex") List<Parameter> complex);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("set")
    String set(@QueryParam("param") Set<String> test, @QueryParam("complex") Set<Parameter> complex);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("arrays")
    String arrays( @QueryParam("int")int[] integer,@QueryParam("doubleArray") Double[] test, @QueryParam("stringArray") String[] strings,@QueryParam("complexArray") Parameter[] complex);


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("list")
    String charArray(@QueryParam("param1") Character[] charArray1,@QueryParam("param2") char[] charArray2);

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("list")
    String list(@QueryParam("param") List<String> test, @QueryParam("complex") List<Parameter> complex, List<Parameter> postBody);
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("set")
    String set(@QueryParam("param") Set<String> test, @QueryParam("complex") Set<Parameter> complex, Set<Parameter> postBody);
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("longcall")
    String longcall();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("shortcall")
    String shortcall();

    class Result {
        private String name;
        private Collection<String> ids;
        private Moyo[] moyos;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Collection<String> getIds()
        {
            return ids;
        }

        public void setIds(Collection<String> ids)
        {
            this.ids = ids;
        }

        public Moyo[] getMoyos()
        {
            return moyos;
        }

        public void setMoyos(Moyo[] moyos)
        {
            this.moyos = moyos;
        }

        public static class Moyo{
            private Set<String> stringSet;
            private String asd;
            public Set<String> getStringSet()
            {
                return stringSet;
            }

            public void setStringSet(Set<String> stringSet)
            {
                this.stringSet = stringSet;
            }

            public String getAsd()
            {
                return asd;
            }

            public void setAsd(String asd)
            {
                this.asd = asd;
            }

        }

    }

    class Parameter
    {
        private Map<String, List<String>> requestedIds;
        private Collection<Integer> actionIds;
        private Date lastRequestTime;
        private Collection<String> casts;

        public Map<String, List<String>> getRequestedIds()
        {
            return requestedIds;
        }

        public void setRequestedIds(Map<String, List<String>> requestedIds)
        {
            this.requestedIds = requestedIds;
        }

        public Collection<Integer> getActionIds()
        {
            return actionIds;
        }

        public void setActionIds(List<Integer> actionIds)
        {
            this.actionIds = actionIds;
        }

        @Override public String toString()
        {
            return "Parameter{" +
                    "requestedIds=" + requestedIds +
                    ", actionIds=" + actionIds +
                    ", lastRequestTime=" + lastRequestTime +
                    ", casts=" + casts +
                    '}';
        }

        public Date getLastRequestTime()
        {
            return lastRequestTime;
        }

        public void setLastRequestTime(Date lastRequestTime)
        {
            this.lastRequestTime = lastRequestTime;
        }

        public Collection<String> getCasts()
        {
            return casts;
        }

        public void setCasts(Collection<String> casts)
        {
            this.casts = casts;
        }

    }
}