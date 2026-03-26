package io.github.qrgen.svg

import io.github.qrgen.core.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** SVG renderer interface **/
interface QrSvgRenderer {
    fun render(qrResult: QrResult): String
}

/** Default SVG renderer implementation **/
class DefaultSvgRenderer : QrSvgRenderer {

    override fun render(qrResult: QrResult): String {
        val config = qrResult.config
        val count = qrResult.size

        val minSize = minOf(config.layout.width, config.layout.height) - 2 * config.layout.margin
        val realSize = if (config.layout.circleShape) (minSize / sqrt(2.0)).toInt() else minSize
        val dot = realSize.toDouble() / count
        val x0 = (config.layout.width - count * dot) / 2.0
        val y0 = (config.layout.height - count * dot) / 2.0

        val sb = StringBuilder(75000)
        sb.append("""<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${config.layout.width}" height="${config.layout.height}" viewBox="0 0 ${config.layout.width} ${config.layout.height}">""")
        sb.append("<defs>")
        generateDefs(sb, config)
        sb.append("</defs>")

        renderBackground(sb, config)
        renderBorders(sb, config)
        renderQuietZoneAccent(sb, config, x0, y0, dot, count)

        val clipId = if (config.layout.circleShape) "clipCircle" else null
        sb.append("<g")
        if (clipId != null) sb.append(""" clip-path="url(#$clipId)"""")
        sb.append(">")

        renderCenterImage(sb, config)
        renderLocators(sb, qrResult, x0, y0, dot)
        renderAlignmentPatterns(sb, qrResult, x0, y0, dot)
        renderModules(sb, qrResult, x0, y0, dot)
        sb.append("</g>")

        renderMicroTypography(sb, config)
        sb.append("</svg>")
        return sb.toString()
    }

    private fun generateDefs(sb: StringBuilder, config: QrStyleConfig) {
        config.advanced.backgroundPattern?.takeIf { it.enabled }?.let { generateBackgroundPattern(sb, it) }
        config.advanced.dropShadow?.takeIf { it.enabled }?.let { generateDropShadowFilter(sb, it) }
        config.gradient.type?.let { generateGradient(sb, config.gradient, config.layout) }
        if (config.layout.circleShape) {
            val r = minOf(config.layout.width, config.layout.height) / 2
            sb.append("""<clipPath id="clipCircle"><circle cx="${config.layout.width / 2}" cy="${config.layout.height / 2}" r="$r"/></clipPath>""")
        }
    }

    private fun renderBackground(sb: StringBuilder, config: QrStyleConfig) {
        val bgFill = if (config.advanced.backgroundPattern?.enabled == true) "url(#bgPattern)" else config.colors.background
        bgFill?.let {
            val radius = config.layout.backgroundCornerRadius
            sb.append("""<rect width="${config.layout.width}" height="${config.layout.height}" fill="$it"""")
            if (!radius.isNearZero()) {
                sb.append(""" rx="${fmt(radius)}" ry="${fmt(radius)}"""")
            }
            sb.append("/>")
        }
    }

    private fun renderBorders(sb: StringBuilder, config: QrStyleConfig) {
        if (config.border.thickness > 0) {
            drawBorder(sb, config.border, config.layout.width.toDouble(), config.layout.height.toDouble())
        }
    }

    private fun renderQuietZoneAccent(sb: StringBuilder, config: QrStyleConfig, x0: Double, y0: Double, dot: Double, count: Int) {
        config.advanced.quietZoneAccent?.takeIf { it.enabled }?.let { accent ->
            val accentX = x0 - dot
            val accentY = y0 - dot
            val accentSize = (count + 2) * dot
            sb.append("""<rect x="${fmt(accentX)}" y="${fmt(accentY)}" width="${fmt(accentSize)}" height="${fmt(accentSize)}" """)
            sb.append("""fill="none" stroke="${accent.color}" stroke-width="${fmt(accent.width)}" stroke-dasharray="${accent.dashArray}"/>""")
        }
    }

    private fun renderCenterImage(sb: StringBuilder, config: QrStyleConfig) {
        config.logo.href?.let { href ->
            val realSize = minOf(config.layout.width, config.layout.height) - 2 * config.layout.margin
            val imgSize = realSize * config.logo.sizeRatio
            val ix = (config.layout.width - imgSize) / 2.0
            val iy = (config.layout.height - imgSize) / 2.0
            sb.append(
                """<image href="$href" xlink:href="$href" x="${fmt(ix)}" y="${fmt(iy)}" width="${fmt(imgSize)}" height="${fmt(imgSize)}" preserveAspectRatio="xMidYMid meet"/>"""
            )
        }
    }

    private fun renderLocators(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double) {
        val config = qrResult.config
        if (!config.locators.enabled) return
        val count = qrResult.size
        val positions = mapOf(
            LocatorPosition.TOP_LEFT to (0 to 0),
            LocatorPosition.TOP_RIGHT to ((count - 7) to 0),
            LocatorPosition.BOTTOM_LEFT to (0 to (count - 7))
        )
        positions.forEach { (position, modulePos) ->
            val style = config.locators.styleFor(position) ?: return@forEach
            val x = x0 + modulePos.first * dot
            val y = y0 + modulePos.second * dot
            renderLocator(sb, x, y, dot, style)
        }
    }

    private fun renderLocator(sb: StringBuilder, x: Double, y: Double, dot: Double, style: LocatorCornerStyle) {
        val sizePx = dot * style.sizeRatio
        val outerColor = style.outerColor ?: style.color
        val innerColor = style.innerColor ?: style.color
        renderLocatorFrame(sb, x, y, sizePx, style, outerColor)

        val inset = sizePx * 0.25
        val innerSize = sizePx - inset * 2
        renderLocatorDot(sb, x + inset, y + inset, innerSize, style, innerColor)

        style.logo.href?.let { href ->
            val logoSize = innerSize * style.logo.sizeRatio.coerceIn(0.2, 0.8)
            val logoInset = (sizePx - logoSize) / 2.0
            sb.append(
                """<image href="$href" xlink:href="$href" x="${fmt(x + logoInset)}" y="${fmt(y + logoInset)}" width="${fmt(logoSize)}" height="${fmt(logoSize)}" preserveAspectRatio="xMidYMid meet"/>"""
            )
        }
    }

    private fun renderLocatorFrame(sb: StringBuilder, x: Double, y: Double, size: Double, style: LocatorCornerStyle, color: String) {
        when (style.outerShape) {
            LocatorFrameShape.SQUARE -> sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(size)}" height="${fmt(size)}" fill="$color"/>""")
            LocatorFrameShape.CIRCLE -> {
                val r = size / 2
                sb.append("""<circle cx="${fmt(x + r)}" cy="${fmt(y + r)}" r="${fmt(r)}" fill="$color"/>""")
            }
            LocatorFrameShape.ROUNDED -> {
                val rx = size * style.radiusFactor
                sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(size)}" height="${fmt(size)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$color"/>""")
            }
            LocatorFrameShape.CLASSY -> {
                val cx = x + size / 2
                val cy = y + size / 2
                val sw = size * 0.16
                val radius = size / 2 - sw / 2
                sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(radius)}" fill="none" stroke="$color" stroke-width="${fmt(sw)}"/>""")
            }
            LocatorFrameShape.DIAMOND -> {
                sb.append("""<path d="${diamondPath(x, y, size, size)}" fill="$color"/>""")
            }
        }
    }

    private fun renderLocatorDot(sb: StringBuilder, x: Double, y: Double, size: Double, style: LocatorCornerStyle, color: String) {
        when (style.innerShape) {
            LocatorDotShape.SQUARE -> sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(size)}" height="${fmt(size)}" fill="$color"/>""")
            LocatorDotShape.CIRCLE -> {
                val r = size / 2
                sb.append("""<circle cx="${fmt(x + r)}" cy="${fmt(y + r)}" r="${fmt(r)}" fill="$color"/>""")
            }
            LocatorDotShape.ROUNDED -> {
                val rx = size * style.radiusFactor
                sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(size)}" height="${fmt(size)}" rx="${fmt(rx)}" ry="${fmt(rx)}" fill="$color"/>""")
            }
            LocatorDotShape.DIAMOND -> {
                sb.append("""<path d="${diamondPath(x, y, size, size)}" fill="$color"/>""")
            }
        }
    }

    private fun renderAlignmentPatterns(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double) {
        val options = qrResult.config.alignmentPatterns
        if (!options.enabled) return
        val color = options.color ?: if (qrResult.config.gradient.type != null) "url(#grad0)" else qrResult.config.colors.foreground
        val sizePx = dot * (5.0 * options.sizeRatio.coerceIn(0.5, 1.1))
        QrAnalysis.alignmentCenters(qrResult.qrCode.version).forEach { (row, col) ->
            val x = x0 + (col - 2) * dot + (5 * dot - sizePx) / 2.0
            val y = y0 + (row - 2) * dot + (5 * dot - sizePx) / 2.0
            when (options.shape) {
                AlignmentPatternShape.SQUARE -> sb.append("""<rect x="${fmt(x)}" y="${fmt(y)}" width="${fmt(sizePx)}" height="${fmt(sizePx)}" fill="$color"/>""")
                AlignmentPatternShape.CIRCLE -> {
                    val r = sizePx / 2
                    sb.append("""<circle cx="${fmt(x + r)}" cy="${fmt(y + r)}" r="${fmt(r)}" fill="$color"/>""")
                }
                AlignmentPatternShape.DIAMOND -> sb.append("""<path d="${diamondPath(x, y, sizePx, sizePx)}" fill="$color"/>""")
                AlignmentPatternShape.STAR -> sb.append("""<path d="${starPath(x + sizePx / 2, y + sizePx / 2, sizePx / 2)}" fill="$color"/>""")
            }
        }
    }

    private fun renderModules(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double) {
        val config = qrResult.config
        val fillAttr = if (config.gradient.type != null) "url(#grad0)" else config.colors.foreground
        val groupAttrs = buildString {
            append(""" fill="$fillAttr"""")
            config.advanced.moduleOutline?.takeIf { it.enabled }?.let {
                append(""" stroke="${it.color}" stroke-width="${fmt(it.width)}"""")
            }
            config.advanced.dropShadow?.takeIf { it.enabled }?.let {
                append(""" filter="url(#dropShadow)"""")
            }
            if (!config.modules.roundSize) {
                append(""" shape-rendering="crispEdges"""")
            }
        }
        sb.append("<g$groupAttrs>")

        when (config.modules.type) {
            DotType.SQUARE -> renderSquareModules(sb, qrResult, x0, y0, dot, fillAttr)
            DotType.CLASSY -> renderClassyModules(sb, qrResult, x0, y0, dot, fillAttr)
            DotType.ROUNDED, DotType.EXTRA_ROUNDED, DotType.CLASSY_ROUNDED -> renderRoundedModules(sb, qrResult, x0, y0, dot, fillAttr)
            DotType.CIRCLE -> renderCircleModules(sb, qrResult, x0, y0, dot, fillAttr)
        }

        sb.append("</g>")
    }

    private fun renderSquareModules(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double, fillAttr: String) {
        val path = StringBuilder(qrResult.size * 20)
        for (row in 0 until qrResult.size) {
            var run = 0
            var startCol = 0
            for (col in 0 until qrResult.size) {
                val draw = qrResult.modules[row][col] && QrAnalysis.shouldDrawModule(row, col, qrResult, x0, y0, dot)
                if (draw) {
                    if (run == 0) startCol = col
                    run++
                } else if (run > 0) {
                    val x = x0 + startCol * dot
                    val y = y0 + row * dot
                    rectPath(path, x, y, run * dot, dot)
                    run = 0
                }
            }
            if (run > 0) {
                rectPath(path, x0 + startCol * dot, y0 + row * dot, run * dot, dot)
            }
        }
        sb.append("""<path d="$path" fill="$fillAttr">${animationMarkup(qrResult.config.animation)}</path>""")
    }

    private fun renderCircleModules(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double, fillAttr: String) {
        val baseRadius = dot * qrResult.config.modules.radiusFactor
        forEachDrawableModule(qrResult, x0, y0, dot) { row, col, x, y ->
            val bounds = QrAnalysis.scaledModuleBounds(x, y, dot, qrResult.config.modules, row, col)
            val cx = bounds.x + bounds.width / 2
            val cy = bounds.y + bounds.height / 2
            val color = applyGradientMasking(qrResult.config, fillAttr, cx, cy, qrResult.config.layout.width / 2.0, qrResult.config.layout.height / 2.0)
            val radius = min(bounds.width, bounds.height) / 2.0
            sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(min(radius, baseRadius))}" fill="$color">${animationMarkup(qrResult.config.animation)}</circle>""")
        }
    }

    private fun renderClassyModules(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double, fillAttr: String) {
        val strokeW = dot * 0.2
        forEachDrawableModule(qrResult, x0, y0, dot) { row, col, x, y ->
            val bounds = QrAnalysis.scaledModuleBounds(x, y, dot, qrResult.config.modules, row, col)
            val cx = bounds.x + bounds.width / 2
            val cy = bounds.y + bounds.height / 2
            val rr = min(bounds.width, bounds.height) / 2 - strokeW / 2
            val color = applyGradientMasking(qrResult.config, fillAttr, cx, cy, qrResult.config.layout.width / 2.0, qrResult.config.layout.height / 2.0)
            sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(rr)}" fill="none" stroke="$color" stroke-width="${fmt(strokeW)}">${animationMarkup(qrResult.config.animation)}</circle>""")
        }
    }

    private fun renderRoundedModules(sb: StringBuilder, qrResult: QrResult, x0: Double, y0: Double, dot: Double, fillAttr: String) {
        forEachDrawableModule(qrResult, x0, y0, dot) { row, col, x, y ->
            val bounds = QrAnalysis.scaledModuleBounds(x, y, dot, qrResult.config.modules, row, col)
            val neighbors = QrAnalysis.moduleNeighbors(qrResult.modules, row, col)
            val baseRadius = when (qrResult.config.modules.type) {
                DotType.EXTRA_ROUNDED -> min(bounds.width, bounds.height) * 0.48
                DotType.CLASSY_ROUNDED -> min(bounds.width, bounds.height) * 0.42
                else -> min(bounds.width, bounds.height) * 0.28
            }
            val path = roundedRectPath(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                neighbors.cornerRadius(Corner.TOP_LEFT, baseRadius),
                neighbors.cornerRadius(Corner.TOP_RIGHT, baseRadius),
                neighbors.cornerRadius(Corner.BOTTOM_RIGHT, baseRadius),
                neighbors.cornerRadius(Corner.BOTTOM_LEFT, baseRadius)
            )
            val cx = bounds.x + bounds.width / 2
            val cy = bounds.y + bounds.height / 2
            val color = applyGradientMasking(qrResult.config, fillAttr, cx, cy, qrResult.config.layout.width / 2.0, qrResult.config.layout.height / 2.0)
            sb.append("""<path d="$path" fill="$color">${animationMarkup(qrResult.config.animation)}</path>""")
            if (qrResult.config.modules.type == DotType.CLASSY_ROUNDED) {
                val innerRadius = min(bounds.width, bounds.height) * 0.16
                sb.append("""<circle cx="${fmt(cx)}" cy="${fmt(cy)}" r="${fmt(innerRadius)}" fill="$color">${animationMarkup(qrResult.config.animation)}</circle>""")
            }
        }
    }

    private inline fun forEachDrawableModule(
        qrResult: QrResult,
        x0: Double,
        y0: Double,
        dot: Double,
        block: (row: Int, col: Int, x: Double, y: Double) -> Unit
    ) {
        for (row in 0 until qrResult.size) {
            for (col in 0 until qrResult.size) {
                if (!qrResult.modules[row][col]) continue
                if (!QrAnalysis.shouldDrawModule(row, col, qrResult, x0, y0, dot)) continue
                block(row, col, x0 + col * dot, y0 + row * dot)
            }
        }
    }

    private fun animationMarkup(options: AnimationOptions): String {
        if (!options.enabled) return ""
        val duration = "${options.durationSeconds}s"
        return when (options.preset) {
            AnimationPreset.FADE -> """<animate attributeName="opacity" values="0.55;1;0.55" dur="$duration" repeatCount="${options.repeatCount}"/>"""
            AnimationPreset.PULSE -> """<animateTransform attributeName="transform" type="scale" values="1;1.06;1" dur="$duration" repeatCount="${options.repeatCount}"/>"""
            AnimationPreset.DRAW_IN -> """<animate attributeName="opacity" values="0;1" dur="$duration" repeatCount="1" fill="freeze"/>"""
        }
    }

    private fun generateBackgroundPattern(sb: StringBuilder, pattern: BackgroundPattern) {
        sb.append("""<pattern id="bgPattern" patternUnits="userSpaceOnUse" width="${pattern.size}" height="${pattern.size}">""")
        when (pattern.type) {
            PatternType.DOTS -> {
                val r = pattern.size * 0.2
                sb.append("""<circle cx="${pattern.size / 2}" cy="${pattern.size / 2}" r="$r" fill="${pattern.color}" opacity="${pattern.opacity}"/>""")
            }
            PatternType.GRID -> {
                sb.append("""<rect width="${pattern.size}" height="${pattern.size}" fill="none" stroke="${pattern.color}" stroke-width="0.5" opacity="${pattern.opacity}"/>""")
            }
            PatternType.DIAGONAL_LINES -> {
                sb.append("""<path d="M0,0 L${pattern.size},${pattern.size} M0,${pattern.size} L${pattern.size},0" stroke="${pattern.color}" stroke-width="0.5" opacity="${pattern.opacity}"/>""")
            }
            PatternType.HEXAGON -> {
                val hex = createHexagon(pattern.size * 0.4, pattern.size / 2, pattern.size / 2)
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
                sb.append("""<radialGradient id="grad0" cx="${layout.width / 2}" cy="${layout.height / 2}" r="${maxOf(layout.width, layout.height) / 2}">""")
                gradient.stops.forEach { sb.append("""<stop offset="${fmtPct(it.offset)}" stop-color="${it.color}"/>""") }
                sb.append("</radialGradient>")
            }
            null -> Unit
        }
    }

    private fun renderMicroTypography(sb: StringBuilder, config: QrStyleConfig) {
        config.advanced.microTypography?.takeIf { it.enabled && it.text.isNotEmpty() }?.let { typo ->
            when (typo.path) {
                TypographyPath.CIRCULAR -> {
                    val radius = min(config.layout.width, config.layout.height) / 2.0 - typo.fontSize
                    val pathId = "circularPath"
                    sb.append("""<defs><path id="$pathId" d="M ${config.layout.width / 2},${typo.fontSize} A $radius,$radius 0 1,1 ${config.layout.width / 2 - 1},${typo.fontSize}"/></defs>""")
                    sb.append("""<text font-size="${typo.fontSize}" fill="${typo.color}"><textPath href="#$pathId">${typo.text}</textPath></text>""")
                }
                TypographyPath.LINEAR_TOP -> {
                    sb.append("""<text x="${config.layout.width / 2}" y="${typo.fontSize}" text-anchor="middle" font-size="${typo.fontSize}" fill="${typo.color}">${typo.text}</text>""")
                }
                TypographyPath.LINEAR_BOTTOM -> {
                    sb.append("""<text x="${config.layout.width / 2}" y="${config.layout.height - 4}" text-anchor="middle" font-size="${typo.fontSize}" fill="${typo.color}">${typo.text}</text>""")
                }
            }
        }
    }
}

