package nto.core.entities;

import jakarta.persistence.*;
import lombok.*;
import nto.core.entities.ServerEntity;
import nto.core.entities.base.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password; // Будет хранить хеш

    // Связь 1:N (User -> Servers)
    // mappedBy = "owner" указывает на поле 'owner' в классе ServerEntity
    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServerEntity> servers = new ArrayList<>();

    // Связь 1:N (User -> Scripts)
    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScriptEntity> scripts = new ArrayList<>();
}