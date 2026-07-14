package com.piedpiper.carbonhub.invitacion.controller;

import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionPublicaResponseDTO;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/invitaciones")
public class InvitacionPublicaController {

    private final InvitacionService invitacionService;

    public InvitacionPublicaController(InvitacionService invitacionService) {
        this.invitacionService = invitacionService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<InvitacionPublicaResponseDTO> resolver(@PathVariable String token) {
        return ResponseEntity.ok(invitacionService.resolver(token));
    }
}
