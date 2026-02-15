package nto.core.entities;

import nto.core.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // Храним как строку ("SUCCESS"), а не число
    private TaskStatus status;

    @Column(columnDefinition = "TEXT")
    private String output; // stdout + stderr

    @CreationTimestamp // Автоматически ставит время создания
    private LocalDateTime createdAt;

    // N:1 К Серверу
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private ServerEntity server;

    // N:1 К Скрипту
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id", nullable = false)
    private ScriptEntity script;
}