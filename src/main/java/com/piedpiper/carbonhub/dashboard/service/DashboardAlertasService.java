package com.piedpiper.carbonhub.dashboard.service;

import com.piedpiper.carbonhub.certificacion.models.entities.Certificacion;
import com.piedpiper.carbonhub.certificacion.models.enums.EstadoCertificacion;
import com.piedpiper.carbonhub.certificacion.repository.CertificacionRepository;
import com.piedpiper.carbonhub.common.ZonasHorarias;
import com.piedpiper.carbonhub.dashboard.models.dtos.AlertaVencimientoResponseDTO;
import com.piedpiper.carbonhub.emision.service.EmisionEmpresaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Panel "Alertas activas" del dashboard (PP-76): certificaciones de la
 * empresa autenticada que ya vencieron o estan dentro de alguno de los
 * umbrales de alerta (90/30/7 dias, PP-70), ordenadas de mas a menos
 * urgente.
 *
 * <p>Deliberadamente NO lee la tabla {@code alertas}: esa tabla registra el
 * envio de correo (PP-71) — su {@code estado} es PENDIENTE/ENVIADA/FALLIDA,
 * sobre el correo, no sobre si la certificacion sigue vencida — y una vez
 * generada una fila para un umbral, esa fila no desaparece aunque la
 * empresa renueve la certificacion despues. Igual que
 * {@link CalendarioVencimientosService}, este panel recalcula
 * {@code diasRestantes} contra {@code fechaVencimiento} en cada consulta:
 * si la certificacion se renueva (la fecha de vencimiento se mueve mas
 * alla de 90 dias), deja de listarse sola, sin depender de que nadie borre
 * o actualice una fila de {@code alertas}. El filtro por
 * {@code estado = ACTIVA} cubre el caso de una certificacion revocada
 * (activa = no revocada; vigente = no vencida, ver
 * {@code CertificacionRepository}).</p>
 *
 * <p>Se traen todas las certificaciones activas de la empresa antes de
 * filtrar el umbral de 90 dias en memoria (a la escala tipica de alertas
 * por empresa esto es preferible a sumar un metodo de repositorio casi
 * identico solo para mover el filtro a SQL; si el volumen crece, ese es
 * el primer punto a optimizar).</p>
 */
@Service
public class DashboardAlertasService {

    private final EmisionEmpresaService emisionEmpresaService;
    private final CertificacionRepository certificacionRepository;
    private final VencimientoPresentacionService vencimientoPresentacionService;

    public DashboardAlertasService(
            EmisionEmpresaService emisionEmpresaService,
            CertificacionRepository certificacionRepository,
            VencimientoPresentacionService vencimientoPresentacionService) {
        this.emisionEmpresaService = emisionEmpresaService;
        this.certificacionRepository = certificacionRepository;
        this.vencimientoPresentacionService = vencimientoPresentacionService;
    }

    @Transactional(readOnly = true)
    public List<AlertaVencimientoResponseDTO> obtenerAlertas(UUID usuarioId) {
        UUID empresaId = emisionEmpresaService.empresaId(usuarioId);
        LocalDate hoy = LocalDate.now(ZonasHorarias.COSTA_RICA);

        List<Certificacion> certificaciones = certificacionRepository
                .findByEmpresaIdAndEstadoOrderByFechaVencimientoAsc(empresaId, EstadoCertificacion.ACTIVA);

        // Sin .sorted(): el repositorio ya devuelve fechaVencimiento ascendente, y
        // diasRestantes es monotono respecto a esa fecha (misma "hoy" para todas), asi
        // que el orden ya queda correcto sin un paso extra. Si esto deja de ser cierto
        // (p. ej. diasRestantes empieza a depender de algo mas que fechaVencimiento),
        // hay que volver a ordenar explicitamente aca.
        return certificaciones.stream()
                .map(certificacion -> aDto(certificacion, hoy))
                .filter(alerta -> vencimientoPresentacionService.dentroDelUmbralMaximo(alerta.getDiasRestantes()))
                .collect(Collectors.toList());
    }

    private AlertaVencimientoResponseDTO aDto(Certificacion certificacion, LocalDate hoy) {
        long diasRestantes = ChronoUnit.DAYS.between(hoy, certificacion.getFechaVencimiento());

        return new AlertaVencimientoResponseDTO(
                certificacion.getId(),
                vencimientoPresentacionService.nombreLegible(certificacion),
                certificacion.getFechaVencimiento(),
                diasRestantes,
                vencimientoPresentacionService.urgenciaPara(diasRestantes));
    }
}
