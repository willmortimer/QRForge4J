package qrcli

import io.nayuki.qrcodegen.QrCode
import io.nayuki.qrcodegen.QrCode.Ecc
import io.nayuki.qrcodegen.QrSegment
import kotlin.math.*

/** Options controlling every aspect of the SVG output **/
data class RenderOpts(
    val width: Int = 512,
    val height: Int = 512,
    val margin: Int = 16,
    val circleShape: Boolean = false,
    val dotsType: DotType = DotType.CIRCLE,
    val dotRadiusFactor: Double = 0.5,
    val rounded: Boolean = false,
    val extraRounded: Boolean = false,
    val classyRounded: Boolean = false,
    val fgColor: String = "#000",
    val bgColor: String? = "#fff",
    val logoHoleRadiusPx: Double? = null,
    val centerImageHref: String? = null,
    val centerImageSizeRatio: Double = 0.2,
    val locatorOpts: LocatorOpts? = null,
    val gradient: GradientSpec? = null,
    val border: BorderSpec? = null,
    val ecc: Ecc = Ecc.QUARTILE,
    val mask: Int = -1,
    // Advanced styling features
    val moduleOutline: ModuleOutline? = null,
    val quietZoneAccent: QuietZoneAccent? = null,
    val dropShadow: DropShadow? = null,
    val backgroundPattern: BackgroundPattern? = null,
    val alignmentPattern: AlignmentPattern? = null,
    val finderRefinement: FinderRefinement? = null,
    val gradientMasking: GradientMasking? = null,
    val microTypography: MicroTypography? = null
)

enum class DotType { CIRCLE, SQUARE, CLASSY, ROUNDED, EXTRA_ROUNDED, CLASSY_ROUNDED }

sealed class LocatorShape {
    object Square     : LocatorShape()
    object Circle     : LocatorShape()
    data class Rounded(val radiusFactor: Double = 0.35) : LocatorShape()
    object Classy     : LocatorShape()
}

/** Drawn at each finder‐pattern corner */
data class LocatorOpts(
    val shape: LocatorShape,
    val color: String,
    val sizeRatio: Double = 7.0   // in “modules” (finder box is 7×7)
)

data class GradientSpec(
    val type: GradientType,
    val stops: List<ColorStop>,
    val rotationRad: Double = 0.0
)
enum class GradientType { LINEAR, RADIAL }
data class ColorStop(val offset: Double, val color: String)

data class BorderSpec(
    val thickness: Double,
    val color: String,
    val round: Double = 0.0,
    val inner: BorderSpec? = null,
    val outer: BorderSpec? = null
)

data class ModuleOutline(
    val enabled: Boolean = false,
    val color: String = "#111111",
    val width: Double = 0.5
)

data class QuietZoneAccent(
    val enabled: Boolean = false,
    val color: String = "#444444",
    val width: Double = 1.0,
    val dashArray: String = "4 4"
)

data class DropShadow(
    val enabled: Boolean = false,
    val blur: Double = 1.0,
    val opacity: Double = 0.2,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0
)

data class BackgroundPattern(
    val enabled: Boolean = false,
    val type: PatternType = PatternType.DOTS,
    val color: String = "#f0f0f0",
    val opacity: Double = 0.02,
    val size: Double = 4.0
)

enum class PatternType { DOTS, GRID, DIAGONAL_LINES, HEXAGON }

data class AlignmentPattern(
    val enabled: Boolean = false,
    val type: AlignmentType = AlignmentType.CIRCLE,
    val color: String? = null // null means use module color
)

enum class AlignmentType { CIRCLE, TRIANGLE, DIAMOND, STAR, CUSTOM }

data class FinderRefinement(
    val enabled: Boolean = false,
    val innerLogo: String? = null,
    val logoSize: Double = 0.5
)

data class GradientMasking(
    val enabled: Boolean = false,
    val type: MaskingType = MaskingType.CONCENTRIC,
    val centerColor: String? = null,
    val edgeColor: String? = null
)