private fun drawBorder(sb: StringBuilder, b: BorderOptions, w: Double, h: Double) {
    fun rect(th: Double, col: String, rx: Double) {
        val off = th / 2
        sb.append("""<rect x="${fmt(off)}" y="${fmt(off)}" width="${fmt(w - th)}" height="${fmt(h - th)}" fill="none" stroke="$col" stroke-width="${fmt(th)}" """)
        if (rx > 0) sb.append("""rx="${fmt(rx)}" ry="${fmt(rx)}" """)
        sb.append("/>")
    }
    rect(b.thickness, b.color, (min(w, h) / 2) * b.round)
    b.inner?.let { rect(it.thickness, it.color, (min(w, h) / 2) * it.round) }
    b.outer?.let { rect(it.thickness, it.color, (min(w, h) / 2) * it.round) }
}

private fun applyGradientMasking(config: QrStyleConfig, defaultColor: String, x: Double, y: Double, centerX: Double, centerY: Double): String {
    val masking = config.advanced.gradientMasking ?: return defaultColor
    if (!masking.enabled) return defaultColor
    return when (masking.type) {
        MaskingType.CONCENTRIC, MaskingType.RADIAL, MaskingType.LINEAR -> interpolateColor(
            masking.centerColor ?: defaultColor,
            masking.edgeColor ?: defaultColor,
            ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).let { kotlin.math.sqrt(it) } /
                kotlin.math.sqrt(centerX * centerX + centerY * centerY)
        )
    }
}

