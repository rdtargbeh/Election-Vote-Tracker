package Backend.ElectionVote.service;

import Backend.ElectionVote.dto.OrgSettingDto;

import java.util.Map;

public interface OrgSettingService {

    OrgSettingDto getForCurrentTenant();
    OrgSettingDto updateForCurrentTenant(Map<String, Object> patch);
}