package nto.services;

import nto.application.dto.base.BaseDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.infrastructure.services.MappingServiceImpl;
import nto.core.entities.base.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingServiceImplTest {

    @Test
    void mapToDtoShouldReturnNullWhenEntityIsNull() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));

        TestDto result = service.mapToDto(null, TestDto.class);

        assertNull(result);
    }

    @Test
    void mapToDtoShouldUseMatchingProfile() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));

        TestDto result = service.mapToDto(new TestEntity("v"), TestDto.class);

        assertEquals("v", result.value());
    }

    @Test
    void mapToDtoShouldThrowWhenProfileMissing() {
        MappingServiceImpl service = new MappingServiceImpl(List.of());

        assertThrows(IllegalStateException.class,
            () -> service.mapToDto(new TestEntity("v"), TestDto.class));
    }

    @Test
    void mapToEntityShouldReturnNullWhenDtoIsNull() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));

        TestEntity result = service.mapToEntity(null, TestEntity.class);

        assertNull(result);
    }

    @Test
    void mapToEntityShouldUseMatchingProfile() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));

        TestEntity result = service.mapToEntity(new TestDto("v"), TestEntity.class);

        assertEquals("v", result.value);
    }

    @Test
    void mapToEntityShouldThrowWhenProfileMissing() {
        MappingServiceImpl service = new MappingServiceImpl(List.of());

        assertThrows(IllegalStateException.class,
            () -> service.mapToEntity(new TestDto("v"), TestEntity.class));
    }

    @Test
    void mapToEntityUpdateShouldWriteValues() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));
        TestEntity entity = new TestEntity("old");

        service.mapToEntity(new TestDto("new"), entity);

        assertEquals("new", entity.value);
    }

    @Test
    void mapToEntityUpdateShouldIgnoreNullInputs() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));
        TestEntity entity = new TestEntity("old");

        service.mapToEntity(null, entity);
        service.mapToEntity(new TestDto("new"), (TestEntity) null);

        assertEquals("old", entity.value);
    }

    @Test
    void mapToEntityUpdateShouldThrowWhenProfileMissing() {
        MappingServiceImpl service = new MappingServiceImpl(List.of());

        assertThrows(IllegalStateException.class,
            () -> service.mapToEntity(new TestDto("v"), new TestEntity("a")));
    }

    @Test
    void mapListToDtoShouldReturnEmptyWhenInputIsNull() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));

        List<TestDto> result = service.mapListToDto(null, TestDto.class);

        assertTrue(result.isEmpty());
    }

    @Test
    void mapListToDtoShouldMapAllItems() {
        MappingServiceImpl service = new MappingServiceImpl(List.of(new TestProfile()));
        List<TestEntity> entities = List.of(new TestEntity("a"), new TestEntity("b"));

        List<TestDto> result = service.mapListToDto(entities, TestDto.class);

        assertEquals(List.of(new TestDto("a"), new TestDto("b")), result);
    }

    private static class TestEntity implements BaseEntity {
        private String value;

        TestEntity(String value) {
            this.value = value;
        }
    }

    private record TestDto(String value) implements BaseDto {
    }

    private static class TestProfile implements MapperProfile<TestEntity, TestDto> {

        @Override
        public Class<TestEntity> getEntityClass() {
            return TestEntity.class;
        }

        @Override
        public Class<TestDto> getDtoClass() {
            return TestDto.class;
        }

        @Override
        public TestDto mapToDto(TestEntity entity) {
            return new TestDto(entity.value);
        }

        @Override
        public TestEntity mapToEntity(TestDto dto) {
            return new TestEntity(dto.value());
        }

        @Override
        public void mapToEntity(TestDto dto, TestEntity entity) {
            entity.value = dto.value();
        }
    }
}
