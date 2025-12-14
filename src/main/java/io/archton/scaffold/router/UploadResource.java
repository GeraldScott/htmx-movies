package io.archton.scaffold.router;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Path("/uploads")
public class UploadResource {

    private static final String UPLOAD_DIR = "uploads";

    @GET
    @Path("/{filename}")
    public Response getFile(@PathParam("filename") String filename) {
        try {
            java.nio.file.Path filePath = Paths.get(UPLOAD_DIR, filename);
            if (!Files.exists(filePath)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            byte[] data = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }

            return Response.ok(data, contentType).build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
