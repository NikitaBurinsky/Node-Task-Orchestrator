package nto.application.interfaces.services;

import nto.application.dto.ServerGroupDto;
import nto.application.dto.TaskDto;

import java.util.List;
import java.util.Map;

public interface ServerGroupService {
    ServerGroupDto createGroup(ServerGroupDto dto);

    ServerGroupDto getGroupById(Long id);

    List<ServerGroupDto> getAllGroups();

    void deleteGroup(Long id);

    // Управление составом группы
    void addServerToGroup(Long groupId, Long serverId);

    void removeServerFromGroup(Long groupId, Long serverId);

    // Групповые операции
    Map<Long, Boolean> pingGroup(Long groupId);

    List<TaskDto> executeScriptOnGroup(Long groupId, Long scriptId);

    List<TaskDto> getLastGroupExecutionStatus(Long groupId);
}