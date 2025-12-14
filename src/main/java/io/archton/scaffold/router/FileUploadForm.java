package io.archton.scaffold.router;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FileUploadForm {
    @FormParam("photo")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public FileUpload photo;
}
