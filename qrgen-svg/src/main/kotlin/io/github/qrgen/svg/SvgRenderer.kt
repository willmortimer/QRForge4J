package io.github.qrgen.svg

import io.github.qrgen.core.*
import kotlin.math.*

/** SVG renderer interface **/
interface QrSvgRenderer {
    fun render(qrResult: QrResult): String
}

/** Default SVG renderer implementation **/
class DefaultSvgRenderer : QrSvgRenderer {
    
    override fun render(qrResult: QrResult): String {
        val config = qrResult.config
        val modules = qrResult.modules
        val count = qrResult.size
        
        val minSize = minOf(config.layout.width, config.layout.height) - 2 * config.layout.margin
        val realSize = if (config.layout.circleShape) (minSize / sqrt(2.0)).toInt() else minSize
        val dot = realSize.toDouble() / count
        val x0 = (config.layout.width - count * dot) / 2.0
        val y0 = (config.layout.height - count * dot) / 2.0
        
        val sb = StringBuilder(60000)
        
        // SVG header
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" width="${config.layout.width}" height="${config.layout.height}" viewBox="0 0 ${config.layout.width} ${config.layout.height}">""")
        sb.append("<defs>")
        
        // Generate definitions (patterns, filters, gradients, etc.)
        generateDefs(sb, config)
        
        sb.append("</defs>")
        
        // Background
        renderBackground(sb, config)
        
        // Borders
        renderBorders(sb, config)
        
        // Quiet zone accent
        renderQuietZoneAccent(sb, config, x0, y0, dot, count)
        
        // Clip group for circular shape
        val clipId = if (config.layout.circleShape) "clipCircle" else null
        sb.append("<g")
        if (clipId != null) {
            sb.append(""" clip-path="url(#$clipId)"""")
        }
        sb.append(">")
        
        // Center image/logo
        renderCenterImage(sb, config)
        
        // Custom corner locators
        renderLocators(sb, config, count, x0, y0, dot)
        
        // QR modules
        renderModules(sb, config, modules, count, x0, y0, dot)
        
        sb.append("</g>")
        
        // Micro typography
        renderMicroTypography(sb, config)
        
        sb.append("</svg>")
        return sb.toString()
    }
    
    private fun generateDefs(sb: StringBuilder, config: QrStyleConfig) {
        // Background pattern
        config.advanced.backgroundPattern?.let { pattern ->
            if (pattern.enabled) {
                generateBackgroundPattern(sb, pattern)
            }
        }
        
        // Drop shadow filter
        config.advanced.dropShadow?.let { shadow ->
            if (shadow.enabled) {
                generateDropShadowFilter(sb, shadow)
            }
        }
        
        // Global gradient
        config.gradient.type?.let { gradType ->
            generateGradient(sb, config.gradient, config.layout)
        }
        
        // Circle clip path
        if (config.layout.circleShape) {
            val r = minOf(config.layout.width, config.layout.height) / 2
            sb.append("""<clipPath id="clipCircle"><circle cx="${config.layout.width/2}" cy="${config.layout.height/2}" r="$r"/></clipPath>""")
        }
    }
    
