package com.uzima.bootstrap.adapter.http.qrcode;

import com.uzima.application.qrcode.*;
import com.uzima.application.qrcode.port.in.*;
import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.user.model.UserId;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller — QR Codes Contextuels Intelligents.
 *
 * POST   /api/qrcodes                         → 201 + id
 * GET    /api/qrcodes                         → 200 + liste
 * POST   /api/qrcodes/{id}/scan               → 200 + ScanResult
 * POST   /api/qrcodes/{id}/revoke             → 204
 * PUT    /api/qrcodes/{id}/rules              → 204
 * GET    /api/qrcodes/suggest                  → 200 + suggestion
 */
@RestController
@RequestMapping("/api/qrcodes")
public class QrCodeController {

    private final CreateQrCodeUseCase         createQrCode;
    private final GetMyQrCodesUseCase         getMyQrCodes;
    private final ScanQrCodeUseCase           scanQrCode;
    private final RevokeQrCodeUseCase         revokeQrCode;
    private final ConfigureQrCodeRulesUseCase configureRules;
    private final SuggestQrCodeTypeUseCase    suggestType;

    public QrCodeController(CreateQrCodeUseCase createQrCode,
                             GetMyQrCodesUseCase getMyQrCodes,
                             ScanQrCodeUseCase scanQrCode,
                             RevokeQrCodeUseCase revokeQrCode,
                             ConfigureQrCodeRulesUseCase configureRules,
                             SuggestQrCodeTypeUseCase suggestType) {
        this.createQrCode  = createQrCode;
        this.getMyQrCodes  = getMyQrCodes;
        this.scanQrCode    = scanQrCode;
        this.revokeQrCode  = revokeQrCode;
        this.configureRules= configureRules;
        this.suggestType   = suggestType;
    }

    // -------------------------------------------------------------------------
    // POST /api/qrcodes
    // -------------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<Map<String, String>> createQrCode(
            @RequestBody CreateQrCodeRequest req
    ) {
        UserId ownerId = SecurityContextHelper.currentUserId();
        var cmd = new CreateQrCodeCommand(
            ownerId,
            QrCodeType.valueOf(req.type()),
            req.validForMinutes() != null ? Duration.ofMinutes(req.validForMinutes()) : null,
            req.singleUse() != null ? req.singleUse() : false
        );
        QrCode qrCode = createQrCode.execute(cmd);
        return ResponseEntity
            .created(URI.create("/api/qrcodes/" + qrCode.id()))
            .body(Map.of("id", qrCode.id().toString()));
    }

    // -------------------------------------------------------------------------
    // GET /api/qrcodes
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<QrCodeResponse>> getMyQrCodes() {
        UserId ownerId = SecurityContextHelper.currentUserId();
        var views = getMyQrCodes.execute(ownerId);
        return ResponseEntity.ok(views.stream().map(QrCodeResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // POST /api/qrcodes/{id}/scan
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/scan")
    public ResponseEntity<Map<String, Object>> scan(
            @PathVariable UUID id
    ) {
        UserId scannerId = SecurityContextHelper.currentUserId();
        var result = scanQrCode.execute(new ScanQrCodeCommand(
            QrCodeId.of(id), scannerId
        ));
        return ResponseEntity.ok(Map.of(
            "qrCodeId",   result.qrCodeId(),
            "type",       result.type().name(),
            "ownerId",    result.ownerId(),
            "totalScans", result.totalScans()
        ));
    }

    // -------------------------------------------------------------------------
    // POST /api/qrcodes/{id}/revoke
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        revokeQrCode.execute(QrCodeId.of(id), requesterId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // PUT /api/qrcodes/{id}/rules
    // -------------------------------------------------------------------------

    @PutMapping("/{id}/rules")
    public ResponseEntity<Void> configureRules(
            @PathVariable UUID id,
            @RequestBody ConfigureRulesRequest req
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        GeofenceRule geofence = req.geofence() != null
            ? GeofenceRule.of(req.geofence().latitude(), req.geofence().longitude(), req.geofence().radiusMeters())
            : null;

        PersonalizationRule personalization = req.personalization() != null
            ? PersonalizationRule.of(req.personalization().condition(), req.personalization().targetProfile())
            : null;

        configureRules.execute(new ConfigureQrCodeRulesCommand(
            QrCodeId.of(id), requesterId, geofence, personalization
        ));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // GET /api/qrcodes/suggest
    // -------------------------------------------------------------------------

    @GetMapping("/suggest")
    public ResponseEntity<Map<String, String>> suggest() {
        UserId userId = SecurityContextHelper.currentUserId();
        var suggestion = suggestType.execute(userId);
        return ResponseEntity.ok(Map.of(
            "suggestedType", suggestion.suggestedType().name(),
            "reason",        suggestion.reason()
        ));
    }

    // -------------------------------------------------------------------------
    // DTOs internes
    // -------------------------------------------------------------------------

    record CreateQrCodeRequest(String type, Long validForMinutes, Boolean singleUse) {}

    record ConfigureRulesRequest(GeofenceDto geofence, PersonalizationDto personalization) {}
    record GeofenceDto(double latitude, double longitude, int radiusMeters) {}

    record PersonalizationDto(String condition, String targetProfile) {}

    record QrCodeResponse(
            String id, String ownerId, String type, String status,
            Instant createdAt, int scanCount, boolean hasGeofence
    ) {
        static QrCodeResponse from(GetMyQrCodesUseCase.QrCodeView view) {
            QrCode q = view.qrCode();
            return new QrCodeResponse(
                    q.id().toString(), q.ownerId().toString(), q.type().name(),
                    view.isActive() ? "ACTIVE" : "INACTIVE",
                    q.createdAt(), q.scanCount(), q.hasGeofence()
            );
        }
    }
}
