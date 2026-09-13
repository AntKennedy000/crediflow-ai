package br.com.crediflow.controller;

import br.com.crediflow.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AudioController {
    private final AudioService audio;
    private final AssistenteService assistente;
    public AudioController(AudioService audio, AssistenteService assistente) { this.audio=audio;this.assistente=assistente; }
    public record TextoVoz(@NotBlank @Size(max=1200) String texto) { }
    @PostMapping(value="/audio/voz",produces="audio/wav")
    public byte[] falar(@Valid @RequestBody TextoVoz pedido) { return audio.falar(pedido.texto()); }
    @PostMapping(value="/audio/transcricao",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,String> transcrever(@RequestParam("arquivo") MultipartFile arquivo) throws java.io.IOException {
        return Map.of("texto",audio.transcrever(arquivo.getBytes()));
    }
    @PostMapping(value="/assistente/audio",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Processado processar(@RequestParam("arquivo") MultipartFile arquivo) throws java.io.IOException {
        String texto=audio.transcrever(arquivo.getBytes());
        var resposta=assistente.conversar(texto);
        // Falha no TTS nao elimina um rascunho ja preparado nem induz repeticao silenciosa.
        try {
            String voz=Base64.getEncoder().encodeToString(audio.falar(resposta.mensagem()));
            return new Processado(texto,resposta,voz,null);
        } catch (RuntimeException e) {
            return new Processado(texto,resposta,null,"Resposta em texto disponivel; a sintese local falhou.");
        }
    }
    public record Processado(String transcricao, AssistenteService.Resposta resposta, String audioBase64, String avisoAudio) { }
}