enum class MaskingType { CONCENTRIC, RADIAL, LINEAR }

data class MicroTypography(
    val enabled: Boolean = false,
    val text: String = "",
    val fontSize: Double = 8.0,
    val color: String = "#666666",
    val path: TypographyPath = TypographyPath.CIRCULAR
)

enum class TypographyPath { CIRCULAR, LINEAR_TOP, LINEAR_BOTTOM }

/** Public entry: encode a Latin-1 string */
fun renderSvgLatin1(data: String, opts: RenderOpts): String {
    val bytes = data.toByteArray(Charsets.ISO_8859_1)
    val seg = QrSegment.makeBytes(bytes)
    return renderQr(seg, opts)
}

/** Public entry: decode a Base64 payload */
fun renderSvgBase64(base64: String, opts: RenderOpts): String {
    val data = java.util.Base64.getDecoder().decode(base64.trim())
    val seg = QrSegment.makeBytes(data)
    return renderQr(seg, opts)
}

/** Core: build the QR matrix, then emit optimized SVG */
private fun renderQr(seg: QrSegment, opts: RenderOpts): String {
    val qr = QrCode.encodeSegments(
        listOf(seg), opts.ecc, 1, 40, opts.mask, true
    )
    val count   = qr.size
    val minSize = min(opts.width, opts.height) - 2 * opts.margin
    val realSize = if (opts.circleShape) (minSize / sqrt(2.0)).floorToInt() else minSize
    val dot     = realSize.toDouble() / count
    val x0      = (opts.width - count * dot) / 2.0
    val y0      = (opts.height - count * dot) / 2.0

    val sb = StringBuilder(60000)
    sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="${opts.width}" height="${opts.height}" viewBox="0 0 ${opts.width} ${opts.height}">""")
    sb.append("<defs>")

    // Background pattern
    opts.backgroundPattern?.let { pattern ->
        if (pattern.enabled) {
            val patternId = "bgPattern"
            sb.append("""<pattern id="$patternId" patternUnits="userSpaceOnUse" width="${pattern.size}" height="${pattern.size}">""")
            when (pattern.type) {
                PatternType.DOTS -> {
                    val r = pattern.size * 0.2
                    sb.append("""<circle cx="${pattern.size/2}" cy="${pattern.size/2}" r="$r" fill="${pattern.color}" opacity="${pattern.opacity}"/>""")
                }
                PatternType.GRID -> {
                    sb.append("""<rect width="${pattern.size}" height="${pattern.size}" fill="none" stroke="${pattern.color}" stroke-width="0.5" opacity="${pattern.opacity}"/>""")
                }
                PatternType.DIAGONAL_LINES -> {
                    sb.append("""<path d="M0,0 L${pattern.size},${pattern.size} M0,${pattern.size} L${pattern.size},0" stroke="${pattern.color}" stroke-width="0.5" opacity="${pattern.opacity}"/>""")
                }
                PatternType.HEXAGON -> {
                    val hex = createHexagon(pattern.size * 0.4, pattern.size/2, pattern.size/2)
                    sb.append("""<path d="$hex" fill="none" stroke="${pattern.color}" stroke-width="0.5" opacity="${pattern.opacity}"/>""")
                }
            }
            sb.append("</pattern>")
        }
    }

    // Drop shadow filter
    opts.dropShadow?.let { shadow ->
        if (shadow.enabled) {
            val filterId = "dropShadow"
            sb.append("""<filter id="$filterId" x="-20%" y="-20%" width="140%" height="140%">""")
            sb.append("""<feGaussianBlur in="SourceAlpha" stdDeviation="${shadow.blur}" result="blur"/>""")
            sb.append("""<feOffset in="blur" dx="${shadow.offsetX}" dy="${shadow.offsetY}" result="offsetBlur"/>""")
            sb.append("""<feFlood flood-color="black" flood-opacity="${shadow.opacity}" result="shadowColor"/>""")
            sb.append("""<feComposite in="shadowColor" in2="offsetBlur" operator="in" result="shadow"/>""")
            sb.append("""<feMerge>""")
            sb.append("""<feMergeNode in="shadow"/>""")
            sb.append("""<feMergeNode in="SourceGraphic"/>""")
            sb.append("""</feMerge>""")
            sb.append("</filter>")
        }
    }

    // Global module gradient
    val paintId = opts.gradient?.let { grad ->
        val id = "grad0"
        when (grad.type) {
            GradientType.LINEAR -> {
                val (x1, y1, x2, y2) = linearEndpoints(grad.rotationRad, opts.width.toDouble(), opts.height.toDouble())
                sb.append("""<linearGradient id="$id" gradientUnits="userSpaceOnUse" x1="${fmt(x1)}" y1="${fmt(y1)}" x2="${fmt(x2)}" y2="${fmt(y2)}">""")
                grad.stops.forEach { sb.append("""<stop offset="${fmtPct(it.offset)}" stop-color="${it.color}"/>""") }
                sb.append("</linearGradient>")
            }
            GradientType.RADIAL -> {
                sb.append("""<radialGradient id="$id" cx="${opts.width/2}" cy="${opts.height/2}" r="${max(opts.width, opts.height)/2}">""")
                grad.stops.forEach { sb.append("""<stop offset="${fmtPct(it.offset)}" stop-color="${it.color}"/>""") }
                sb.append("</radialGradient>")
            }
        }
        id
    }

    // Circle‐mask clipPath
    val clipId = if (opts.circleShape) {
        val id = "clipCircle"
        val r  = min(opts.width, opts.height)/2
        sb.append("""<clipPath id="$id"><circle cx="${opts.width/2}" cy="${opts.height/2}" r="$r"/></clipPath>""")
        id
    } else null

    sb.append("</defs>")

    // Background
    val bgFill = if (opts.backgroundPattern?.enabled == true) "url(#bgPattern)" else opts.bgColor
    bgFill?.let { sb.append("""<rect width="${opts.width}" height="${opts.height}" fill="$it"/>""") }

    // Borders
    opts.border?.let { drawBorder(sb, it, opts.width.toDouble(), opts.height.toDouble()) }

    // Quiet zone accent lines
    opts.quietZoneAccent?.let { accent ->
        if (accent.enabled) {
            val accentX = x0 - dot
            val accentY = y0 - dot  
            val accentSize = (count + 2) * dot
            sb.append("""<rect x="${fmt(accentX)}" y="${fmt(accentY)}" width="${fmt(accentSize)}" height="${fmt(accentSize)}" """)
            sb.append("""fill="none" stroke="${accent.color}" stroke-width="${fmt(accent.width)}" stroke-dasharray="${accent.dashArray}"/>""")
        }
    }

    // Clip‐group
    sb.append("<g")
    if (clipId != null) sb.append(""" clip-path="url(#$clipId)""" )
    sb.append(">")

    // Center image
    opts.centerImageHref?.let { href ->
        val imgSize = realSize * opts.centerImageSizeRatio
        val ix = (opts.width - imgSize) / 2.0
        val iy = (opts.height - imgSize) / 2.0
        sb.append(
          """<image href="$href" x="${fmt(ix)}" y="${fmt(iy)}" """ +
          """width="${fmt(imgSize)}" height="${fmt(imgSize)}" """ +
          """preserveAspectRatio="xMidYMid meet"/>"""
        )
    }

    // Custom corner locators
    opts.locatorOpts?.let { drawLocators(sb, count, x0, y0, dot, it) }

    // Modules group
    val fillAttr = paintId?.let { "url(#$it)" } ?: opts.fgColor
    val strokeAttr = if (opts.moduleOutline?.enabled == true) {
        """stroke="${opts.moduleOutline.color}" stroke-width="${fmt(opts.moduleOutline.width)}""""
    } else ""
    val filterAttr = if (opts.dropShadow?.enabled == true) {
        """filter="url(#dropShadow)""""
    } else ""
    sb.append("""<g fill="$fillAttr" $strokeAttr $filterAttr shape-rendering="crispEdges">""")

    when {
      // Fast batched squares
      opts.dotsType == DotType.SQUARE && !opts.rounded -> {
        val path = StringBuilder(count * 20)
        for (r in 0 until count) {
          var run = 0
          var sc  = 0
          for (c in 0 until count) {
            val on = qr.getModule(c, r)
            if (on && allowDot(r, c, count, opts, x0, y0, dot)) {
              if (run==0) sc = c
              run++
            } else if (run>0) {
              rectPath(path, x0+sc*dot, y0+r*dot, run*dot, dot)
              run=0
            }
          }
          if (run>0) rectPath(path, x0+sc*dot, y0+r*dot, run*dot, dot)
        }
        sb.append("""<path d="$path"/>""")
      }
      // Classy ring‐modules
      opts.dotsType == DotType.CLASSY -> {
        val strokeW = dot * 0.2
        val rr      = dot/2 - strokeW/2
        for (r in 0 until count) for (c in 0 until count) {
          if (!qr.getModule(c, r)) continue
          if (!allowDot(r, c, count, opts, x0, y0, dot)) continue
          val cx = x0 + c*dot + dot/2
          val cy = y0 + r*dot + dot/2
          sb.append(
            """<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rr)}" """ +
            """fill="none" stroke="$fillAttr" stroke-width="${fmt(strokeW)}"/>"""
          )
        }
      }
      // Circles, rounded squares, and advanced styles
      else -> {
        val r = dot * opts.dotRadiusFactor
        for (row in 0 until count) for (col in 0 until count) {
          if (!qr.getModule(col, row)) continue
          if (!allowDot(row, col, count, opts, x0, y0, dot)) continue
          val x = x0 + col*dot
          val y = y0 + row*dot
          val cx = x + dot/2
          val cy = y + dot/2
          
          // Apply gradient masking if enabled
          val moduleColor = if (opts.gradientMasking?.enabled == true) {
            applyGradientMasking(opts.gradientMasking, fillAttr, cx, cy, opts.width/2.0, opts.height/2.0)
          } else fillAttr
          
          when (opts.dotsType) {
            DotType.CIRCLE -> {
              sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(r)}" fill="$moduleColor"/>""")
            }
            DotType.ROUNDED -> {
              val rx = if (opts.rounded) r else dot * 0.2
              sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$moduleColor"/>""")
            }
            DotType.EXTRA_ROUNDED -> {
              val rx = dot * 0.45  // Much larger radius for extra-rounded
              sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$moduleColor"/>""")
            }
            DotType.CLASSY_ROUNDED -> {
              // Combination of ring and rounded
              val outerR = dot * 0.45
              val innerR = outerR * 0.6
              val strokeW = outerR - innerR
              sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(outerR)}" ry="${fmt(outerR)}" """)
              sb.append("""fill="none" stroke="$moduleColor" stroke-width="${fmt(strokeW)}"/>""")
              sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(innerR * 0.7)}" fill="$moduleColor"/>""")
            }
            else -> {
              // Default square or existing circle logic
              if (opts.dotsType == DotType.CIRCLE) {
                sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(r)}" fill="$moduleColor"/>""")
              } else {
                val rx = if (opts.rounded) r else 0.0
                sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$moduleColor"/>""")
              }
            }
          }
        }
      }
    }

    sb.append("</g>")
    
    // Micro typography
    opts.microTypography?.let { typo ->
        if (typo.enabled && typo.text.isNotEmpty()) {
            when (typo.path) {
                TypographyPath.CIRCULAR -> {
                    val radius = min(opts.width, opts.height) / 2.0 - typo.fontSize
                    val pathId = "circularPath"
                    sb.append("""<defs><path id="$pathId" d="M ${opts.width/2},${typo.fontSize} A $radius,$radius 0 1,1 ${opts.width/2-1},${typo.fontSize}"/></defs>""")
                    sb.append("""<text font-size="${typo.fontSize}" fill="${typo.color}">""")
                    sb.append("""<textPath href="#$pathId">${typo.text}</textPath>""")
                    sb.append("""</text>""")
                }
                TypographyPath.LINEAR_TOP -> {
                    sb.append("""<text x="${opts.width/2}" y="${typo.fontSize}" text-anchor="middle" font-size="${typo.fontSize}" fill="${typo.color}">${typo.text}</text>""")
                }
                TypographyPath.LINEAR_BOTTOM -> {
                    sb.append("""<text x="${opts.width/2}" y="${opts.height - 4}" text-anchor="middle" font-size="${typo.fontSize}" fill="${typo.color}">${typo.text}</text>""")
                }
            }
        }
    }
    
    sb.append("</g></svg>")
    return sb.toString()
}

