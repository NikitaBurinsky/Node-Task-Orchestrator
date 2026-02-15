package nto.core.entities;

import jakarta.persistence.*;
import lombok.*;

import jakarta.persistence.*;
import lombok.*;
import nto.core.entities.base.BaseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "servers")
@Getter // Используем Getter/Setter вместо @Data для сущностей со сложными связями (избегаем StackOverflow в toString/hashCode)
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

    // --- Связи ---

    // 1. Владелец
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // nullable = true пока не реализована авторизация, потом false
    private UserEntity owner;

    // 2. Группы (M:N)
    // Владелец связи (Owner side) - тот, кто имеет @JoinTable
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