package com.example.ip_asset_management.service;

import com.profesorfalken.wmi4java.WMI4Java;
import com.profesorfalken.wmi4java.WMIException;
import jcifs.CIFSContext;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Service
public class WindowsScannerService {
    private static final Logger logger = LoggerFactory.getLogger(WindowsScannerService.class);
    
    @Value("${scan.windows.username:}")
    private String windowsUsername;
    
    @Value("${scan.windows.password:}")
    private String windowsPassword;
    
    @Value("${scan.windows.domain:}")
    private String windowsDomain;
    
    /**
     * Gets detailed system information from a Windows machine.
     * 
     * @param ipAddress The IP address of the Windows machine
     * @return A map containing system information, or empty map if retrieval fails
     */
    public Map<String, Object> getWindowsSystemInfo(String ipAddress) {
        Map<String, Object> systemInfo = new HashMap<>();
        
        // First try WMI for remote Windows machines
        try {
            if (tryWmiConnection(ipAddress, systemInfo)) {
                logger.info("Successfully retrieved Windows system info via WMI for {}", ipAddress);
                return systemInfo;
            }
        } catch (Exception e) {
            logger.warn("WMI connection failed for {}: {}", ipAddress, e.getMessage());
        }
        
        // Fallback to SMB/CIFS
        try {
            if (trySmbConnection(ipAddress, systemInfo)) {
                logger.info("Successfully retrieved Windows system info via SMB for {}", ipAddress);
                return systemInfo;
            }
        } catch (Exception e) {
            logger.warn("SMB connection failed for {}: {}", ipAddress, e.getMessage());
        }
        
        // Fallback to PowerShell remoting
        try {
            if (tryPowerShellRemoting(ipAddress, systemInfo)) {
                logger.info("Successfully retrieved Windows system info via PowerShell remoting for {}", ipAddress);
                return systemInfo;
            }
        } catch (Exception e) {
            logger.warn("PowerShell remoting failed for {}: {}", ipAddress, e.getMessage());
        }
        
        // If all methods failed, try using local system commands if running on Windows
        try {
            if (isWindowsOS() && tryLocalCommands(ipAddress, systemInfo)) {
                logger.info("Retrieved basic Windows system info via local commands for {}", ipAddress);
                return systemInfo;
            }
        } catch (Exception e) {
            logger.warn("Local Windows commands failed for {}: {}", ipAddress, e.getMessage());
        }
        
        // If all else fails, use port scanning to determine as much as possible
        try {
            if (determineInfoFromPorts(ipAddress, systemInfo)) {
                logger.info("Determined basic system info from port scanning for {}", ipAddress);
                return systemInfo;
            }
        } catch (Exception e) {
            logger.warn("Port-based info determination failed for {}: {}", ipAddress, e.getMessage());
        }
        
        return systemInfo;
    }
    
