package coffee;

import dagger.Lazy;
import javax.inject.Inject;
import java.util.Map;

public class CoffeeMaker {
  private final Lazy<Heater> heater; // Create a possibly costly heater only when we use it.
  private final Pump pump;
  Map<String,MapEntry> map;



  @Inject CoffeeMaker(Lazy<Heater> heater, Pump pump,Map<String,MapEntry> map) {
    this.heater = heater;
    this.pump = pump;
    this.map = map;
  }

  public void brew() {
    heater.get().on();
    pump.pump();
    System.out.println(" [_]P coffee! [_]P ");
    for ( MapEntry entry:map.values())
    {
      System.out.println(entry.toString());
    }
    heater.get().off();
  }
}
