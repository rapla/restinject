package org.rapla.inject.internal;

public class GeneratorUtil
{
    public static String firstCharUp(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.length() < 1)
        {
            return s;
        }
        final String result = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        return result;
    }

    public static String firstCharLow(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.length() < 1)
        {
            return s;
        }
        final String result = Character.toLowerCase(s.charAt(0)) + s.substring(1);
        return result;
    }

}