    private fun generateBackgroundPattern(sb: StringBuilder, pattern: BackgroundPattern) {
        sb.append("""<pattern id="bgPattern" patternUnits="userSpaceOnUse" width="${pattern.size}" height="${pattern.size}">""")
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
    
    private fun generateDropShadowFilter(sb: StringBuilder, shadow: DropShadow) {
        sb.append("""<filter id="dropShadow" x="-20%" y="-20%" width="140%" height="140%">""")
        sb.append("""<feGaussianBlur in="SourceAlpha" stdDeviation="${shadow.blur}" result="blur"/>""")
        sb.append("""<feOffset in="blur" dx="${shadow.offsetX}" dy="${shadow.offsetY}" result="offsetBlur"/>""")
        sb.append("""<feFlood flood-color="black" flood-opacity="${shadow.opacity}" result="shadowColor"/>""")
        sb.append("""<feComposite in="shadowColor" in2="offsetBlur" operator="in" result="shadow"/>""")
        sb.append("""<feMerge><feMergeNode in="shadow"/><feMergeNode in="SourceGraphic"/></feMerge>""")
        sb.append("</filter>")
    }
    
    private fun generateGradient(sb: StringBuilder, gradient: GradientOptions, layout: LayoutOptions) {
        when (gradient.type) {
            GradientType.LINEAR -> {
                val (x1, y1, x2, y2) = linearEndpoints(gradient.rotationRad, layout.width.toDouble(), layout.height.toDouble())
                sb.append("""<linearGradient id="grad0" gradientUnits="userSpaceOnUse" x1="${fmt(x1)}" y1="${fmt(y1)}" x2="${fmt(x2)}" y2="${fmt(y2)}">""")
                gradient.stops.forEach { sb.append("""<stop offset="${fmtPct(it.offset)}" stop-color="${it.color}"/>""") }
                sb.append("</linearGradient>")
            }
            GradientType.RADIAL -> {
                sb.append("""<radialGradient id="grad0" cx="${layout.width/2}" cy="${layout.height/2}" r="${maxOf(layout.width, layout.height)/2}">""")
                gradient.stops.forEach { sb.append("""<stop offset="${fmtPct(it.offset)}" stop-color="${it.color}"/>""") }
                sb.append("</radialGradient>")
            }
            null -> {} // No gradient
        }
    }
    
    private fun renderBackground(sb: StringBuilder, config: QrStyleConfig) {
        val bgFill = if (config.advanced.backgroundPattern?.enabled == true) "url(#bgPattern)" else config.colors.background
        bgFill?.let { 
            sb.append("""<rect width="${config.layout.width}" height="${config.layout.height}" fill="$it"/>""") 
        }
    }
    
    private fun renderBorders(sb: StringBuilder, config: QrStyleConfig) {
        if (config.border.thickness > 0) {
            drawBorder(sb, config.border, config.layout.width.toDouble(), config.layout.height.toDouble())
        }
    }
    
    private fun renderQuietZoneAccent(sb: StringBuilder, config: QrStyleConfig, x0: Double, y0: Double, dot: Double, count: Int) {
        config.advanced.quietZoneAccent?.let { accent ->
            if (accent.enabled) {
                val accentX = x0 - dot
                val accentY = y0 - dot  
                val accentSize = (count + 2) * dot
                sb.append("""<rect x="${fmt(accentX)}" y="${fmt(accentY)}" width="${fmt(accentSize)}" height="${fmt(accentSize)}" """)
                sb.append("""fill="none" stroke="${accent.color}" stroke-width="${fmt(accent.width)}" stroke-dasharray="${accent.dashArray}"/>""")
            }
        }
    }
    
    private fun renderCenterImage(sb: StringBuilder, config: QrStyleConfig) {
        config.logo.href?.let { href ->
            val realSize = minOf(config.layout.width, config.layout.height) - 2 * config.layout.margin
            val imgSize = realSize * config.logo.sizeRatio
            val ix = (config.layout.width - imgSize) / 2.0
            val iy = (config.layout.height - imgSize) / 2.0
            sb.append(
                """<image href="$href" x="${fmt(ix)}" y="${fmt(iy)}" """ +
                """width="${fmt(imgSize)}" height="${fmt(imgSize)}" """ +
                """preserveAspectRatio="xMidYMid meet"/>"""
            )
        }
    }
    
    private fun renderLocators(sb: StringBuilder, config: QrStyleConfig, count: Int, x0: Double, y0: Double, dot: Double) {
        config.locators.shape?.let { shape ->
            val sizePx = dot * config.locators.sizeRatio
            val positions = listOf(
                0 to 0,
                (count - config.locators.sizeRatio).toInt() to 0,
                0 to (count - config.locators.sizeRatio).toInt()
            )
            
            positions.forEach { (col, row) ->
                val x = x0 + col * dot
                val y = y0 + row * dot
                when (shape) {
                    LocatorShape.Square -> {
                        sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(sizePx)}" height="${fmt(sizePx)}" fill="${config.locators.color}"/>""")
                    }
                    LocatorShape.Circle -> {
                        val r = sizePx / 2
                        sb.append("""<circle cx="${fmt(x + r)}" cy="${fmt(y + r)}" r="${fmt(r)}" fill="${config.locators.color}"/>""")
                    }
                    is LocatorShape.Rounded -> {
                        val rx = sizePx * shape.radiusFactor
                        sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(sizePx)}" height="${fmt(sizePx)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="${config.locators.color}"/>""")
                    }
                    LocatorShape.Classy -> {
                        val cx = x + sizePx / 2
                        val cy = y + sizePx / 2
                        val sw = sizePx * 0.15
                        val rO = sizePx / 2 - sw / 2
                        sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rO)}" fill="none" stroke="${config.locators.color}" stroke-width="${fmt(sw)}"/>""")
                        val rI = rO - sw * 1.2
                        sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rI)}" fill="${config.locators.color}"/>""")
                    }
                }
            }
        }
    }
    
    private fun renderModules(sb: StringBuilder, config: QrStyleConfig, modules: Array<BooleanArray>, count: Int, x0: Double, y0: Double, dot: Double) {
        val fillAttr = if (config.gradient.type != null) "url(#grad0)" else config.colors.foreground
        val strokeAttr = config.advanced.moduleOutline?.let { outline ->
            if (outline.enabled) {
                """stroke="${outline.color}" stroke-width="${fmt(outline.width)}""""
            } else ""
        } ?: ""
        val filterAttr = config.advanced.dropShadow?.let { shadow ->
            if (shadow.enabled) {
                """filter="url(#dropShadow)""""
            } else ""
        } ?: ""
        
        sb.append("""<g fill="$fillAttr" $strokeAttr $filterAttr shape-rendering="crispEdges">""")
        
        when {
            // Fast batched squares
            config.modules.type == DotType.SQUARE && !config.modules.rounded -> {
                renderBatchedSquares(sb, modules, count, x0, y0, dot, config)
            }
            // Classy ring modules
            config.modules.type == DotType.CLASSY -> {
                renderClassyModules(sb, modules, count, x0, y0, dot, config, fillAttr)
            }
            // Individual modules (circles, rounded, etc.)
            else -> {
                renderIndividualModules(sb, modules, count, x0, y0, dot, config, fillAttr)
            }
        }
        
        sb.append("</g>")
    }
    
    private fun renderBatchedSquares(sb: StringBuilder, modules: Array<BooleanArray>, count: Int, x0: Double, y0: Double, dot: Double, config: QrStyleConfig) {
        val path = StringBuilder(count * 20)
        for (r in 0 until count) {
            var run = 0
            var sc = 0
            for (c in 0 until count) {
                val on = modules[r][c]
                if (on && shouldDrawModule(r, c, count, config, x0, y0, dot)) {
                    if (run == 0) sc = c
                    run++
                } else if (run > 0) {
                    rectPath(path, x0 + sc * dot, y0 + r * dot, run * dot, dot)
                    run = 0
                }
            }
            if (run > 0) rectPath(path, x0 + sc * dot, y0 + r * dot, run * dot, dot)
        }
        sb.append("""<path d="$path"/>""")
    }
    
    private fun renderClassyModules(sb: StringBuilder, modules: Array<BooleanArray>, count: Int, x0: Double, y0: Double, dot: Double, config: QrStyleConfig, fillAttr: String) {
        val strokeW = dot * 0.2
        val rr = dot / 2 - strokeW / 2
        for (r in 0 until count) {
            for (c in 0 until count) {
                if (!modules[r][c]) continue
                if (!shouldDrawModule(r, c, count, config, x0, y0, dot)) continue
                val cx = x0 + c * dot + dot / 2
                val cy = y0 + r * dot + dot / 2
                sb.append(
                    """<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rr)}" """ +
                    """fill="none" stroke="$fillAttr" stroke-width="${fmt(strokeW)}"/>"""
                )
            }
        }
    }
    
    private fun renderIndividualModules(sb: StringBuilder, modules: Array<BooleanArray>, count: Int, x0: Double, y0: Double, dot: Double, config: QrStyleConfig, fillAttr: String) {
        val r = dot * config.modules.radiusFactor
        for (row in 0 until count) {
            for (col in 0 until count) {
                if (!modules[row][col]) continue
                if (!shouldDrawModule(row, col, count, config, x0, y0, dot)) continue
                
                val x = x0 + col * dot
                val y = y0 + row * dot
                val cx = x + dot / 2
                val cy = y + dot / 2
                
                val moduleColor = applyGradientMasking(config, fillAttr, cx, cy, config.layout.width / 2.0, config.layout.height / 2.0)
                
                when (config.modules.type) {
                    DotType.CIRCLE -> {
                        sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(r)}" fill="$moduleColor"/>""")
                    }
                    DotType.ROUNDED -> {
                        val rx = if (config.modules.rounded) r else dot * 0.2
                        sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$moduleColor"/>""")
                    }
                    DotType.EXTRA_ROUNDED -> {
                        val rx = dot * 0.45
                        sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$moduleColor"/>""")
                    }
                    DotType.CLASSY_ROUNDED -> {
                        val outerR = dot * 0.45
                        val innerR = outerR * 0.6
                        val strokeW = outerR - innerR
                        sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(outerR)}" ry="${fmt(outerR)}" """)
                        sb.append("""fill="none" stroke="$moduleColor" stroke-width="${fmt(strokeW)}"/>""")
                        sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(innerR * 0.7)}" fill="$moduleColor"/>""")
                    }
                    else -> {
                        val rx = if (config.modules.rounded) r else 0.0
                        sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(dot)}" height="${fmt(dot)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$moduleColor"/>""")
                    }
                }
            }
        }
    }
    
    private fun renderMicroTypography(sb: StringBuilder, config: QrStyleConfig) {
        config.advanced.microTypography?.let { typo ->
            if (typo.enabled && typo.text.isNotEmpty()) {
                when (typo.path) {
                    TypographyPath.CIRCULAR -> {
                        val radius = minOf(config.layout.width, config.layout.height) / 2.0 - typo.fontSize
                        sb.append("""<defs><path id="circularPath" d="M ${config.layout.width/2},${typo.fontSize} A $radius,$radius 0 1,1 ${config.layout.width/2-1},${typo.fontSize}"/></defs>""")
                        sb.append("""<text font-size="${typo.fontSize}" fill="${typo.color}">""")
                        sb.append("""<textPath href="#circularPath">${typo.text}</textPath>""")
                        sb.append("""</text>""")
                    }
                    TypographyPath.LINEAR_TOP -> {
                        sb.append("""<text x="${config.layout.width/2}" y="${typo.fontSize}" text-anchor="middle" font-size="${typo.fontSize}" fill="${typo.color}">${typo.text}</text>""")
                    }
                    TypographyPath.LINEAR_BOTTOM -> {
                        sb.append("""<text x="${config.layout.width/2}" y="${config.layout.height - 4}" text-anchor="middle" font-size="${typo.fontSize}" fill="${typo.color}">${typo.text}</text>""")
                    }
                }
            }
        }
    }
    
    // Helper functions
    
    private fun shouldDrawModule(row: Int, col: Int, count: Int, config: QrStyleConfig, x0: Double, y0: Double, dot: Double): Boolean {
        return QrAnalysis.shouldDrawModule(row, col, count, config, config.layout.width / 2.0, config.layout.height / 2.0, dot)
    }
    
    private fun applyGradientMasking(config: QrStyleConfig, defaultColor: String, x: Double, y: Double, centerX: Double, centerY: Double): String {
        val masking = config.advanced.gradientMasking ?: return defaultColor
        if (!masking.enabled) return defaultColor
        
        return when (masking.type) {
            MaskingType.CONCENTRIC -> {
                val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                val maxDistance = sqrt(centerX.pow(2) + centerY.pow(2))
                val ratio = (distance / maxDistance).coerceIn(0.0, 1.0)
                interpolateColor(masking.centerColor ?: defaultColor, masking.edgeColor ?: defaultColor, ratio)
            }
            MaskingType.RADIAL -> {
                val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                val maxDistance = minOf(centerX, centerY)
                val ratio = (distance / maxDistance).coerceIn(0.0, 1.0)
                interpolateColor(masking.centerColor ?: defaultColor, masking.edgeColor ?: defaultColor, ratio)
            }
            MaskingType.LINEAR -> {
                val ratio = (x / (centerX * 2)).coerceIn(0.0, 1.0)
                interpolateColor(masking.centerColor ?: defaultColor, masking.edgeColor ?: defaultColor, ratio)
            }
        }
    }
    
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
    
    private fun interpolateColor(startColor: String, endColor: String, ratio: Double): String {
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
    
    private fun drawBorder(sb: StringBuilder, border: BorderOptions, w: Double, h: Double) {
        fun rect(th: Double, col: String, rx: Double) {
            val off = th/2
            sb.append("""<rect x="${fmt(off)}" y="${fmt(off)}" width="${fmt(w-th)}" height="${fmt(h-th)}" fill="none" stroke="$col" stroke-width="${fmt(th)}" """)
            if (rx > 0) sb.append("""rx="${fmt(rx)}" ry="${fmt(rx)}" """)
            sb.append("/>")
        }
        rect(border.thickness, border.color, (minOf(w, h) / 2) * border.round)
        border.inner?.let { rect(it.thickness, it.color, (minOf(w, h) / 2) * it.round) }
        border.outer?.let { rect(it.thickness, it.color, (minOf(w, h) / 2) * it.round) }
    }
    
    private fun rectPath(sb: StringBuilder, x: Double, y: Double, w: Double, h: Double) {
        sb.append("M").append(fmt(x)).append(",").append(fmt(y))
            .append("h").append(fmt(w))
            .append("v").append(fmt(h))
            .append("h-").append(fmt(w))
            .append("z")
    }
    
    private fun linearEndpoints(rot: Double, w: Double, h: Double): Quad {
        val cx = w/2; val cy = h/2
        val dx = cos(rot); val dy = sin(rot)
        val half = maxOf(w, h)
        return Quad(cx - dx*half, cy - dy*half, cx + dx*half, cy + dy*half)
    }
    
    private data class Quad(val x1: Double, val y1: Double, val x2: Double, val y2: Double)
    
    private fun fmt(d: Double): String =
        if (d % 1.0 == 0.0) d.toInt().toString()
        else String.format("%.2f", d)
    
    private fun fmtPct(d: Double): String = String.format("%.2f%%", d*100.0)
} 