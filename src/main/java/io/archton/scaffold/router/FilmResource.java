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
import org.jboss.resteasy.reactive.MultipartForm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Path("/films")
public class FilmResource {

    @Inject
    SecurityIdentity securityIdentity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance films(String title, String userName, List<Film> films);
        public static native TemplateInstance filmList(List<Film> films);
        public static native TemplateInstance filmListMessage(List<Film> films, String message);
        public static native TemplateInstance filmDetail(Film userfilm, String userName);
        public static native TemplateInstance searchResults(List<FilmCatalog> results, String search);
    }

    private static final String UPLOAD_DIR = "uploads";

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

        String message = null;
        if (filmname != null && !filmname.isBlank()) {
            Film film = new Film();
            film.name = filmname.trim();
            film.user = user;
            film.displayOrder = Film.getMaxOrder(user) + 1;
            film.persist();
            message = "Added " + filmname.trim() + " to list of films";
        }

        List<Film> films = Film.findByUser(user);
        return Templates.filmListMessage(films, message);
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

        // Reorder remaining films to remove gaps
        Film.reorderFilms(user);

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

    @POST
    @Path("/reorder")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance reorder(@FormParam("film_order") List<Long> filmIds) {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);

        // Update order based on position in list
        int order = 1;
        for (Long filmId : filmIds) {
            Film.update("displayOrder = ?1 where id = ?2 and user = ?3", order++, filmId, user);
        }

        List<Film> films = Film.findByUser(user);
        return Templates.filmList(films);
    }

    @GET
    @Path("/{id}/detail")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance detail(@PathParam("id") Long id) {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);

        Film film = Film.find("id = ?1 and user = ?2", id, user).firstResult();
        if (film == null) {
            throw new NotFoundException();
        }

        return Templates.filmDetail(film, userName);
    }

    @GET
    @Path("/list-partial")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance listPartial() {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);
        List<Film> films = Film.findByUser(user);
        return Templates.filmList(films);
    }

    @POST
    @Path("/{id}/upload-photo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance uploadPhoto(@PathParam("id") Long id,
                                        @MultipartForm FileUploadForm form) {
        String userName = securityIdentity.getPrincipal().getName();
        User user = User.findByUsername(userName);

        Film film = Film.find("id = ?1 and user = ?2", id, user).firstResult();
        if (film == null) {
            throw new NotFoundException();
        }

        if (form.photo != null) {
            String filename = saveFile(form.photo, film.id);
            film.photoPath = filename;
        }

        return Templates.filmDetail(film, userName);
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.TEXT_HTML)
    public String clear() {
        return "";
    }

    private String saveFile(org.jboss.resteasy.reactive.multipart.FileUpload file, Long filmId) {
        try {
            java.nio.file.Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalName = file.fileName();
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalName.substring(dotIndex);
            }

            String filename = "film_" + filmId + extension;
            java.nio.file.Path targetPath = uploadDir.resolve(filename);
            Files.copy(file.filePath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }
}
