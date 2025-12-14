package io.archton.scaffold.router;

import io.archton.scaffold.entity.Film;
import io.archton.scaffold.entity.FilmCatalog;
import io.archton.scaffold.entity.User;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/films")
public class FilmResource {

    @Inject
    SecurityIdentity securityIdentity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance films(String title, String userName, List<Film> films);
        public static native TemplateInstance filmList(List<Film> films);
        public static native TemplateInstance searchResults(List<FilmCatalog> results, String search);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);
        List<Film> films = Film.findByUser(user);
        return Templates.films("My Films", userName, films);
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance add(@FormParam("filmname") String filmname) {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);

        if (filmname != null && !filmname.isBlank()) {
            Film film = new Film();
            film.name = filmname.trim();
            film.user = user;
            film.persist();
        }

        List<Film> films = Film.findByUser(user);
        return Templates.filmList(films);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance delete(@PathParam("id") Long id) {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);

        // Only delete if film belongs to current user
        Film.delete("id = ?1 and user = ?2", id, user);

        List<Film> films = Film.findByUser(user);
        return Templates.filmList(films);
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance search(@FormParam("search") String search) {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);

        if (search == null || search.isBlank()) {
            return Templates.searchResults(List.of(), null);
        }

        // Get names of films user already has
        List<String> userFilmNames = Film.findByUser(user)
            .stream()
            .map(f -> f.name)
            .toList();

        List<FilmCatalog> results = FilmCatalog.searchByName(search.trim(), userFilmNames);
        return Templates.searchResults(results, search);
    }
}
