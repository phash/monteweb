package com.monteweb.shared.util;

import com.monteweb.admin.AdminModuleApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Communicates with a ClamAV daemon via the INSTREAM protocol over TCP.
 * Checks uploaded file data for viruses.
 */
@Service
public class ClamAvService {

    private static final Logger log = LoggerFactory.getLogger(ClamAvService.class);
    private static final int CHUNK_SIZE = 2048;
    private static final int SOCKET_TIMEOUT_MS = 30_000;

    private final AdminModuleApi adminModuleApi;

    public ClamAvService(AdminModuleApi adminModuleApi) {
        this.adminModuleApi = adminModuleApi;
    }

    /**
     * Result of a ClamAV virus scan.
     */
    public record ScanResult(boolean isClean, String virusName) {

        public static ScanResult clean() {
            return new ScanResult(true, null);
        }

        public static ScanResult infected(String virusName) {
            return new ScanResult(false, virusName);
        }
    }

    /**
     * Returns whether ClamAV scanning is enabled (via modules map).
     */
    public boolean isEnabled() {
        return adminModuleApi.isModuleEnabled("clamav");
    }

    /**
     * Checks if the ClamAV daemon is reachable by sending a PING command.
     */
    public boolean isAvailable() {
        if (!isEnabled()) {
            return false;
        }
        var config = adminModuleApi.getTenantConfig();
        try (Socket socket = createSocket(config.clamavHost(), config.clamavPort())) {
            OutputStream out = socket.getOutputStream();
            out.write("zPING\0".getBytes(StandardCharsets.US_ASCII));
            out.flush();
            byte[] response = socket.getInputStream().readAllBytes();
            String responseStr = new String(response, StandardCharsets.US_ASCII).trim();
            return "PONG".equals(responseStr);
        } catch (Exception e) {
            log.warn("ClamAV not available at {}:{}: {}", config.clamavHost(), config.clamavPort(), e.getMessage());
            return false;
        }
    }

    /**
     * Scans the given byte array for viruses using the ClamAV INSTREAM protocol.
     * If ClamAV is not enabled, returns a clean result without scanning.
     */
    public ScanResult scan(byte[] data) {
        if (!isEnabled()) {
            return ScanResult.clean();
        }
        var config = adminModuleApi.getTenantConfig();
        return scanWithDaemon(data, config.clamavHost(), config.clamavPort());
    }

    private ScanResult scanWithDaemon(byte[] data, String host, int port) {
        try (Socket socket = createSocket(host, port)) {
            OutputStream out = socket.getOutputStream();

            // Send INSTREAM command (null-terminated)
            out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

            // Send data in chunks: 4-byte big-endian length prefix + chunk data
            InputStream dataStream = new ByteArrayInputStream(data);
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = dataStream.read(buffer)) > 0) {
                out.write(ByteBuffer.allocate(4).putInt(bytesRead).array());
                out.write(buffer, 0, bytesRead);
            }

            // Send zero-length chunk to signal end of stream
            out.write(new byte[]{0, 0, 0, 0});
            out.flush();

            // Read response
            byte[] response = socket.getInputStream().readAllBytes();
            String responseStr = new String(response, StandardCharsets.US_ASCII).trim();

            log.debug("ClamAV response: {}", responseStr);

            if (responseStr.endsWith("OK")) {
                return ScanResult.clean();
            }
            if (responseStr.contains("FOUND")) {
                // Response format: "stream: <virusname> FOUND"
                String virusName = extractVirusName(responseStr);
                log.warn("ClamAV detected virus: {}", virusName);
                return ScanResult.infected(virusName);
            }

            // Unexpected response — treat as error but not as virus
            log.error("Unexpected ClamAV response: {}", responseStr);
            return ScanResult.clean();
        } catch (Exception e) {
            log.error("ClamAV scan failed: {}", e.getMessage(), e);
            // On error, allow upload rather than blocking — log the failure
            return ScanResult.clean();
        }
    }

    private String extractVirusName(String response) {
        // Format: "stream: <virusname> FOUND"
        String withoutPrefix = response.replace("stream:", "").trim();
        if (withoutPrefix.endsWith("FOUND")) {
            return withoutPrefix.substring(0, withoutPrefix.length() - "FOUND".length()).trim();
        }
        return withoutPrefix;
    }

    Socket createSocket(String host, int port) throws java.io.IOException {
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        return socket;
    }
}