/** Draw the 3 finder‐pattern locators with whatever shape was requested */
private fun drawLocators(
    sb: StringBuilder,
    count: Int,
    x0: Double,
    y0: Double,
    dot: Double,
    loc: LocatorOpts
) {
    val sizePx = dot * loc.sizeRatio
    val positions = listOf(
        0 to 0,
        (count - loc.sizeRatio).toInt() to 0,
        0 to (count - loc.sizeRatio).toInt()
    )
    positions.forEach { (col, row) ->
        val x = x0 + col*dot
        val y = y0 + row*dot
        when (loc.shape) {
          LocatorShape.Square -> {
            sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(sizePx)}" height="${fmt(sizePx)}" fill="${loc.color}"/>""")
          }
          LocatorShape.Circle -> {
            val r = sizePx/2
            sb.append("""<circle cx="${fmt(x+r)}" cy="${fmt(y+r)}" r="${fmt(r)}" fill="${loc.color}"/>""")
          }
          is LocatorShape.Rounded -> {
            val rx = sizePx * loc.shape.radiusFactor
            sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(sizePx)}" height="${fmt(sizePx)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="${loc.color}"/>""")
          }
          LocatorShape.Classy -> {
            val cx = x + sizePx/2
            val cy = y + sizePx/2
            val sw = sizePx * 0.15
            val rO = sizePx/2 - sw/2
            sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rO)}" fill="none" stroke="${loc.color}" stroke-width="${fmt(sw)}"/>""")
            val rI = rO - sw*1.2
            sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rI)}" fill="${loc.color}"/>""")
          }
        }
    }
}

