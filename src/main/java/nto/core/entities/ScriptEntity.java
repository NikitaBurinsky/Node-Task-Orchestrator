package nto.core.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scripts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT") // Для больших скриптов
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity owner;

    // Один скрипт может быть запущен много раз (Tasks)
    @Builder.Default
    @OneToMany(mappedBy = "script", cascade = CascadeType.ALL)
    private List<TaskEntity> tasks = new ArrayList<>();
}