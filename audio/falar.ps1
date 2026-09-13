param([Parameter(Mandatory=$true)][string]$ArquivoSaida)
$ErrorActionPreference = 'Stop'
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$textoVoz = [Console]::In.ReadToEnd()
if ([string]::IsNullOrWhiteSpace($textoVoz) -or $textoVoz.Length -gt 1200) { throw 'Texto invalido.' }

# SAPI nativo evita a dependencia de uma janela/dispatcher do System.Speech.
$voz = New-Object -ComObject SAPI.SpVoice
$arquivo = New-Object -ComObject SAPI.SpFileStream
try {
    try {
        for ($i = 0; $i -lt $voz.GetVoices().Count; $i++) {
            $token = $voz.GetVoices().Item($i)
            if ($token.GetAttribute('Language') -match '0416|pt-BR') { $voz.Voice = $token; break }
        }
    } catch { }
    # 3 = SSFMCreateForWrite; formato WAV padrao do SAPI.
    $arquivo.Open($ArquivoSaida, 3, $false)
    $voz.AudioOutputStream = $arquivo
    [void]$voz.Speak($textoVoz)
} finally {
    if ($arquivo) { $arquivo.Close() }
    if ($voz) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($voz) | Out-Null }
    if ($arquivo) { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($arquivo) | Out-Null }
}
