package com.backend.pedidos_app.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file, String bucketName) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType()));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
        } else {
            throw new RuntimeException("Error al subir el archivo: " + response.getBody());
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            // Extraer el bucket y el nombre del archivo de la URL
            String publicPrefix = supabaseUrl + "/storage/v1/object/public/";
            if (!fileUrl.startsWith(publicPrefix)) {
                throw new IllegalArgumentException("URL de archivo no válida: " + fileUrl);
            }
            
            String path = fileUrl.substring(publicPrefix.length());
            String[] parts = path.split("/", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("URL de archivo no válida: " + fileUrl);
            }
            
            String bucketName = parts[0];
            String fileName = parts[1];
            
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    requestEntity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Error al eliminar el archivo: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar el archivo: " + fileUrl, e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);
        headers.set("apikey", supabaseKey);
        return headers;
    }
}