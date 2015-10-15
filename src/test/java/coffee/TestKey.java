package coffee;

import dagger.MapKey;

@MapKey(unwrapValue = true)
@interface TestKey {
    String value();
}