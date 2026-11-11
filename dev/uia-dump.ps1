<#
.SYNOPSIS
    Dumps the UI Automation tree of a running PDFsam window: what screen readers such as NVDA read.
.DESCRIPTION
    Prints one line per element of the UI Automation control view with its control type, name, localized
    control type, help text (read as description), value and toggle state. Use it to check that every
    control has a name and that fields and combo boxes report their real value.
    Windows only; PDFsam must be running.
.EXAMPLE
    ./dev/uia-dump.ps1
    ./dev/uia-dump.ps1 -Out merge.txt -MaxDepth 20
#>
param(
    [string]$WindowTitle = 'PDFsam',
    [int]$MaxDepth = 40,
    [string]$Out
)
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes
$walker = [System.Windows.Automation.TreeWalker]::ControlViewWalker
$sb = [System.Text.StringBuilder]::new()

function Dump($el, $depth) {
    if ($depth -gt $MaxDepth) { return }
    try {
        $c = $el.Current
        $type = $c.ControlType.ProgrammaticName -replace 'ControlType\.', ''
        $flags = @()
        if (-not $c.IsEnabled) { $flags += 'disabled' }
        if ($c.IsOffscreen) { $flags += 'offscreen' }
        $help = if ($c.HelpText) { " help='$($c.HelpText)'" } else { '' }
        $lct = if ($c.LocalizedControlType) { " lct='$($c.LocalizedControlType)'" } else { '' }
        $val = ''
        try {
            $vp = $el.GetCurrentPattern([System.Windows.Automation.ValuePattern]::Pattern)
            if ($vp.Current.Value) { $val = " value='$($vp.Current.Value)'" }
        } catch {}
        $tg = ''
        try {
            $tp = $el.GetCurrentPattern([System.Windows.Automation.TogglePattern]::Pattern)
            $tg = " toggle=$($tp.Current.ToggleState)"
        } catch {}
        $f = if ($flags) { " [$($flags -join ',')]" } else { '' }
        [void]$sb.AppendLine(('  ' * $depth) + "$type name='$($c.Name)'$lct$help$val$tg$f")
    } catch {
        [void]$sb.AppendLine(('  ' * $depth) + "<error: $_>")
        return
    }
    $child = $walker.GetFirstChild($el)
    while ($child) {
        Dump $child ($depth + 1)
        $child = $walker.GetNextSibling($child)
    }
}

$windows = [System.Windows.Automation.AutomationElement]::RootElement.FindAll(
    [System.Windows.Automation.TreeScope]::Children, [System.Windows.Automation.Condition]::TrueCondition) |
    Where-Object { $_.Current.Name -like "$WindowTitle*" }
if (-not $windows) { throw "No window whose title starts with '$WindowTitle'" }
foreach ($w in $windows) { Dump $w 0 }
if ($Out) { $sb.ToString() | Set-Content -Encoding utf8 $Out } else { $sb.ToString() }
