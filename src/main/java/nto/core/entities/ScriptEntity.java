package nto.core.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nto.core.entities.base.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scripts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptEntity implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT") // Для больших скриптов
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false; // По умолчанию скрипт приватный

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity owner;

    // Один скрипт может быть запущен много раз (Tasks)
    @Builder.Default
    @OneToMany(mappedBy = "script", cascade = CascadeType.ALL)
    private List<TaskEntity> tasks = new ArrayList<>();
}