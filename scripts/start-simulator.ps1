# Sobe o simulador de garagem do enunciado (cfontes0estapar/garage-sim:1.0.0).
#
# Uso: .\scripts\start-simulator.ps1 [-Mode host|port]
#   host  (padrão) comando do enunciado, com --network="host".
#         No Docker Desktop exige "Enable host networking" (Settings > Resources > Network).
#   port  plano B: publica a porta 3000 e envia o webhook para host.docker.internal:3003.
#
# O simulador só começa a enviar eventos depois da primeira chamada a GET /garage,
# que a aplicação faz ao subir.
param(
    [ValidateSet("host", "port")]
    [string]$Mode = "host"
)

$ErrorActionPreference = "Stop"
$Image = "cfontes0estapar/garage-sim:1.0.0"
$Name = "garage-sim"

docker rm -f $Name 2>$null | Out-Null

if ($Mode -eq "host") {
    docker run -d --name $Name --network="host" $Image
} else {
    docker run -d --name $Name -p 3000:3000 `
        -e CLIENT_WEBHOOK_URL=http://host.docker.internal:3003/webhook `
        $Image
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Simulador no modo '$Mode': GET http://localhost:3000/garage"
Write-Host "Eventos enviados para a aplicação em http://localhost:3003/webhook"
Write-Host "Logs: docker logs -f $Name | Parar: docker rm -f $Name"
