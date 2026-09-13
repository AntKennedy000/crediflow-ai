import subprocess
from servidor import falar

try:
    audio = falar("Consulte o resumo de setembro de dois mil e vinte e seis.")
    assert audio[:4] == b"RIFF"
    print("WAV local valido:", len(audio), "bytes")
except subprocess.CalledProcessError as erro:
    print("Falha de sintese:", erro.stderr)
    raise SystemExit(1)
