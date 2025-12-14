package io.archton.scaffold.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "film_catalog")
public class FilmCatalog extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 128)
    public String name;

    public static List<FilmCatalog> searchByName(String search, List<String> excludeNames) {
        if (excludeNames.isEmpty()) {
            return list("lower(name) like lower(?1)", "%" + search + "%");
        }
        return list("lower(name) like lower(?1) and name not in ?2",
                    "%" + search + "%", excludeNames);
    }
}
