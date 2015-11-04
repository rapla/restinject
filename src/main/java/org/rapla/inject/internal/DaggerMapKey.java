package org.rapla.inject.internal;

import dagger.MapKey;

@MapKey(unwrapValue = true)
public @interface DaggerMapKey
{
    String value();
}