/** Exclude finder areas, dotMask, and logo‐hole region */
private fun allowDot(
    row: Int, col: Int, count: Int,
    opts: RenderOpts,
    x0: Double, y0: Double, dot: Double
): Boolean {
    // mask out the 7×7 finder at TL, TR, BL
    fun inFinder(r: Int, c: Int) = (r < 7 && c < 7) ||
                                   (r < 7 && c >= count-7) ||
                                   (r >= count-7 && c < 7)
    if (inFinder(row, col)) return false
    // logo‐hole
    opts.logoHoleRadiusPx?.let { hr ->
        val cx = x0 + col*dot + dot/2 - opts.width/2.0
        val cy = y0 + row*dot + dot/2 - opts.height/2.0
        if (sqrt(cx*cx + cy*cy) < hr) return false
    }
    return true
}

private fun rectPath(sb: StringBuilder, x: Double, y: Double, w: Double, h: Double) {
    sb.append("M").append(fmt(x)).append(",").append(fmt(y))
      .append("h").append(fmt(w))
      .append("v").append(fmt(h))
      .append("h-").append(fmt(w))
      .append("z")
}

private fun drawBorder(sb: StringBuilder, b: BorderSpec, w: Double, h: Double) {
    fun rect(th: Double, col: String, rx: Double) {
        val off = th/2
        sb.append("""<rect x="${fmt(off)}" y="${fmt(off)}" width="${fmt(w-th)}" height="${fmt(h-th)}" fill="none" stroke="$col" stroke-width="${fmt(th)}" """)
        if (rx>0) sb.append("""rx="${fmt(rx)}" ry="${fmt(rx)}" """)
        sb.append("/>")
    }
    rect(b.thickness, b.color, (min(w,h)/2)*b.round)
    b.inner?.let { rect(it.thickness, it.color, (min(w,h)/2)*it.round) }
    b.outer?.let { rect(it.thickness, it.color, (min(w,h)/2)*it.round) }
}

