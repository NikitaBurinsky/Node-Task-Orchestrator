package nto.core.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import nto.core.enums.TaskStatus;

@Converter(autoApply = false)
public class TaskStatusConverter implements AttributeConverter<TaskStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TaskStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public TaskStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : TaskStatus.fromCode(dbData);
    }
}
