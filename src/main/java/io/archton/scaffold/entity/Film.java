package io.archton.scaffold.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "films")
public class Film extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 128)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "display_order", nullable = false)
    public Integer displayOrder;

    @Column(name = "photo_path", length = 255)
    public String photoPath;

    public static List<Film> findByUser(User user) {
        return list("user = ?1 order by displayOrder", user);
    }

    public static Integer getMaxOrder(User user) {
        Film film = find("user = ?1 order by displayOrder desc", user).firstResult();
        return film != null ? film.displayOrder : 0;
    }

    public static void reorderFilms(User user) {
        List<Film> films = findByUser(user);
        int order = 1;
        for (Film film : films) {
            film.displayOrder = order++;
        }
    }
}
