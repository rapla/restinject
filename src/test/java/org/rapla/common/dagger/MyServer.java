package org.rapla.common.dagger;

import javax.inject.Inject;

public class MyServer
{
    @Inject MyServer(MySingleton singleton)
    {

    }

    public MyRequestImpl request()
    {
        return null;
    }

    public static void main(String[] args)
    {
        MyComponent component = DaggerMyComponent.create();
        {
            final MyServer server = component.getServer();
            server.request();
        }
        {
            final MyServer server = component.getServer();
            server.request();
        }
        {

            {
                final MyHttpRequest myHttpRequest = new MyHttpRequest();
                final MyComponent.MySubComponent requestComponent = component.getRequestComponent(new MyRequestModule(myHttpRequest));
                final MyRequest request = requestComponent.getRequest();
                final MyRequest request2 = requestComponent.getRequest();
            }
            {
                final MyHttpRequest myHttpRequest = new MyHttpRequest();
                final MyComponent.MySubComponent requestComponent = component.getRequestComponent(new MyRequestModule(myHttpRequest));
                final MyRequest request = requestComponent.getRequest();
            }
        }
    }
}
