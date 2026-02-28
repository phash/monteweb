-- DSGVO Fix: Jitsi default-URL entfernen (Art. 44 – Drittlandübermittlung)
-- Setzt jitsi_server_url auf NULL für Instanzen, die nie manuell konfiguriert wurden.
-- Damit wird verhindert, dass Daten ohne Rechtsgrundlage an meet.jit.si (USA) übertragen werden.
UPDATE tenant_config
SET jitsi_server_url = NULL
WHERE jitsi_server_url = 'https://meet.jit.si';
