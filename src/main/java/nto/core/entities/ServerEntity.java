package nto.core.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import nto.core.entities.base.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "servers")
@Getter
// Используем Getter/Setter вместо @Data для сущностей со сложными связями (избегаем StackOverflow в toString/hashCode)
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerEntity implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String hostname;

    @Column(nullable = false)
    private String ipAddress;

    private Integer port;

    @Column(nullable = false)
    private String username;

    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity owner;

    // (M:N)
    @Builder.Default
    @ManyToMany
    @JoinTable(
        name = "server_group_link",
        joinColumns = @JoinColumn(name = "server_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<ServerGroupEntity> groups = new HashSet<>();

    // 3. Задачи (1:N)
    // CascadeType.ALL: Удаление сервера удалит и TaskEntity
    @Builder.Default
    @OneToMany(mappedBy = "server", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskEntity> tasks = new ArrayList<>();
}