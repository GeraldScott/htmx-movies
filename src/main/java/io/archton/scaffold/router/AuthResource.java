package io.archton.scaffold.router;

import io.archton.scaffold.entity.User;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/")
public class AuthResource {

    @Inject
    SecurityIdentity securityIdentity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance login(String title, boolean error, String userName);
        public static native TemplateInstance register(String title, String error, String userName);
    }

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance loginPage(@QueryParam("error") boolean error) {
        String userName = getUserName();
        return Templates.login("Login", error, userName);
    }

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerPage() {
        String userName = getUserName();
        return Templates.register("Register", null, userName);
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("confirmPassword") String confirmPassword) {

        String userName = getUserName();

        // Validation
        if (username == null || username.isBlank() || username.length() < 3) {
            return Response.ok(Templates.register("Register", "Username must be at least 3 characters", userName)).build();
        }
        if (password == null || password.length() < 6) {
            return Response.ok(Templates.register("Register", "Password must be at least 6 characters", userName)).build();
        }
        if (!password.equals(confirmPassword)) {
            return Response.ok(Templates.register("Register", "Passwords do not match", userName)).build();
        }
        if (User.existsByUsername(username)) {
            return Response.ok(Templates.register("Register", "Username already exists", userName)).build();
        }

        // Create user
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = "user";
        user.persist();

        return Response.seeOther(URI.create("/login")).build();
    }

    @POST
    @Path("/check-username")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public String checkUsername(@FormParam("username") String username) {
        if (username == null || username.isBlank()) {
            return "";
        }
        if (username.length() < 3) {
            return "<span class=\"uk-text-warning\">Username must be at least 3 characters</span>";
        }
        if (User.existsByUsername(username)) {
            return "<span class=\"uk-text-danger\">This username already exists</span>";
        }
        return "<span class=\"uk-text-success\">Username is available</span>";
    }

    @POST
    @Path("/logout")
    public Response logout() {
        NewCookie removeCookie = new NewCookie.Builder("quarkus-credential")
                .path("/")
                .maxAge(0)
                .build();
        return Response.seeOther(URI.create("/"))
                .cookie(removeCookie)
                .build();
    }

    private String getUserName() {
        if (securityIdentity.isAnonymous()) {
            return null;
        }
        return securityIdentity.getPrincipal().getName();
    }
}
