package org.rapla.server.rest;

import dagger.MembersInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.validation.GeneralValidator;

import java.lang.reflect.Method;
import java.util.Map;

public class ResteasyMembersInjector implements GeneralValidator
{
    public static final String RAPLA_CONTEXT = "raplaContext";

    @Override
    public void validate(HttpRequest request, Object object, Class<?>... groups)
    {
        final Object context = request.getAttribute(RAPLA_CONTEXT);
        if (context != null)
        {
            try
            {
                Map<String, MembersInjector> membersInjector = (Map<String, MembersInjector> )context;
                final MembersInjector memInj = membersInjector.get(object.getClass().getCanonicalName());
                memInj.injectMembers(object);
//                final Method membersInjectMethod = context.getClass().getDeclaredMethod("");
//                final Object membersInjector = membersInjectMethod.invoke(context);
//                ((MembersInjector) membersInjector).injectMembers(object);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Could not inject dependencies for " + object + ": " + e.getMessage(), e);
            }
        }
        //        attribute.getRestExample().injectMembers((RestExample) object);
    }

    @Override
    public void validateAllParameters(HttpRequest request, Object object, Method method, Object[] parameterValues, Class<?>... groups)
    {

    }

    @Override
    public void validateReturnValue(HttpRequest request, Object object, Method method, Object returnValue, Class<?>... groups)
    {

    }

    @Override
    public boolean isValidatable(Class<?> clazz)
    {
        return true;
    }

    @Override
    public boolean isMethodValidatable(Method method)
    {
        return false;
    }

    @Override
    public void checkViolations(HttpRequest request)
    {

    }

}
