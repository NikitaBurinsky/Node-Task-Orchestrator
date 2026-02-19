package nto.infrastructure.services;

import nto.application.dto.base.BaseDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.application.interfaces.services.MappingService;
import nto.core.entities.base.BaseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MappingServiceImpl implements MappingService {

    private final Map<String, MapperProfile<?, ?>> profiles;

    // Constructor Injection: Spring сам найдет все реализации MapperProfile и передаст их сюда списком
    public MappingServiceImpl(List<MapperProfile<?, ?>> profileList) {
        this.profiles = profileList.stream()
            .collect(Collectors.toMap(
                // Ключ: "EntityClassName_DtoClassName"
                p -> buildKey(p.getEntityClass(), p.getDtoClass()),
                p -> p
            ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends BaseEntity, D extends BaseDto> D mapToDto(E entity, Class<D> dtoClass) {
        if (entity == null) {
            return null;
        }
        var profile = (MapperProfile<E, D>) getProfile(entity.getClass(), dtoClass);
        if (profile == null) {
            throw new IllegalArgumentException(
                "No profile found for entity class: " + entity.getClass());
        }
        return profile.mapToDto(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends BaseEntity, D extends BaseDto> E mapToEntity(D dto, Class<E> entityClass) {
        if (dto == null) {
            return null;
        }
        var profile = (MapperProfile<E, D>) getProfile(entityClass, dto.getClass());
        if (profile == null) {
            throw new IllegalArgumentException("No profile found for dto class: " + dto.getClass());
        }
        return profile.mapToEntity(dto);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends BaseEntity, D extends BaseDto> void mapToEntity(D dto, E entity) {
        if (dto == null || entity == null) {
            return;
        }
        var profile = (MapperProfile<E, D>) getProfile(entity.getClass(), dto.getClass());
        if (profile == null) {
            throw new IllegalArgumentException(
                "No profile found for entity class: " + entity.getClass());
        }
        profile.mapToEntity(dto, entity);
    }

    @Override
    public <E extends BaseEntity, D extends BaseDto> List<D> mapListToDto(List<E> entities,
                                                                          Class<D> dtoClass) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
            .map(e -> mapToDto(e, dtoClass))
            .collect(Collectors.toList());
    }

    private MapperProfile<?, ?> getProfile(Class<?> entityClass, Class<?> dtoClass) {
        String key = buildKey(entityClass, dtoClass);
        MapperProfile<?, ?> profile = profiles.get(key);
        if (profile == null) {
            throw new IllegalStateException("Mapper profile not found for pair: "
                + entityClass.getSimpleName() + " <-> " + dtoClass.getSimpleName());
        }
        return profile;
    }

    private String buildKey(Class<?> entityClass, Class<?> dtoClass) {
        return entityClass.getName() + "_" + dtoClass.getName();
    }
}