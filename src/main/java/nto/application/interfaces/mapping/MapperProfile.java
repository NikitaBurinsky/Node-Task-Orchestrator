package nto.application.interfaces.mapping;

import nto.application.dto.base.BaseDto;
import nto.core.entities.base.BaseEntity;





public interface MapperProfile<E extends BaseEntity, D extends BaseDto> {

    
    Class<E> getEntityClass();

    
    Class<D> getDtoClass();

    D mapToDto(E entity);

    
    E mapToEntity(D dto);

    
    void mapToEntity(D dto, E entity);
}