private fun interpolateColor(start: String, end: String, ratio: Double): String {
    if (!start.startsWith("#") || !end.startsWith("#") || start.length < 7 || end.length < 7) return start
    val clamped = ratio.coerceIn(0.0, 1.0)
    fun channel(color: String, offset: Int): Int = color.substring(offset, offset + 2).toInt(16)
    val r = (channel(start, 1) + ((channel(end, 1) - channel(start, 1)) * clamped)).toInt()
    val g = (channel(start, 3) + ((channel(end, 3) - channel(start, 3)) * clamped)).toInt()
    val b = (channel(start, 5) + ((channel(end, 5) - channel(start, 5)) * clamped)).toInt()
    return "#%02x%02x%02x".format(r, g, b)
}

private fun rectPath(sb: StringBuilder, x: Double, y: Double, w: Double, h: Double) {
    sb.append("M").append(fmt(x)).append(",").append(fmt(y))
        .append("h").append(fmt(w))
        .append("v").append(fmt(h))
        .append("h-").append(fmt(w))
        .append("z")
}

private fun roundedRectPath(x: Double, y: Double, w: Double, h: Double, tl: Double, tr: Double, br: Double, bl: Double): String {
    return buildString {
        append("M${fmt(x + tl)},${fmt(y)}")
        append("H${fmt(x + w - tr)}")
        if (!tr.isNearZero()) append("Q${fmt(x + w)},${fmt(y)} ${fmt(x + w)},${fmt(y + tr)}")
        append("V${fmt(y + h - br)}")
        if (!br.isNearZero()) append("Q${fmt(x + w)},${fmt(y + h)} ${fmt(x + w - br)},${fmt(y + h)}")
        append("H${fmt(x + bl)}")
        if (!bl.isNearZero()) append("Q${fmt(x)},${fmt(y + h)} ${fmt(x)},${fmt(y + h - bl)}")
        append("V${fmt(y + tl)}")
        if (!tl.isNearZero()) append("Q${fmt(x)},${fmt(y)} ${fmt(x + tl)},${fmt(y)}")
        append("Z")
    }
}

private fun diamondPath(x: Double, y: Double, w: Double, h: Double): String {
    val cx = x + w / 2
    val cy = y + h / 2
    return "M${fmt(cx)},${fmt(y)} L${fmt(x + w)},${fmt(cy)} L${fmt(cx)},${fmt(y + h)} L${fmt(x)},${fmt(cy)} Z"
}

private fun starPath(cx: Double, cy: Double, radius: Double): String {
    val inner = radius * 0.42
    return buildString {
        for (i in 0 until 10) {
            val angle = -PI / 2 + i * PI / 5
            val r = if (i % 2 == 0) radius else inner
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            append(if (i == 0) "M" else " L")
            append(fmt(x)).append(",").append(fmt(y))
        }
        append(" Z")
    }
}

private fun linearEndpoints(rot: Double, w: Double, h: Double): Quad {
    val cx = w / 2
    val cy = h / 2
    val dx = cos(rot)
    val dy = sin(rot)
    val half = max(w, h)
    return Quad(cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half)
}

private data class Quad(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

private fun fmt(d: Double): String =
    if (d % 1.0 == 0.0) d.toInt().toString() else String.format("%.2f", d)

private fun fmtPct(d: Double): String = String.format("%.2f%%", d * 100.0)

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
