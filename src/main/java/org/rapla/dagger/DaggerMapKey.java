package org.rapla.dagger;

import dagger.MapKey;

@MapKey(unwrapValue = true)
public @interface DaggerMapKey
{
    String value();
}
