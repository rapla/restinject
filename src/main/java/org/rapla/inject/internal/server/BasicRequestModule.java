package org.rapla.inject.internal.server;

import dagger.Module;
import dagger.Provides;
import org.rapla.server.RequestScoped;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Module
public class BasicRequestModule
{
  HttpServletRequest request;
  HttpServletResponse response;
  public BasicRequestModule(HttpServletRequest request, HttpServletResponse response){
    this.request = request;this.response = response;
  };
  @Provides @RequestScoped public HttpServletRequest provideRequest()  {  return request;    }
  @Provides @RequestScoped public HttpServletResponse provideResponse(){ return response;     }
}
