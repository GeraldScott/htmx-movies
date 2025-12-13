package io.archton.scaffold.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@UserDefinition
public class User extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Username
    @Column(unique = true, nullable = false, length = 50)
    public String username;

    @Password
    @Column(nullable = false)
    public String password;

    @Roles
    @Column(nullable = false, length = 50)
    public String role = "user";

    public static User findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }
}
