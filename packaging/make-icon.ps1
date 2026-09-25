# Génère packaging\cafe.ico (tasse à café) sans dépendance externe.
Add-Type -AssemblyName System.Drawing

function New-CupPng([int]$size) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.Clear([System.Drawing.Color]::Transparent)
    $s = $size / 256.0

    # Fond arrondi marron foncé
    $bg = New-Object System.Drawing.Drawing2D.GraphicsPath
    $r = 48 * $s; $d = $r * 2
    $bg.AddArc(0, 0, $d, $d, 180, 90)
    $bg.AddArc($size - $d, 0, $d, $d, 270, 90)
    $bg.AddArc($size - $d, $size - $d, $d, $d, 0, 90)
    $bg.AddArc(0, $size - $d, $d, $d, 90, 90)
    $bg.CloseFigure()
    $g.FillPath((New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 58, 36, 26))), $bg)

    $cream = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 245, 232, 214))
    $coffee = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 122, 76, 46))

    # Anse
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 245, 232, 214)), (16 * $s)
    $g.DrawArc($pen, 160 * $s, 104 * $s, 56 * $s, 60 * $s, -90, 180)
    # Tasse
    $cup = New-Object System.Drawing.Drawing2D.GraphicsPath
    $cup.AddLine(52 * $s, 96 * $s, 186 * $s, 96 * $s)
    $cup.AddArc(52 * $s, 96 * $s, 134 * $s, 100 * $s, 0, 180)
    $cup.CloseFigure()
    $g.FillPath($cream, $cup)
    # Café
    $g.FillEllipse($coffee, 60 * $s, 88 * $s, 118 * $s, 18 * $s)
    # Soucoupe
    $g.FillEllipse($cream, 40 * $s, 190 * $s, 160 * $s, 20 * $s)
    # Vapeur
    $steam = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200, 245, 232, 214)), (9 * $s)
    $steam.StartCap = 'Round'; $steam.EndCap = 'Round'
    foreach ($x in 88, 120, 152) {
        $g.DrawBezier($steam, $x * $s, 74 * $s, ($x - 14) * $s, 58 * $s, ($x + 14) * $s, 44 * $s, $x * $s, 26 * $s)
    }
    $g.Dispose()
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    return , $ms.ToArray()
}

$sizes = 256, 128, 64, 48, 32, 16
$images = foreach ($sz in $sizes) { , (New-CupPng $sz) }

$out = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter $out
$w.Write([uint16]0); $w.Write([uint16]1); $w.Write([uint16]$sizes.Count)
$offset = 6 + 16 * $sizes.Count
for ($i = 0; $i -lt $sizes.Count; $i++) {
    $sz = $sizes[$i]
    $w.Write([byte]($(if ($sz -ge 256) { 0 } else { $sz })))
    $w.Write([byte]($(if ($sz -ge 256) { 0 } else { $sz })))
    $w.Write([byte]0); $w.Write([byte]0)
    $w.Write([uint16]1); $w.Write([uint16]32)
    $w.Write([uint32]$images[$i].Length)
    $w.Write([uint32]$offset)
    $offset += $images[$i].Length
}
foreach ($img in $images) { $w.Write($img) }
$w.Flush()

$target = Join-Path $PSScriptRoot 'cafe.ico'
[System.IO.File]::WriteAllBytes($target, $out.ToArray())
Write-Host "Icone creee : $target"