    /**
     * Try to connect to the remote Windows machine using WMI4Java.
     */
    private boolean tryWmiConnection(String ipAddress, Map<String, Object> systemInfo) {
        try {
            // Configure WMI4Java for remote connection
            WMI4Java wmi = WMI4Java.get()
                .computerName(ipAddress);
            
            // Add credentials if provided
            if (windowsUsername != null && !windowsUsername.isEmpty() && 
                windowsPassword != null && !windowsPassword.isEmpty()) {
                // wmi.credentials(windowsDomain, windowsUsername, windowsPassword);
            }
            
            // Get operating system information
            Map<String, String> osInfo = wmi.getWMIObject("Win32_OperatingSystem");
            if (osInfo != null && !osInfo.isEmpty()) {
                systemInfo.put("osName", osInfo.getOrDefault("Caption", "Windows"));
                systemInfo.put("osVersion", osInfo.getOrDefault("Version", "Unknown"));
                systemInfo.put("osArchitecture", osInfo.getOrDefault("OSArchitecture", "Unknown"));
                systemInfo.put("lastBootTime", osInfo.getOrDefault("LastBootUpTime", "Unknown"));
                
                // Convert memory values
                try {
                    String totalVisibleMemory = osInfo.get("TotalVisibleMemorySize");
                    if (totalVisibleMemory != null) {
                        long memory = Long.parseLong(totalVisibleMemory);
                        double memoryGB = memory / 1024.0 / 1024.0;
                        systemInfo.put("ramSize", String.format("%.2f GB", memoryGB));
                    }
                } catch (NumberFormatException e) {
                    systemInfo.put("ramSize", "Unknown");
                }
            }
            
            // Get CPU information
            Map<String, String> cpuInfo = wmi.getWMIObject("Win32_Processor");
            if (cpuInfo != null && !cpuInfo.isEmpty()) {
                systemInfo.put("cpuModel", cpuInfo.getOrDefault("Name", "Unknown"));
                systemInfo.put("cpuCores", cpuInfo.getOrDefault("NumberOfCores", "Unknown"));
                systemInfo.put("cpuThreads", cpuInfo.getOrDefault("NumberOfLogicalProcessors", "Unknown"));
                systemInfo.put("cpuManufacturer", cpuInfo.getOrDefault("Manufacturer", "Unknown"));
            }
            
            // Get GPU information
            Map<String, String> gpuInfo = wmi.getWMIObject("Win32_VideoController");
            if (gpuInfo != null && !gpuInfo.isEmpty()) {
                systemInfo.put("gpuName", gpuInfo.getOrDefault("Name", "Unknown"));
                systemInfo.put("gpuDriver", gpuInfo.getOrDefault("DriverVersion", "Unknown"));
                systemInfo.put("gpuMemory", gpuInfo.getOrDefault("AdapterRAM", "Unknown"));
            }
            
            // Get system information
            Map<String, String> systemData = wmi.getWMIObject("Win32_ComputerSystem");
            if (systemData != null && !systemData.isEmpty()) {
                systemInfo.put("manufacturer", systemData.getOrDefault("Manufacturer", "Unknown"));
                systemInfo.put("model", systemData.getOrDefault("Model", "Unknown"));
                systemInfo.put("systemType", systemData.getOrDefault("SystemType", "Unknown"));
                systemInfo.put("domain", systemData.getOrDefault("Domain", "Unknown"));
            }
            
            return !systemInfo.isEmpty();
        } catch (WMIException e) {
            logger.error("WMI error for {}: {}", ipAddress, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error in WMI scan for {}: {}", ipAddress, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try to connect to the remote Windows machine using SMB/CIFS.
     */
    private boolean trySmbConnection(String ipAddress, Map<String, Object> systemInfo) {
        try {
            // Create SMB context with credentials if available
            CIFSContext context;
            if (windowsUsername != null && !windowsUsername.isEmpty() && 
                windowsPassword != null && !windowsPassword.isEmpty()) {
                NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(
                    windowsDomain, windowsUsername, windowsPassword);
                context = new BaseContext(null).withCredentials(auth);
            } else {
                context = new BaseContext(null);
            }
            
            // Try to connect to the admin share to check accessibility
            SmbFile smbFile = new SmbFile(String.format("smb://%s/C$/", ipAddress), context);
            boolean accessible = smbFile.exists();
            
            if (accessible) {
                systemInfo.put("smbAccessible", true);
                systemInfo.put("assetType", "WINDOWS");
                
                // Try to retrieve more information if possible
                // Note: Getting detailed info via SMB is limited without additional tools
                // This is a placeholder for more advanced implementations
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("SMB connection failed for {}: {}", ipAddress, e.getMessage());
            return false;
        }
    }
    
    /**
     * Try to use PowerShell remoting to connect to the Windows machine.
     */
    private boolean tryPowerShellRemoting(String ipAddress, Map<String, Object> systemInfo) {
        try {
            StringBuilder psCommand = new StringBuilder();
            psCommand.append("powershell -Command \"");
            
            // Add authentication if credentials are provided
            if (windowsUsername != null && !windowsUsername.isEmpty() && 
                windowsPassword != null && !windowsPassword.isEmpty()) {
                psCommand.append("$username = '").append(windowsUsername).append("'; ");
                psCommand.append("$password = '").append(windowsPassword).append("' | ConvertTo-SecureString -AsPlainText -Force; ");
                psCommand.append("$cred = New-Object System.Management.Automation.PSCredential($username, $password); ");
                
                psCommand.append("Invoke-Command -ComputerName ").append(ipAddress)
                         .append(" -Credential $cred -ScriptBlock {");
            } else {
                psCommand.append("Invoke-Command -ComputerName ").append(ipAddress)
                         .append(" -ScriptBlock {");
            }
            
            // Script to gather system information
            psCommand.append("$os = Get-WmiObject -Class Win32_OperatingSystem; ");
            psCommand.append("$cs = Get-WmiObject -Class Win32_ComputerSystem; ");
            psCommand.append("$cpu = Get-WmiObject -Class Win32_Processor; ");
            psCommand.append("$gpu = Get-WmiObject -Class Win32_VideoController; ");
            
            psCommand.append("$result = [PSCustomObject]@{");
            psCommand.append("  OSName = $os.Caption; ");
            psCommand.append("  OSVersion = $os.Version; ");
            psCommand.append("  OSArchitecture = $os.OSArchitecture; ");
            psCommand.append("  MemoryGB = [math]::Round($cs.TotalPhysicalMemory / 1GB, 2); ");
            psCommand.append("  CPUModel = $cpu.Name; ");
            psCommand.append("  CPUCores = $cpu.NumberOfCores; ");
            psCommand.append("  Manufacturer = $cs.Manufacturer; ");
            psCommand.append("  Model = $cs.Model; ");
            psCommand.append("  GPUName = $gpu.Name; ");
            psCommand.append("}; ");
            
            psCommand.append("$result | ConvertTo-Json");
            psCommand.append("}\"");
            
            Process process = Runtime.getRuntime().exec(psCommand.toString());
            if (process.waitFor(30, TimeUnit.SECONDS)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
                
                // Parse the JSON output
                String jsonOutput = output.toString();
                if (jsonOutput.contains("OSName") && jsonOutput.contains("CPUModel")) {
                    // Basic manual parsing of JSON output
                    // In a real implementation, use a proper JSON library like Jackson
                    systemInfo.put("osName", extractJsonValue(jsonOutput, "OSName"));
                    systemInfo.put("osVersion", extractJsonValue(jsonOutput, "OSVersion"));
                    systemInfo.put("ramSize", extractJsonValue(jsonOutput, "MemoryGB") + " GB");
                    systemInfo.put("cpuModel", extractJsonValue(jsonOutput, "CPUModel"));
                    systemInfo.put("cpuCores", extractJsonValue(jsonOutput, "CPUCores"));
                    systemInfo.put("manufacturer", extractJsonValue(jsonOutput, "Manufacturer"));
                    systemInfo.put("model", extractJsonValue(jsonOutput, "Model"));
                    systemInfo.put("gpuName", extractJsonValue(jsonOutput, "GPUName"));
                    
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("PowerShell remoting failed for {}: {}", ipAddress, e.getMessage());
            return false;
        }
    }
    
    /**
     * Very basic JSON value extractor - in real code, use a proper JSON library.
     */
    private String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(searchPattern);
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
    }
    
    /**
     * Try using local Windows system commands if running on a Windows machine.
     */
    private boolean tryLocalCommands(String ipAddress, Map<String, Object> systemInfo) {
        try {
            // This will only work if the scanner is running on Windows
            if (!isWindowsOS()) {
                return false;
            }
            
            // Try to use net commands to get basic info
            Process process = Runtime.getRuntime().exec("cmd.exe /c net view \\\\" + ipAddress);
            if (process.waitFor(5, TimeUnit.SECONDS)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                
                if (output.toString().contains("Share name")) {
                    systemInfo.put("networked", true);
                    systemInfo.put("assetType", "WINDOWS");
                    
                    // Try to get OS version
                    Process systemInfoProcess = Runtime.getRuntime().exec(
                            "cmd.exe /c systeminfo /s " + ipAddress);
                    if (systemInfoProcess.waitFor(10, TimeUnit.SECONDS)) {
                        reader = new BufferedReader(new InputStreamReader(systemInfoProcess.getInputStream()));
                        output = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                            
                            if (line.contains("OS Name")) {
                                systemInfo.put("osName", line.split(":\\s+")[1].trim());
                            } else if (line.contains("OS Version")) {
                                systemInfo.put("osVersion", line.split(":\\s+")[1].trim());
                            } else if (line.contains("System Manufacturer")) {
                                systemInfo.put("manufacturer", line.split(":\\s+")[1].trim());
                            } else if (line.contains("System Model")) {
                                systemInfo.put("model", line.split(":\\s+")[1].trim());
                            } else if (line.contains("Total Physical Memory")) {
                                systemInfo.put("ramSize", line.split(":\\s+")[1].trim());
                            }
                        }
                    }
                    
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("Local command execution failed for {}: {}", ipAddress, e.getMessage());
            return false;
        }
    }
    
    /**
     * Determine if scanning host is running Windows.
     */
    private boolean isWindowsOS() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }
    
    /**
     * If other methods fail, try to gather info based on open ports.
     */
    private boolean determineInfoFromPorts(String ipAddress, Map<String, Object> systemInfo) {
        try {
            // Check common Windows ports
            boolean rdpOpen = isPortOpen(ipAddress, 3389);
            boolean smbOpen = isPortOpen(ipAddress, 445);
            boolean netbiosOpen = isPortOpen(ipAddress, 139);
            
            // If typical Windows ports are open, it's likely Windows
            if (rdpOpen || smbOpen || netbiosOpen) {
                systemInfo.put("likelyWindows", true);
                systemInfo.put("assetType", "WINDOWS");
                
                if (rdpOpen) systemInfo.put("rdpEnabled", true);
                if (smbOpen) systemInfo.put("smbEnabled", true);
                if (netbiosOpen) systemInfo.put("netbiosEnabled", true);
                
                return true;
            }
            
            return false;
        } catch (Exception e) {
            logger.warn("Port-based detection failed for {}: {}", ipAddress, e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a specific port is open.
     */
    private boolean isPortOpen(String ip, int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), 1000); // 1 second timeout
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attempts basic SSH-based scanning for non-Windows systems
     * Particularly useful for Mac and Linux systems
     * 
     * @param ipAddress Target IP address
     * @return Map containing discovered information
     */
    public Map<String, Object> attemptBasicSshScan(String ipAddress) {
        Map<String, Object> results = new HashMap<>();
        
        // Check if SSH port is open
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, 22), 1000);
            if (socket.isConnected()) {
                results.put("sshAvailable", true);
                results.put("hostType", "Likely Unix/Linux/Mac");
                
                // For Mac systems, we can attempt some Banner grabbing
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    
                    // Wait briefly for SSH banner
                    String banner = reader.readLine();
                    if (banner != null && !banner.isEmpty()) {
                        results.put("sshBanner", banner);
                        
                        if (banner.toLowerCase().contains("mac") || banner.toLowerCase().contains("darwin")) {
                            results.put("osType", "macOS");
                        } else if (banner.toLowerCase().contains("ubuntu")) {
                            results.put("osType", "Ubuntu Linux");
                        } else if (banner.toLowerCase().contains("debian")) {
                            results.put("osType", "Debian Linux");
                        }
                    }
                } catch (Exception e) {
                    // Banner grabbing failed, but we know SSH is available
                }
            }
        } catch (Exception e) {
            results.put("sshAvailable", false);
        }
        
        return results;
    }
}