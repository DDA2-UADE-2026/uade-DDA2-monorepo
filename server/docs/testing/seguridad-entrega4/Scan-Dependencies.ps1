param([Parameter(Mandatory=$true)][string]$Inventory, [Parameter(Mandatory=$true)][string]$Output)
$ErrorActionPreference = 'Stop'
$packages = @(Get-Content -LiteralPath $Inventory -Raw | ConvertFrom-Json)
if ($packages.Count -eq 0) { throw 'Empty dependency inventory' }
$queries = @($packages | ForEach-Object { @{package=@{ecosystem='Maven';name=$_.name};version=$_.version} })
$body = @{queries=$queries} | ConvertTo-Json -Depth 5
$response = Invoke-RestMethod -Uri 'https://api.osv.dev/v1/querybatch' -Method Post -ContentType 'application/json' -Body $body
if ($response.results.Count -ne $packages.Count) { throw 'Incomplete OSV response' }
$findings = @(for ($i=0; $i -lt $packages.Count; $i++) {
    if ($response.results[$i].next_page_token) { throw 'OSV response requires pagination; do not treat it as complete' }
    if ($response.results[$i].vulns) {
        @{package=$packages[$i].name;version=$packages[$i].version;ids=@($response.results[$i].vulns.id)}
    }
})
@{date=(Get-Date -Format o);tool='OSV querybatch API v1';scope='Gradle runtimeClasspath; Maven ecosystem';packages=$packages.Count;findings=$findings} |
    ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Output -Encoding utf8
Write-Output "Packages: $($packages.Count); packages with advisories: $($findings.Count)"
if ($findings.Count -gt 0) { exit 1 }
