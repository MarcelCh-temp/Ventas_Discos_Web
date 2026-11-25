package ms_ordenes.ms_ordenes.controller;

import ms_ordenes.ms_ordenes.security.AuthUser;
import ms_ordenes.ms_ordenes.service.RuletaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards/roulette")
public class RuletaController {
    private final RuletaService ruleta;
    public RuletaController(RuletaService ruleta) { this.ruleta = ruleta; }
    @GetMapping
    public ResponseEntity<RuletaService.Estado> estado(AuthUser user) {
        return ResponseEntity.ok(ruleta.estado(user.userId()));
    }
    @PostMapping("/spin")
    public ResponseEntity<RuletaService.Estado> girar(AuthUser user) {
        return ResponseEntity.ok(ruleta.girar(user.userId()));
    }
}
