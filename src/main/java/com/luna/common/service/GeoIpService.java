package com.luna.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class GeoIpService {
    
    private static final String IP_API_URL = "http://ip-api.com/json/";
    private final RestTemplate restTemplate;
    
    public GeoIpService() {
        this.restTemplate = new RestTemplate();
    }
    
    public GeoIpInfo getGeoInfo(String ipAddress) {
        try {
            // Skip for localhost/private IPs
            if (isPrivateIp(ipAddress)) {
                log.debug("Skipping GeoIP lookup for private IP: {}", ipAddress);
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                IP_API_URL + ipAddress + "?fields=status,country,countryCode",
                Map.class
            );
            
            if (response != null && "success".equals(response.get("status"))) {
                return new GeoIpInfo(
                    (String) response.get("countryCode"),
                    (String) response.get("country")
                );
            }
            
            log.warn("GeoIP lookup failed for IP: {}", ipAddress);
            return null;
        } catch (Exception e) {
            log.error("Error during GeoIP lookup for IP: {}", ipAddress, e);
            return null;
        }
    }
    
    private boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) return true;
        return ip.equals("127.0.0.1") 
            || ip.equals("0:0:0:0:0:0:0:1")
            || ip.startsWith("192.168.")
            || ip.startsWith("10.")
            || ip.startsWith("172.16.")
            || ip.startsWith("172.17.")
            || ip.startsWith("172.18.")
            || ip.startsWith("172.19.")
            || ip.startsWith("172.2")
            || ip.startsWith("172.30.")
            || ip.startsWith("172.31.");
    }
    
    public record GeoIpInfo(String countryCode, String country) {}
}
