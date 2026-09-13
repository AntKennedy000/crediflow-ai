package br.com.crediflow.service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AudioService {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String baseUrl;
    public AudioService(@Value("${crediflow.audio.url:http://127.0.0.1:8008}") String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (!"http".equals(uri.getScheme()) || !"127.0.0.1".equals(uri.getHost()))
            throw new IllegalArgumentException("Audio deve usar somente localhost.");
        this.baseUrl = baseUrl;
    }
    private byte[] enviar(String rota, byte[] dados, String tipo) {
        try {
            var request = HttpRequest.newBuilder(URI.create(baseUrl + rota)).timeout(Duration.ofMinutes(3))
                .header("Content-Type",tipo).POST(HttpRequest.BodyPublishers.ofByteArray(dados)).build();
            var resposta = http.send(request,HttpResponse.BodyHandlers.ofByteArray());
            if (resposta.statusCode() == 400 || resposta.statusCode() == 413)
                throw new IllegalArgumentException("Audio ou texto invalido.");
            if (resposta.statusCode() != 200) throw new IllegalStateException("Adaptador de audio indisponivel.");
            return resposta.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Audio interrompido.",e);
        } catch (java.io.IOException e) { throw new IllegalStateException("Audio local indisponivel.",e); }
    }
    public String transcrever(byte[] arquivo) {
        if (arquivo.length == 0 || arquivo.length > 10*1024*1024) throw new IllegalArgumentException("Audio invalido.");
        String texto = json.readTree(enviar("/transcrever",arquivo,"application/octet-stream")).path("texto").asText();
        if (texto.isBlank() || texto.length() > 1000) throw new IllegalArgumentException("Transcricao invalida.");
        return texto;
    }
    public byte[] falar(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 1200) throw new IllegalArgumentException("Texto invalido.");
        return enviar("/falar",json.writeValueAsBytes(Map.of("texto",texto)),"application/json");
    }
}
