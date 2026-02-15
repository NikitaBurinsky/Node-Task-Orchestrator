package nto.application.interfaces.services;

import nto.application.dto.base.BaseDto;
import nto.core.entities.base.BaseEntity;

import java.util.List;

public interface MappingService {

    // Преобразовать Entity -> Dto
    <E extends BaseEntity, D extends BaseDto> D mapToDto(E entity, Class<D> dtoClass);

    // Преобразовать Dto -> Entity (создание новой)
    <E extends BaseEntity, D extends BaseDto> E mapToEntity(D dto, Class<E> entityClass);

    // Обновить существующую Entity данными из Dto
    <E extends BaseEntity, D extends BaseDto> void mapToEntity(D dto, E entity);

    // Хелпер для списков
    <E extends BaseEntity, D extends BaseDto> List<D> mapListToDto(List<E> entities, Class<D> dtoClass);
}