private fun linearEndpoints(rot: Double, w: Double, h: Double): Quad {
    val cx = w/2; val cy = h/2
    val dx = cos(rot); val dy = sin(rot)
    val half = max(w,h)
    return Quad(cx - dx*half, cy - dy*half, cx + dx*half, cy + dy*half)
}
private data class Quad(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

private fun fmt(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString()
    else String.format("%.2f", d)

private fun fmtPct(d: Double): String = String.format("%.2f%%", d*100.0)
private fun Double.floorToInt(): Int = floor(this).toInt()

private fun createHexagon(radius: Double, cx: Double, cy: Double): String {
    val points = mutableListOf<String>()
    for (i in 0 until 6) {
        val angle = i * PI / 3.0
        val x = cx + radius * cos(angle)
        val y = cy + radius * sin(angle)
        points.add("${fmt(x)},${fmt(y)}")
    }
    return "M${points[0]} L${points.joinToString(" L")} Z"
}

private fun applyGradientMasking(masking: GradientMasking, defaultColor: String, x: Double, y: Double, centerX: Double, centerY: Double): String {
    if (!masking.enabled) return defaultColor
    
    return when (masking.type) {
        MaskingType.CONCENTRIC -> {
            val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            val maxDistance = sqrt(centerX.pow(2) + centerY.pow(2))
            val ratio = (distance / maxDistance).coerceIn(0.0, 1.0)
            
            // Interpolate between center and edge colors
            val startColor = masking.centerColor ?: defaultColor
            val endColor = masking.edgeColor ?: defaultColor
            interpolateColor(startColor, endColor, ratio)
        }
        MaskingType.RADIAL -> {
            val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
            val maxDistance = min(centerX, centerY)
            val ratio = (distance / maxDistance).coerceIn(0.0, 1.0)
            
            val startColor = masking.centerColor ?: defaultColor
            val endColor = masking.edgeColor ?: defaultColor
            interpolateColor(startColor, endColor, ratio)
        }
        MaskingType.LINEAR -> {
            val ratio = (x / (centerX * 2)).coerceIn(0.0, 1.0)
            val startColor = masking.centerColor ?: defaultColor
            val endColor = masking.edgeColor ?: defaultColor
            interpolateColor(startColor, endColor, ratio)
        }
    }
}

private fun interpolateColor(startColor: String, endColor: String, ratio: Double): String {
    // Simple RGB interpolation for hex colors
    if (!startColor.startsWith("#") || !endColor.startsWith("#")) return startColor
    
    try {
        val start = startColor.substring(1).toInt(16)
        val end = endColor.substring(1).toInt(16)
        
        val startR = (start shr 16) and 0xFF
        val startG = (start shr 8) and 0xFF
        val startB = start and 0xFF
        
        val endR = (end shr 16) and 0xFF
        val endG = (end shr 8) and 0xFF
        val endB = end and 0xFF
        
        val r = (startR + (endR - startR) * ratio).toInt().coerceIn(0, 255)
        val g = (startG + (endG - startG) * ratio).toInt().coerceIn(0, 255)
        val b = (startB + (endB - startB) * ratio).toInt().coerceIn(0, 255)
        
        return "#%02x%02x%02x".format(r, g, b)
    } catch (e: Exception) {
        return startColor
    }
}
