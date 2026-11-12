[CmdletBinding()]
param([int64]$ChunkSize = 16MB)
$ErrorActionPreference = 'Stop'
$uri = 'https://download.geofabrik.de/asia/india/northern-zone-latest.osm.pbf'
$checksumUri = "$uri.md5"
$out = Join-Path $PSScriptRoot '../../data/transit/raw/northern-zone-latest.osm.pbf'
$out = [IO.Path]::GetFullPath($out); $partial = "$out.partial"
New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($out)) | Out-Null
if ((Test-Path $out) -and -not (Test-Path $partial)) { Move-Item -LiteralPath $out -Destination $partial }
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new(); $client.Timeout = [TimeSpan]::FromMinutes(10)
try {
  $head = $client.SendAsync([System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Head,$uri)).GetAwaiter().GetResult()
  if (-not $head.IsSuccessStatusCode -or -not $head.Content.Headers.ContentLength) { throw "Unable to read the PBF Content-Length (HTTP $([int]$head.StatusCode))." }
  $total = [int64]$head.Content.Headers.ContentLength
  $range = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get,$uri); $range.Headers.Range = [System.Net.Http.Headers.RangeHeaderValue]::new(0,0)
  $probe = $client.SendAsync($range).GetAwaiter().GetResult(); if ($probe.StatusCode -ne 206) { throw "Server does not support byte ranges (HTTP $([int]$probe.StatusCode))." }; $probe.Dispose()
  $existing = if (Test-Path $partial) {[IO.FileInfo]$partial | Select-Object -ExpandProperty Length} else {0L}
  if ($existing -gt $total) { throw "Partial file is larger than Content-Length; refusing to overwrite it." }
  while ($existing -lt $total) {
    $end = [Math]::Min($existing + $ChunkSize - 1, $total - 1); $ok = $false
    for ($attempt=1; $attempt -le 4 -and -not $ok; $attempt++) { try {
      $req=[System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Get,$uri); $req.Headers.Range=[System.Net.Http.Headers.RangeHeaderValue]::new($existing,$end); $res=$client.SendAsync($req).GetAwaiter().GetResult()
      if ($res.StatusCode -ne 206 -or -not $res.Content.Headers.ContentRange -or $res.Content.Headers.ContentRange.From -ne $existing) { throw "Invalid Content-Range response: HTTP $([int]$res.StatusCode)." }
      $bytes=$res.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
      $stream=[IO.File]::Open($partial,[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::Write,[IO.FileShare]::Read);$stream.Seek($existing,[IO.SeekOrigin]::Begin)|Out-Null;$stream.Write($bytes,0,$bytes.Length);$stream.Dispose();$res.Dispose();$existing += $bytes.Length;$ok=$true;Write-Progress -Activity 'Downloading Geofabrik PBF' -Status "$existing / $total bytes" -PercentComplete (($existing/$total)*100)
    } catch { if($attempt -eq 4){throw}; Start-Sleep -Seconds ($attempt*3) } }
  }
  $hash=(Get-FileHash $partial -Algorithm MD5).Hash.ToLowerInvariant(); $published=($client.GetStringAsync($checksumUri).GetAwaiter().GetResult() -split '\s+')[0].ToLowerInvariant(); if($hash -ne $published){throw "MD5 mismatch. Local $hash; published $published."}; Move-Item -Force -LiteralPath $partial -Destination $out; Write-Host "Verified $out ($total bytes), MD5 $hash"
} finally {$client.Dispose()}
