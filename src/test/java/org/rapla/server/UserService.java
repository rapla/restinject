package org.rapla.server;

import org.rapla.rest.PATCH;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("user")
public class UserService
{
    User user = new User();
    {
        user.name = "christopher";
        user.email = "info@rapla.org";
    }
    @GET
    @Path("{username}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public User test(@PathParam("username") String username)
    {
        if ( username != null && username.equals(user.name))
        {
            return user;
        }
        return null;
    }



    @GET
    @Path("{username}")
    @Produces(MediaType.TEXT_HTML)
    public String test_(@PathParam("username") String username) throws IOException
    {
        return user.toString();
    }

    @PATCH
    @Path("{username}")
    @Produces({ MediaType.APPLICATION_JSON})
    public User testPatch(@PathParam("username") String username,User newUser)
    {
        this.user = newUser;
        return newUser;
    }


    public static class User
    {
        @Override public String toString()
        {
            return "User{" +
                    "name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }

        String name;
        String email;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getEmail()
        {
            return email;
        }

        public void setEmail(String email)
        {
            this.email = email;
        }
    }

}
