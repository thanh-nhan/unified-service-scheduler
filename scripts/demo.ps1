# Demo sequence (AC-E03.4) against a running app seeded with dev fixture data.
# Usage: .\scripts\demo.ps1 [-BaseUrl http://localhost:8080]
# Works on Windows PowerShell 5.1 and PowerShell 7+.
param([string]$BaseUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

$Dealership  = "10000000-0000-0000-0000-000000000001"
$EvCheck     = "40000000-0000-0000-0000-000000000003"
$CustomerAn  = "50000000-0000-0000-0000-000000000001"
$VehicleAn   = "60000000-0000-0000-0000-000000000001"
$CustomerBinh = "50000000-0000-0000-0000-000000000002"
$VehicleBinh = "60000000-0000-0000-0000-000000000002"
$RunId = "demo-" + [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

# A future Monday-Saturday, so the dealership is open and the start time is not in the past.
$date = (Get-Date).Date.AddDays(7)
while ($date.DayOfWeek -eq [DayOfWeek]::Sunday) { $date = $date.AddDays(1) }
$Date = $date.ToString("yyyy-MM-dd")
$Start = "${Date}T09:00:00"

$client = New-Object System.Net.Http.HttpClient

# Returns @{ Status = <int>; Body = <parsed json or $null> }; non-2xx does not throw.
function Invoke-Step {
    param([string]$Label, [string]$Method, [string]$Path, [string]$Key = "", [string]$Json = "")
    $req = New-Object System.Net.Http.HttpRequestMessage ([System.Net.Http.HttpMethod]::new($Method), "$BaseUrl$Path")
    if ($Key) { [void]$req.Headers.TryAddWithoutValidation("Idempotency-Key", $Key) }
    if ($Json) { $req.Content = New-Object System.Net.Http.StringContent ($Json, [System.Text.Encoding]::UTF8, "application/json") }
    $resp = $client.SendAsync($req).GetAwaiter().GetResult()
    $text = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $status = [int]$resp.StatusCode
    Write-Host "== $Label -> HTTP $status"
    $body = $null
    if ($text) { try { $body = $text | ConvertFrom-Json } catch { Write-Host $text } }
    return @{ Status = $status; Body = $body }
}

function Show($obj) { $obj | ConvertTo-Json -Compress -Depth 5 | Write-Host }

function Booking-Json($customer, $vehicle) {
    @{ customerId = $customer; vehicleId = $vehicle; dealershipId = $Dealership
       serviceTypeId = $EvCheck; startTime = $Start } | ConvertTo-Json -Compress
}

Write-Host "Base URL: $BaseUrl   Slot: $Start"

$r = Invoke-Step "1. List service types" GET "/api/v1/service-types"
Show ($r.Body | ForEach-Object { [pscustomobject]@{ id = $_.id; code = $_.code; durationMinutes = $_.durationMinutes } })

$r = Invoke-Step "2. Availability on $Date" GET "/api/v1/dealerships/$Dealership/availability?serviceTypeId=$EvCheck&date=$Date"
Show ([pscustomobject]@{ date = $r.Body.date; durationMinutes = $r.Body.durationMinutes; slots = @($r.Body.slots).Count })

$r = Invoke-Step "3. Book (expect 201)" POST "/api/v1/appointments" "$RunId-1" (Booking-Json $CustomerAn $VehicleAn)
Show ([pscustomobject]@{ id = $r.Body.id; status = $r.Body.status; startTime = $r.Body.startTime; endTime = $r.Body.endTime; technicianId = $r.Body.technicianId; bayId = $r.Body.bayId })
$apptId = $r.Body.id

$r = Invoke-Step "4. Replay, same Idempotency-Key (expect 200)" POST "/api/v1/appointments" "$RunId-1" (Booking-Json $CustomerAn $VehicleAn)
Show ([pscustomobject]@{ id = $r.Body.id; status = $r.Body.status })

$r = Invoke-Step "5. Conflicting booking, different customer (expect 409)" POST "/api/v1/appointments" "$RunId-2" (Booking-Json $CustomerBinh $VehicleBinh)
Show ([pscustomobject]@{ status = $r.Body.status; code = $r.Body.code; detail = $r.Body.detail })

$r = Invoke-Step "6. Cancel (expect 200)" POST "/api/v1/appointments/$apptId/cancel"
Show ([pscustomobject]@{ id = $r.Body.id; status = $r.Body.status; cancelledAt = $r.Body.cancelledAt })

$r = Invoke-Step "7. Rebook the freed slot (expect 201)" POST "/api/v1/appointments" "$RunId-3" (Booking-Json $CustomerBinh $VehicleBinh)
Show ([pscustomobject]@{ id = $r.Body.id; status = $r.Body.status; startTime = $r.Body.startTime })
