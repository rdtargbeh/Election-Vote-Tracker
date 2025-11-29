package Backend.ElectionVote.controller;

import Backend.ElectionVote.dto.OrgSettingDto;
import Backend.ElectionVote.service.OrgSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/org-settings")
public class OrgSettingController {

    @Autowired
    private  OrgSettingService service;


    @GetMapping
    public OrgSettingDto get() { return service.getForCurrentTenant(); }

    @PatchMapping
    public OrgSettingDto update(@RequestBody Map<String,Object> patch) {
        return service.updateForCurrentTenant(patch);
    }
}
