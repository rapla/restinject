package org.rapla.inject.internal.server;

import dagger.Module;
import dagger.Provides;

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
  @Provides public HttpServletRequest provideRequest()  {  return request;    }
  @Provides public HttpServletResponse provideResponse(){ return response;     }
}
