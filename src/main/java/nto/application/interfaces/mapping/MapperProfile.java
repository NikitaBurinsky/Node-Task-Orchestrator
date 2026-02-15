package nto.application.interfaces.mapping;

import nto.application.dto.base.BaseDto;
import nto.core.entities.base.BaseEntity;

/**
 * Интерфейс для конкретного маппера (например, ServerProfile).
 * E - Entity, D - Dto
 */
public interface MapperProfile<E extends BaseEntity, D extends BaseDto> {

    // Метод для получения типа Entity, который этот профиль обрабатывает
    Class<E> getEntityClass();

    // Метод для получения типа DTO
    Class<D> getDtoClass();

    D mapToDto(E entity);

    // Создание новой сущности из DTO
    E mapToEntity(D dto);

    // Обновление существующей сущности (как запрошено: Dto + Entity)
    void mapToEntity(D dto, E entity);
}