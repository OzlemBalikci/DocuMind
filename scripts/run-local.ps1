# Proje kokunden: .\scripts\run-local.ps1
# .env dosyasini okur (Docker Compose $$ kurallari degil, duz sifre) ve spring-boot:run baslatir.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$envFile = Join-Path $root ".env"
if (-not (Test-Path $envFile)) {
    Write-Error ".env bulunamadi. .env.example dosyasini kopyalayip doldur."
}

function Get-DotEnvValue([string]$name) {
    foreach ($line in Get-Content $envFile) {
        if ($line -match '^\s*#') { continue }
        if ($line -match "^\s*$([regex]::Escape($name))=(.*)$") {
            $v = $Matches[1].Trim()
            # Docker Compose .env: $$ -> gercek $
            return ($v -replace '\$\$', '$')
        }
    }
    return $null
}

$pgPass = Get-DotEnvValue "local.postgres.password"
if (-not $pgPass) { $pgPass = Get-DotEnvValue "POSTGRES_PASSWORD" }
if (-not $pgPass) { Write-Error "local.postgres.password veya POSTGRES_PASSWORD .env icinde yok." }

$pgUser = Get-DotEnvValue "local.postgres.username"
if (-not $pgUser) { $pgUser = Get-DotEnvValue "POSTGRES_USER" }
if (-not $pgUser) { $pgUser = "interviewpilot_app" }

$jwt = Get-DotEnvValue "local.jwt.secret"
if (-not $jwt) { $jwt = Get-DotEnvValue "JWT_SECRET" }

$rbPass = Get-DotEnvValue "local.rabbitmq.password"
if (-not $rbPass) { $rbPass = Get-DotEnvValue "RABBITMQ_PASSWORD" }

# Spring Boot JDBC (ozel karakterli sifreler icin en guvenilir yol)
$env:SPRING_DATASOURCE_USERNAME = $pgUser
$env:SPRING_DATASOURCE_PASSWORD = $pgPass
if ($jwt) { $env:JWT_SECRET = $jwt; $env:LOCAL_JWT_SECRET = $jwt }
if ($rbPass) { $env:RABBITMQ_PASSWORD = $rbPass; $env:LOCAL_RABBITMQ_PASSWORD = $rbPass }

# Eski/bos Windows POSTGRES_PASSWORD Spring'i bozmasin
Remove-Item Env:POSTGRES_PASSWORD -ErrorAction SilentlyContinue

if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = "local" }
if (-not $env:JAVA_HOME) {
    $adoptium = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    if (Test-Path $adoptium) { $env:JAVA_HOME = $adoptium }
}

Write-Host "Starting backend (profile=$($env:SPRING_PROFILES_ACTIVE), db user=$pgUser)..."
& (Join-Path $root "mvnw.cmd") spring-boot:run @args
