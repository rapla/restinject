package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.common.FutureResult;
import org.rapla.gwtjsonrpc.common.ResultType;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RemoteJsonMethod
public interface AnnotationProcessingTest
{
    FutureResult<List<Result>> sayHello(Parameter param);
    Result sayHello2(Parameter param);
    List<Result> sayHello3(Parameter param);

    public static class Result {
        private String name;
        private List<String> ids;
        private Moyo[] moyos;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public List<String> getIds()
        {
            return ids;
        }

        public void setIds(List<String> ids)
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

    public static class Parameter
    {
        private Map<String, List<String>> requestedIds;
        private List<Integer> actionIds;
        private Date lastRequestTime;
        private List<String> casts;

        public Map<String, List<String>> getRequestedIds()
        {
            return requestedIds;
        }

        public void setRequestedIds(Map<String, List<String>> requestedIds)
        {
            this.requestedIds = requestedIds;
        }

        public List<Integer> getActionIds()
        {
            return actionIds;
        }

        public void setActionIds(List<Integer> actionIds)
        {
            this.actionIds = actionIds;
        }

        public Date getLastRequestTime()
        {
            return lastRequestTime;
        }

        public void setLastRequestTime(Date lastRequestTime)
        {
            this.lastRequestTime = lastRequestTime;
        }

        public List<String> getCasts()
        {
            return casts;
        }

        public void setCasts(List<String> casts)
        {
            this.casts = casts;
        }
    }
}