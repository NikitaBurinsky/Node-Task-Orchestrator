package nto.core.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "server_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // ManyToMany. Обычно инициализируют Set, а не List, чтобы избежать дублей
    @Builder.Default
    @ManyToMany(mappedBy = "groups") // "groups" - поле в ServerEntity
    private Set<ServerEntity> servers = new HashSet<>();
}