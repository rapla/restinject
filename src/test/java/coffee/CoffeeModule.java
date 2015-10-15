package coffee;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class CoffeeModule
{
    @Provides Pump providePump(Thermosiphon pump)
    {
        return pump;
    }
    @Provides @Singleton Heater provideHeater(ElectricHeater heater)
    {
        return heater;
    }
    @Provides(type = Provides.Type.MAP)
    @TestKey("foo")
    MapEntry provideFooKey() {
        return new MapEntry("foo value");
    }

    @Provides(type = Provides.Type.MAP)
    @TestKey("bar")
    MapEntry provideBarKey() {
        return new MapEntry("bar value");
    }
}
