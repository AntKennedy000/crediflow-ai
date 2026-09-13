"""Adaptador de audio local: Whisper CPU + voz SAPI do Windows. Sem API externa."""
import argparse
import io
import json
import os
os.environ.setdefault("OMP_NUM_THREADS", "2")
from pathlib import Path
import subprocess
import tempfile
from http.server import BaseHTTPRequestHandler, HTTPServer

RAIZ = Path(__file__).resolve().parent
MODELOS = RAIZ / "modelos"
MAX_BYTES = 10 * 1024 * 1024
modelo = None


def carregar_modelo(download=False):
    from faster_whisper import WhisperModel
    return WhisperModel("base", device="cpu", compute_type="int8", cpu_threads=2, num_workers=1,
                        download_root=str(MODELOS), local_files_only=not download)


def transcrever(conteudo):
    import av
    import numpy as np
    if not conteudo or len(conteudo) > MAX_BYTES:
        raise ValueError("Arquivo vazio ou maior que 10 MB.")
    # Decodifica em blocos e interrompe em 60 s, inclusive se a duracao no cabecalho faltar.
    partes = []
    amostras = 0
    with av.open(io.BytesIO(conteudo)) as container:
        resampler = av.AudioResampler(format="s16", layout="mono", rate=16000)
        for frame in container.decode(audio=0):
            for convertido in resampler.resample(frame):
                amostras += convertido.samples
                if amostras > 60 * 16000:
                    raise ValueError("Limite de 60 segundos por audio.")
                partes.append(convertido.to_ndarray().reshape(-1))
        for convertido in resampler.resample(None):
            amostras += convertido.samples
            if amostras > 60 * 16000:
                raise ValueError("Limite de 60 segundos por audio.")
            partes.append(convertido.to_ndarray().reshape(-1))
    if not partes:
        raise ValueError("Audio sem amostras.")
    audio = np.concatenate(partes).astype(np.float32) / 32768.0
    segments, _ = modelo.transcribe(audio, language="pt", beam_size=5, vad_filter=True,
                                   condition_on_previous_text=False)
    texto = " ".join(segment.text.strip() for segment in segments).strip()
    if not texto or len(texto) > 1000:
        raise ValueError("Nao foi detectada fala valida de ate 1000 caracteres.")
    return texto


def falar(texto):
    if not isinstance(texto, str) or not texto.strip() or len(texto) > 1200:
        raise ValueError("Texto invalido.")
    with tempfile.TemporaryDirectory(prefix="crediflow-voz-") as pasta:
        saida = Path(pasta) / "voz.wav"
        powershell = str(Path(os.environ.get("SystemRoot", r"C:\Windows")) /
                         "System32/WindowsPowerShell/v1.0/powershell.exe")
        # Argumentos separados; o texto entra por stdin, nunca como codigo PowerShell.
        subprocess.run([powershell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                        str(RAIZ / "falar.ps1"), "-ArquivoSaida", str(saida)],
                       input=texto, encoding="utf-8", errors="replace", capture_output=True, check=True,
                       timeout=60, creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        return saida.read_bytes()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # Nao registrar transcricoes, textos ou audios.

    def enviar(self, status, conteudo, tipo="application/json"):
        if not isinstance(conteudo, bytes):
            conteudo = json.dumps(conteudo, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(conteudo)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(conteudo)

    def do_GET(self):
        if self.path == "/status":
            self.enviar(200, {"status": "AUDIO_LOCAL", "transcritor": "Whisper base CPU", "sintese": "Windows SAPI"})
        else:
            self.enviar(404, {"erro": "NAO_ENCONTRADO"})

    def do_POST(self):
        try:
            tamanho = int(self.headers.get("Content-Length", "0"))
            if tamanho <= 0 or tamanho > MAX_BYTES:
                self.enviar(413, {"erro": "TAMANHO_INVALIDO"})
                return
            self.connection.settimeout(30)
            dados = self.rfile.read(tamanho)
            if len(dados) != tamanho:
                raise ValueError("Corpo incompleto.")
            if self.path == "/transcrever":
                self.enviar(200, {"texto": transcrever(dados)})
            elif self.path == "/falar":
                self.enviar(200, falar(json.loads(dados)["texto"]), "audio/wav")
            else:
                self.enviar(404, {"erro": "NAO_ENCONTRADO"})
        except (ValueError, KeyError):
            self.enviar(400, {"erro": "AUDIO_OU_TEXTO_INVALIDO"})
        except Exception:
            self.enviar(503, {"erro": "FALHA_AUDIO_LOCAL"})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--baixar-modelo", action="store_true")
    parser.add_argument("--porta", type=int, default=8008)
    args = parser.parse_args()
    modelo = carregar_modelo(download=args.baixar_modelo)
    if args.baixar_modelo:
        print("Modelo base baixado. As proximas execucoes usam apenas arquivos locais.")
    else:
        print(f"Audio local em http://127.0.0.1:{args.porta}. Ctrl+C encerra.", flush=True)
        HTTPServer(("127.0.0.1", args.porta), Handler).serve_forever()
