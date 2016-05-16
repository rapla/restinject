package org.rapla.rest.server.provider.resteasy;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.validation.GeneralValidator;
import org.rapla.rest.server.Injector;

import javax.validation.ValidationException;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;

@Provider
public class ResteasyMembersInjector implements ContextResolver<GeneralValidator>
{
    public static final String INJECTOR_CONTEXT = Injector.class.getCanonicalName();
    final MembersInjectionValidator membersInjectionValidator = new MembersInjectionValidator();

    @Override
    public GeneralValidator getContext(Class<?> type)
    {
        return membersInjectionValidator;
    }

    public static class MembersInjectionValidator implements GeneralValidator
    {

        @Override
        public void validate(HttpRequest request, Object object, Class<?>... groups)
        {
            final Object context = request.getAttribute(INJECTOR_CONTEXT);
            if (context != null )
            {
                if ( !(context instanceof Injector))
                {
                    ValidationException newEx = new ValidationException("Request attribute in " + INJECTOR_CONTEXT + " does not implement: " + Injector.class);
                    throw newEx;
                }
                final Class<?> aClass = object.getClass();
                final Injector membersInjector = (Injector) context;
                if ( membersInjector == null)
                {
                    ValidationException newEx = new ValidationException("No members injector available for " + aClass);
                    throw newEx;
                }
                try
                {
                    membersInjector.injectMembers( object);
                }
                catch (Exception e)
                {
                    ValidationException newEx = new ValidationException("Could not inject dependencies for " + object + ": " + e.getMessage(), e);
                    //newEx.printStackTrace();
                    throw newEx;
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
}
