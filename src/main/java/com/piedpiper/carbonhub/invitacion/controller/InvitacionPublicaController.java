package com.piedpiper.carbonhub.invitacion.controller;

import com.piedpiper.carbonhub.invitacion.models.dtos.InvitacionPublicaResponseDTO;
import com.piedpiper.carbonhub.invitacion.service.InvitacionService;
import com.piedpiper.carbonhub.notification.models.dtos.TokenUnSoloUsoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/invitaciones")
public class InvitacionPublicaController {

    private final InvitacionService invitacionService;

    public InvitacionPublicaController(InvitacionService invitacionService) {
        this.invitacionService = invitacionService;
    }

    @PostMapping("/resolver")
    public ResponseEntity<InvitacionPublicaResponseDTO> resolver(
            @Valid @RequestBody TokenUnSoloUsoRequestDTO request) {
        return ResponseEntity.ok(invitacionService.resolver(request.getToken()));
    }
}
