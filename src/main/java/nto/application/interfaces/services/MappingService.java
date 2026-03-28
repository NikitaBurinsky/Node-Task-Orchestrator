package nto.application.interfaces.services;

import nto.application.dto.base.BaseDto;
import nto.core.entities.base.BaseEntity;

import java.util.List;

public interface MappingService {

    
    <E extends BaseEntity, D extends BaseDto> D mapToDto(E entity, Class<D> dtoClass);

    
    <E extends BaseEntity, D extends BaseDto> E mapToEntity(D dto, Class<E> entityClass);

    
    <E extends BaseEntity, D extends BaseDto> void mapToEntity(D dto, E entity);

    
    <E extends BaseEntity, D extends BaseDto> List<D> mapListToDto(List<E> entities,
                                                                   Class<D> dtoClass);
}