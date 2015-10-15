package coffee;

public class MapEntry
{
    String test;
    MapEntry(String test)
    {
        this.test = test;
    }

    @Override public String toString()
    {
        return test;
    }
}
