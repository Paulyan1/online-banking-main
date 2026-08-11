# start-local.ps1
# Start Online Banking local development environment

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$KeycloakHome = "C:\keycloak-26.7.1"
$RabbitContainer = "online-banking-rabbitmq"

Write-Host ""
Write-Host "========================================"
Write-Host " Starting Online Banking Platform"
Write-Host "========================================"
Write-Host ""

function Test-Port {
    param (
        [int]$Port
    )

    return Test-NetConnection `
        -ComputerName localhost `
        -Port $Port `
        -InformationLevel Quiet `
        -WarningAction SilentlyContinue
}

function Wait-ForPort {
    param (
        [int]$Port,
        [string]$Name,
        [int]$TimeoutSeconds = 60
    )

    Write-Host "Waiting for $Name on port $Port..."

    $elapsed = 0

    while ($elapsed -lt $TimeoutSeconds) {
        if (Test-Port $Port) {
            Write-Host "$Name is ready." -ForegroundColor Green
            return
        }

        Start-Sleep -Seconds 2
        $elapsed += 2
    }

    Write-Warning "$Name did not become ready within $TimeoutSeconds seconds."
}

function Start-App {
    param (
        [string]$Name,
        [string]$Directory,
        [string]$Command,
        [int]$Port
    )

    if (Test-Port $Port) {
        Write-Host "$Name already running on port $Port." -ForegroundColor Yellow
        return
    }

    Write-Host "Starting $Name..."

    Start-Process powershell `
        -WorkingDirectory $Directory `
        -ArgumentList "-NoExit", "-Command", $Command
}


# --------------------------------------------------
# PostgreSQL
# --------------------------------------------------

Write-Host "Checking PostgreSQL..."

if (Test-Port 5432) {
    Write-Host "PostgreSQL is running." -ForegroundColor Green
}
else {
    Write-Warning "PostgreSQL is NOT running on port 5432."
    Write-Warning "Start PostgreSQL before using the backend services."
}


# --------------------------------------------------
# RabbitMQ - Docker
# --------------------------------------------------

Write-Host ""
Write-Host "Checking Docker..."

docker info *> $null

if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop is not running."
}

Write-Host "Checking RabbitMQ container..."

$RabbitExists = docker ps -a --format "{{.Names}}" |
    Where-Object { $_ -eq $RabbitContainer }

if (-not $RabbitExists) {

    Write-Host "Creating RabbitMQ container..."

    docker run -d `
        --name $RabbitContainer `
        --restart unless-stopped `
        -p 5672:5672 `
        -p 15672:15672 `
        rabbitmq:3.13-management
}
else {

    $RabbitRunning = docker ps --format "{{.Names}}" |
        Where-Object { $_ -eq $RabbitContainer }

    if (-not $RabbitRunning) {
        Write-Host "Starting RabbitMQ container..."
        docker start $RabbitContainer
    }
    else {
        Write-Host "RabbitMQ already running." -ForegroundColor Yellow
    }
}

Wait-ForPort 5672 "RabbitMQ"


# --------------------------------------------------
# Keycloak
# --------------------------------------------------

Write-Host ""

if (Test-Port 8180) {

    Write-Host "Keycloak already running." -ForegroundColor Yellow

}
else {

    Write-Host "Starting Keycloak..."

    Start-Process powershell `
        -WorkingDirectory $KeycloakHome `
        -ArgumentList `
            "-NoExit",
            "-Command",
            ".\bin\kc.bat start-dev --http-port=8180"

    Wait-ForPort 8180 "Keycloak"
}


# --------------------------------------------------
# Backend Services
# --------------------------------------------------

Write-Host ""
Write-Host "Starting backend services..."

Start-App `
    "Account Service" `
    "$RootDir\backend\account-service" `
    ".\mvnw.cmd spring-boot:run" `
    8081

Start-App `
    "Transaction Service" `
    "$RootDir\backend\transaction-service" `
    ".\mvnw.cmd spring-boot:run" `
    8082

Start-App `
    "Notification Service" `
    "$RootDir\backend\notification-service" `
    ".\mvnw.cmd spring-boot:run" `
    8083

Start-App `
    "API Gateway" `
    "$RootDir\backend\api-gateway" `
    ".\mvnw.cmd spring-boot:run" `
    8080


# --------------------------------------------------
# Frontend
# --------------------------------------------------

Write-Host ""
Write-Host "Starting Angular frontend..."

Start-App `
    "Angular Frontend" `
    "$RootDir\frontend\web" `
    "npm start" `
    4200


# --------------------------------------------------
# Summary
# --------------------------------------------------

Write-Host ""
Write-Host "========================================"
Write-Host " Online Banking services are starting"
Write-Host "========================================"

Write-Host ""
Write-Host "Frontend:"
Write-Host "  http://localhost:4200"

Write-Host ""
Write-Host "Backend:"
Write-Host "  API Gateway:          http://localhost:8080"
Write-Host "  Account Service:      http://localhost:8081"
Write-Host "  Transaction Service:  http://localhost:8082"
Write-Host "  Notification Service: http://localhost:8083"

Write-Host ""
Write-Host "Infrastructure:"
Write-Host "  Keycloak:             http://localhost:8180"
Write-Host "  RabbitMQ UI:          http://localhost:15672"
Write-Host "  PostgreSQL:           localhost:5432"

Write-Host ""
Write-Host "RabbitMQ:"
Write-Host "  username: guest"
Write-Host "  password: guest"

Write-Host ""