package com.uzima.domain.qrcode.model;

import com.uzima.domain.shared.DomainException;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Value Object : Limite du nombre de scans autorisés pour un QR Code.
 * Modes :
 * - UNLIMITED : aucune limite
 * - LIMITED : limite fixe (min 1)
 */
public final class ScanLimit {

    private final Integer maxScans;

    private ScanLimit(Integer maxScans) {
        this.maxScans = maxScans;
    }

    public static ScanLimit unlimited() {
        return new ScanLimit(null);
    }

    public static ScanLimit of(int maxScans) {
        if (maxScans < 1) {
            throw new InvalidScanLimitException("La limite de scans doit être au moins 1 (reçu : " + maxScans + ")");
        }
        return new ScanLimit(maxScans);
    }

    public boolean isUnlimited() {
        return maxScans == null;
    }

    public boolean isReachedBy(int currentScanCount) {
        return maxScans != null && currentScanCount >= maxScans;
    }

    public java.util.OptionalInt maxScans() {
        return maxScans == null ? OptionalInt.empty() : OptionalInt.of(maxScans);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScanLimit s)) return false;
        return Objects.equals(maxScans, s.maxScans);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(maxScans);
    }

    public static final class InvalidScanLimitException extends DomainException {
        public InvalidScanLimitException(String m) { super(m); }
    }
}
