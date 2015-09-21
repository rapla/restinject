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
    all
}
