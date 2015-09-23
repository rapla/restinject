package org.rapla.inject;

/**
 * Specifies in which context the default implemention or the extension point is available.
 * If you specify more then one InjectionContext then the extension point will be available in all specified contexts.
 * <ul>
 *   <li>server: use on the server</li>
 *   <li>client: use on all clients,  gwt, android, ios and swing </li>
 *   <li>swing: use in the swing</li>
 *   <li>gwt: use in the html client</li>
 *   <li>android: use in the native android app (not currently used)</li>
 *   <li>ios: use in the native ios app (not currently used)</li>
 *   <li>all is placeholder for server and client</li>
 * </ul>
 */
public enum InjectionContext
{
    server,
    client,
    swing,
    gwt,
    android,
    ios,
    all;

    public static boolean isInjectableOnSwing(InjectionContext... contexts)
    {
        for ( InjectionContext context:contexts)
        {
            if (context == swing || context == all || context == client)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isInjectableOnGwt(InjectionContext... contexts)
    {
        for ( InjectionContext context:contexts)
        {
            if (context == gwt || context == all || context == client)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isInjectableOnAndroid(InjectionContext... contexts)
    {
        for ( InjectionContext context:contexts)
        {
            if (context == android || context == all || context == client)
            {
                return true;
            }
        }
        return false;

    }

    public static boolean isInjectableOnIos(InjectionContext... contexts)
    {
        for ( InjectionContext context:contexts)
        {
            if (context == ios || context == all || context == client)
            {
                return true;
            }
        }
        return false;
    }


    public static boolean isInjectableOnServer(InjectionContext... contexts)
    {
        for ( InjectionContext context:contexts)
        {
            if (context == all || context == server)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isInjectableOnClient(InjectionContext... contexts)
    {
        for ( InjectionContext context:contexts)
        {
            if (context == all || context == server)
            {
                return true;
            }
        }
        return false;
    }



}
