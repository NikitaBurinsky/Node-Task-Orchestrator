package nto.application.interfaces.services;

import nto.application.dto.ScriptDto;

import java.util.List;

public interface ScriptService {
    ScriptDto createScript(ScriptDto dto);

    ScriptDto getScriptById(Long id);

    List<ScriptDto> getAllScripts();

    void deleteScript(Long id);
}