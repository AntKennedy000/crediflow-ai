$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not (Test-Path '.venv\Scripts\python.exe')) { throw 'Prepare o ambiente Python conforme o README.' }
& .\.venv\Scripts\python.exe audio\servidor.py
