package io.github.qrgen.core

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

@JsonDeserialize(using = OptionalFieldDeserializer::class)
data class OptionalField<T>(
    val isPresent: Boolean = false,
    val value: T? = null
) {
    companion object {
        fun <T> absent(): OptionalField<T> = OptionalField(false, null)
        fun <T> present(value: T?): OptionalField<T> = OptionalField(true, value)
    }
}

/** Jackson deserializer that preserves whether a template field was absent or explicitly provided. */
class OptionalFieldDeserializer(
    private val valueType: JavaType? = null
) : JsonDeserializer<OptionalField<*>>(), ContextualDeserializer {
    override fun getNullValue(ctxt: DeserializationContext): OptionalField<*> = OptionalField.present<Any?>(null)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): OptionalField<*> {
        val codec = p.codec
        val node = codec.readTree<com.fasterxml.jackson.databind.JsonNode>(p)
        if (node.isNull) return OptionalField.present<Any?>(null)
        val type = requireNotNull(valueType) { "OptionalFieldDeserializer missing contextual type" }
        val parser = node.traverse(codec)
        parser.nextToken()
        val value = ctxt.readValue<Any?>(parser, type)
        return OptionalField.present(value)
    }

    override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> {
        val wrapperType = property?.type ?: ctxt.contextualType
        return OptionalFieldDeserializer(wrapperType.containedTypeOrUnknown(0))
    }
}

/** Partial template document used for JSON/YAML profile and template loading. */
data class QrTemplateDocument(
    val profile: String? = null,
    val config: QrStyleConfigPatch = QrStyleConfigPatch()
)

/** Field-by-field patch form of [QrStyleConfig] used during template/profile merging. */
data class QrStyleConfigPatch(
    val qrOptions: OptionalField<QrOptionsPatch> = OptionalField.absent(),
    val layout: OptionalField<LayoutOptionsPatch> = OptionalField.absent(),
    val modules: OptionalField<ModuleOptionsPatch> = OptionalField.absent(),
    val colors: OptionalField<ColorOptionsPatch> = OptionalField.absent(),
    val logo: OptionalField<LogoOptionsPatch> = OptionalField.absent(),
    val locators: OptionalField<LocatorOptionsPatch> = OptionalField.absent(),
    val alignmentPatterns: OptionalField<AlignmentPatternOptionsPatch> = OptionalField.absent(),
    val gradient: OptionalField<GradientOptionsPatch> = OptionalField.absent(),
    val border: OptionalField<BorderOptionsPatch> = OptionalField.absent(),
    val animation: OptionalField<AnimationOptionsPatch> = OptionalField.absent(),
    val raster: OptionalField<RasterOptionsPatch> = OptionalField.absent(),
    val cache: OptionalField<CacheOptionsPatch> = OptionalField.absent(),
    val advanced: OptionalField<AdvancedOptionsPatch> = OptionalField.absent()
) {
    companion object {
        fun fromConfig(config: QrStyleConfig): QrStyleConfigPatch {
            return QrStyleConfigPatch(
                qrOptions = OptionalField.present(QrOptionsPatch.fromConfig(config.qrOptions)),
                layout = OptionalField.present(LayoutOptionsPatch.fromConfig(config.layout)),
                modules = OptionalField.present(ModuleOptionsPatch.fromConfig(config.modules)),
                colors = OptionalField.present(ColorOptionsPatch.fromConfig(config.colors)),
                logo = OptionalField.present(LogoOptionsPatch.fromConfig(config.logo)),
                locators = OptionalField.present(LocatorOptionsPatch.fromConfig(config.locators)),
                alignmentPatterns = OptionalField.present(AlignmentPatternOptionsPatch.fromConfig(config.alignmentPatterns)),
                gradient = OptionalField.present(GradientOptionsPatch.fromConfig(config.gradient)),
                border = OptionalField.present(BorderOptionsPatch.fromConfig(config.border)),
                animation = OptionalField.present(AnimationOptionsPatch.fromConfig(config.animation)),
                raster = OptionalField.present(RasterOptionsPatch.fromConfig(config.raster)),
                cache = OptionalField.present(CacheOptionsPatch.fromConfig(config.cache)),
                advanced = OptionalField.present(AdvancedOptionsPatch.fromConfig(config.advanced))
            )
        }
    }
}

data class QrOptionsPatch(
    val ecc: String? = null,
    val mask: Int? = null,
    val minVersion: Int? = null,
    val maxVersion: Int? = null
) {
    companion object { fun fromConfig(config: QrOptions) = QrOptionsPatch(config.ecc.name, config.mask, config.minVersion, config.maxVersion) }
}

data class LayoutOptionsPatch(
    val width: Int? = null,
    val height: Int? = null,
    val margin: Int? = null,
    val circleShape: Boolean? = null,
    val backgroundCornerRadius: Double? = null
) {
    companion object { fun fromConfig(config: LayoutOptions) = LayoutOptionsPatch(config.width, config.height, config.margin, config.circleShape, config.backgroundCornerRadius) }
}

data class ModuleOptionsPatch(
    val type: DotType? = null,
    val radiusFactor: Double? = null,
    val rounded: Boolean? = null,
    val extraRounded: Boolean? = null,
    val classyRounded: Boolean? = null,
    val roundSize: Boolean? = null,
    val sizeScale: Double? = null
) {
    companion object { fun fromConfig(config: ModuleOptions) = ModuleOptionsPatch(config.type, config.radiusFactor, config.rounded, config.extraRounded, config.classyRounded, config.roundSize, config.sizeScale) }
}

data class ColorOptionsPatch(
    val foreground: String? = null,
    val background: OptionalField<String> = OptionalField.absent()
) {
    companion object { fun fromConfig(config: ColorOptions) = ColorOptionsPatch(config.foreground, OptionalField.present(config.background)) }
}

data class LogoOptionsPatch(
    val href: OptionalField<String> = OptionalField.absent(),
    val sizeRatio: Double? = null,
    val holeRadiusPx: OptionalField<Double> = OptionalField.absent()
) {
    companion object { fun fromConfig(config: LogoOptions) = LogoOptionsPatch(OptionalField.present(config.href), config.sizeRatio, OptionalField.present(config.holeRadiusPx)) }
}

data class LocatorLogoOptionsPatch(
    val href: OptionalField<String> = OptionalField.absent(),
    val sizeRatio: Double? = null
) {
    companion object { fun fromConfig(config: LocatorLogoOptions) = LocatorLogoOptionsPatch(OptionalField.present(config.href), config.sizeRatio) }
}

data class LocatorCornerStylePatch(
    val enabled: Boolean? = null,
    val outerShape: LocatorFrameShape? = null,
    val innerShape: LocatorDotShape? = null,
    val color: String? = null,
    val outerColor: OptionalField<String> = OptionalField.absent(),
    val innerColor: OptionalField<String> = OptionalField.absent(),
    val sizeRatio: Double? = null,
    val radiusFactor: Double? = null,
    val logo: OptionalField<LocatorLogoOptionsPatch> = OptionalField.absent()
) {
    companion object {
        fun fromConfig(config: LocatorCornerStyle) = LocatorCornerStylePatch(
            enabled = config.enabled,
            outerShape = config.outerShape,
            innerShape = config.innerShape,
            color = config.color,
            outerColor = OptionalField.present(config.outerColor),
            innerColor = OptionalField.present(config.innerColor),
            sizeRatio = config.sizeRatio,
            radiusFactor = config.radiusFactor,
            logo = OptionalField.present(LocatorLogoOptionsPatch.fromConfig(config.logo))
        )
    }
}

data class LocatorOptionsPatch(
    val enabled: Boolean? = null,
    val defaultStyle: OptionalField<LocatorCornerStylePatch> = OptionalField.absent(),
    val topLeft: OptionalField<LocatorCornerStylePatch> = OptionalField.absent(),
    val topRight: OptionalField<LocatorCornerStylePatch> = OptionalField.absent(),
    val bottomLeft: OptionalField<LocatorCornerStylePatch> = OptionalField.absent()
) {
    companion object {
        fun fromConfig(config: LocatorOptions) = LocatorOptionsPatch(
            enabled = config.enabled,
            defaultStyle = OptionalField.present(LocatorCornerStylePatch.fromConfig(config.defaultStyle)),
            topLeft = OptionalField.present(config.topLeft?.let(LocatorCornerStylePatch::fromConfig)),
            topRight = OptionalField.present(config.topRight?.let(LocatorCornerStylePatch::fromConfig)),
            bottomLeft = OptionalField.present(config.bottomLeft?.let(LocatorCornerStylePatch::fromConfig))
        )
    }
}

data class AlignmentPatternOptionsPatch(
    val enabled: Boolean? = null,
    val shape: AlignmentPatternShape? = null,
    val color: OptionalField<String> = OptionalField.absent(),
    val sizeRatio: Double? = null
) {
    companion object { fun fromConfig(config: AlignmentPatternOptions) = AlignmentPatternOptionsPatch(config.enabled, config.shape, OptionalField.present(config.color), config.sizeRatio) }
}

data class GradientOptionsPatch(
    val type: OptionalField<GradientType> = OptionalField.absent(),
    val stops: OptionalField<List<ColorStop>> = OptionalField.absent(),
    val rotationRad: Double? = null
) {
    companion object { fun fromConfig(config: GradientOptions) = GradientOptionsPatch(OptionalField.present(config.type), OptionalField.present(config.stops), config.rotationRad) }
}

data class BorderOptionsPatch(
    val thickness: Double? = null,
    val color: String? = null,
    val round: Double? = null,
    val inner: OptionalField<BorderOptionsPatch> = OptionalField.absent(),
    val outer: OptionalField<BorderOptionsPatch> = OptionalField.absent()
) {
    companion object {
        fun fromConfig(config: BorderOptions): BorderOptionsPatch = BorderOptionsPatch(
            config.thickness,
            config.color,
            config.round,
            OptionalField.present(config.inner?.let { fromConfig(it) }),
            OptionalField.present(config.outer?.let { fromConfig(it) })
        )
    }
}

data class AnimationOptionsPatch(
    val enabled: Boolean? = null,
    val preset: AnimationPreset? = null,
    val durationSeconds: Double? = null,
    val repeatCount: String? = null
) {
    companion object { fun fromConfig(config: AnimationOptions) = AnimationOptionsPatch(config.enabled, config.preset, config.durationSeconds, config.repeatCount) }
}

data class RasterOptionsPatch(
    val jpegQuality: Float? = null,
    val dpi: Float? = null
) {
    companion object { fun fromConfig(config: RasterOptions) = RasterOptionsPatch(config.jpegQuality, config.dpi) }
}

data class CacheOptionsPatch(
    val enabled: Boolean? = null,
    val maxEntries: Int? = null
) {
    companion object { fun fromConfig(config: CacheOptions) = CacheOptionsPatch(config.enabled, config.maxEntries) }
}

data class ModuleOutlinePatch(
    val enabled: Boolean? = null,
    val color: String? = null,
    val width: Double? = null
) {
    companion object { fun fromConfig(config: ModuleOutline) = ModuleOutlinePatch(config.enabled, config.color, config.width) }
}

data class QuietZoneAccentPatch(
    val enabled: Boolean? = null,
    val color: String? = null,
    val width: Double? = null,
    val dashArray: String? = null
) {
    companion object { fun fromConfig(config: QuietZoneAccent) = QuietZoneAccentPatch(config.enabled, config.color, config.width, config.dashArray) }
}

data class DropShadowPatch(
    val enabled: Boolean? = null,
    val blur: Double? = null,
    val opacity: Double? = null,
    val offsetX: Double? = null,
    val offsetY: Double? = null
) {
    companion object { fun fromConfig(config: DropShadow) = DropShadowPatch(config.enabled, config.blur, config.opacity, config.offsetX, config.offsetY) }
}

data class BackgroundPatternPatch(
    val enabled: Boolean? = null,
    val type: PatternType? = null,
    val color: String? = null,
    val opacity: Double? = null,
    val size: Double? = null
) {
    companion object { fun fromConfig(config: BackgroundPattern) = BackgroundPatternPatch(config.enabled, config.type, config.color, config.opacity, config.size) }
}

data class GradientMaskingPatch(
    val enabled: Boolean? = null,
    val type: MaskingType? = null,
    val centerColor: OptionalField<String> = OptionalField.absent(),
    val edgeColor: OptionalField<String> = OptionalField.absent()
) {
    companion object { fun fromConfig(config: GradientMasking) = GradientMaskingPatch(config.enabled, config.type, OptionalField.present(config.centerColor), OptionalField.present(config.edgeColor)) }
}

data class MicroTypographyPatch(
    val enabled: Boolean? = null,
    val text: String? = null,
    val fontSize: Double? = null,
    val color: String? = null,
    val path: TypographyPath? = null
) {
    companion object { fun fromConfig(config: MicroTypography) = MicroTypographyPatch(config.enabled, config.text, config.fontSize, config.color, config.path) }
}

data class AdvancedOptionsPatch(
    val moduleOutline: OptionalField<ModuleOutlinePatch> = OptionalField.absent(),
    val quietZoneAccent: OptionalField<QuietZoneAccentPatch> = OptionalField.absent(),
    val dropShadow: OptionalField<DropShadowPatch> = OptionalField.absent(),
    val backgroundPattern: OptionalField<BackgroundPatternPatch> = OptionalField.absent(),
    val gradientMasking: OptionalField<GradientMaskingPatch> = OptionalField.absent(),
    val microTypography: OptionalField<MicroTypographyPatch> = OptionalField.absent()
) {
    companion object {
        fun fromConfig(config: AdvancedOptions) = AdvancedOptionsPatch(
            OptionalField.present(config.moduleOutline?.let(ModuleOutlinePatch::fromConfig)),
            OptionalField.present(config.quietZoneAccent?.let(QuietZoneAccentPatch::fromConfig)),
            OptionalField.present(config.dropShadow?.let(DropShadowPatch::fromConfig)),
            OptionalField.present(config.backgroundPattern?.let(BackgroundPatternPatch::fromConfig)),
            OptionalField.present(config.gradientMasking?.let(GradientMaskingPatch::fromConfig)),
            OptionalField.present(config.microTypography?.let(MicroTypographyPatch::fromConfig))
        )
    }
}

/** Reads and writes QR style templates in JSON or YAML patch form. */
object QrConfigIO {
    private val jsonMapper = buildMapper(jacksonObjectMapper())
    private val yamlMapper = buildMapper(ObjectMapper(YAMLFactory()))

    fun readTemplate(content: String, formatHint: String? = null): QrTemplateDocument {
        val mapper = mapperFor(content, formatHint)
        return mapper.readValue(content)
    }

    fun readTemplate(file: File): QrTemplateDocument = readTemplate(file.readText(), file.extension)

    fun writeJson(document: QrTemplateDocument): String = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document)

    fun writeYaml(document: QrTemplateDocument): String = yamlMapper.writeValueAsString(document)

    private fun mapperFor(content: String, formatHint: String?): ObjectMapper {
        val normalizedHint = formatHint?.lowercase()
        return when {
            normalizedHint == "json" -> jsonMapper
            normalizedHint == "yaml" || normalizedHint == "yml" -> yamlMapper
            content.trimStart().startsWith("{") -> jsonMapper
            else -> yamlMapper
        }
    }

    private fun buildMapper(mapper: ObjectMapper): ObjectMapper {
        return mapper.findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }
}

/** Registry of named style profiles used as merge bases for templates and requests. */
class QrProfileRegistry(
    private val profiles: MutableMap<String, QrStyleConfig> = linkedMapOf()
) {
    fun register(name: String, config: QrStyleConfig) {
        profiles[name] = config
    }

    fun register(name: String, patch: QrStyleConfigPatch) {
        profiles[name] = QrStyleConfigMerger.merge(QrStyleConfig(), patch)
    }

    fun resolve(name: String?): QrStyleConfig? = name?.let { profiles[it] }

    fun merge(template: QrTemplateDocument): QrStyleConfig {
        val base = resolve(template.profile) ?: QrStyleConfig()
        return QrStyleConfigMerger.merge(base, template.config)
    }
}

/** Explicit field-by-field merger for [QrStyleConfigPatch] into [QrStyleConfig]. */
object QrStyleConfigMerger {
    fun merge(base: QrStyleConfig, override: QrStyleConfigPatch): QrStyleConfig {
        return base.copy(
            qrOptions = override.qrOptions.value?.let { merge(base.qrOptions, it) } ?: base.qrOptions,
            layout = override.layout.value?.let { merge(base.layout, it) } ?: base.layout,
            modules = override.modules.value?.let { merge(base.modules, it) } ?: base.modules,
            colors = override.colors.value?.let { merge(base.colors, it) } ?: base.colors,
            logo = override.logo.value?.let { merge(base.logo, it) } ?: base.logo,
            locators = override.locators.value?.let { merge(base.locators, it) } ?: base.locators,
            alignmentPatterns = override.alignmentPatterns.value?.let { merge(base.alignmentPatterns, it) } ?: base.alignmentPatterns,
            gradient = override.gradient.value?.let { merge(base.gradient, it) } ?: base.gradient,
            border = override.border.value?.let { merge(base.border, it) } ?: base.border,
            animation = override.animation.value?.let { merge(base.animation, it) } ?: base.animation,
            raster = override.raster.value?.let { merge(base.raster, it) } ?: base.raster,
            cache = override.cache.value?.let { merge(base.cache, it) } ?: base.cache,
            advanced = override.advanced.value?.let { merge(base.advanced, it) } ?: base.advanced
        )
    }

    private fun merge(base: QrOptions, patch: QrOptionsPatch) = base.copy(
        ecc = patch.ecc?.let { runCatching { io.nayuki.qrcodegen.QrCode.Ecc.valueOf(it) }.getOrDefault(base.ecc) } ?: base.ecc,
        mask = patch.mask ?: base.mask,
        minVersion = patch.minVersion ?: base.minVersion,
        maxVersion = patch.maxVersion ?: base.maxVersion
    )

    private fun merge(base: LayoutOptions, patch: LayoutOptionsPatch) = base.copy(
        width = patch.width ?: base.width,
        height = patch.height ?: base.height,
        margin = patch.margin ?: base.margin,
        circleShape = patch.circleShape ?: base.circleShape,
        backgroundCornerRadius = patch.backgroundCornerRadius ?: base.backgroundCornerRadius
    )

    private fun merge(base: ModuleOptions, patch: ModuleOptionsPatch) = base.copy(
        type = patch.type ?: base.type,
        radiusFactor = patch.radiusFactor ?: base.radiusFactor,
        rounded = patch.rounded ?: base.rounded,
        extraRounded = patch.extraRounded ?: base.extraRounded,
        classyRounded = patch.classyRounded ?: base.classyRounded,
        roundSize = patch.roundSize ?: base.roundSize,
        sizeScale = patch.sizeScale ?: base.sizeScale
    )

    private fun merge(base: ColorOptions, patch: ColorOptionsPatch) = base.copy(
        foreground = patch.foreground ?: base.foreground,
        background = if (patch.background.isPresent) patch.background.value else base.background
    )

    private fun merge(base: LogoOptions, patch: LogoOptionsPatch) = base.copy(
        href = if (patch.href.isPresent) patch.href.value else base.href,
        sizeRatio = patch.sizeRatio ?: base.sizeRatio,
        holeRadiusPx = if (patch.holeRadiusPx.isPresent) patch.holeRadiusPx.value else base.holeRadiusPx
    )

    private fun merge(base: LocatorLogoOptions, patch: LocatorLogoOptionsPatch) = base.copy(
        href = if (patch.href.isPresent) patch.href.value else base.href,
        sizeRatio = patch.sizeRatio ?: base.sizeRatio
    )

    private fun merge(base: LocatorCornerStyle, patch: LocatorCornerStylePatch) = base.copy(
        enabled = patch.enabled ?: base.enabled,
        outerShape = patch.outerShape ?: base.outerShape,
        innerShape = patch.innerShape ?: base.innerShape,
        color = patch.color ?: base.color,
        outerColor = if (patch.outerColor.isPresent) patch.outerColor.value else base.outerColor,
        innerColor = if (patch.innerColor.isPresent) patch.innerColor.value else base.innerColor,
        sizeRatio = patch.sizeRatio ?: base.sizeRatio,
        radiusFactor = patch.radiusFactor ?: base.radiusFactor,
        logo = patch.logo.value?.let { merge(base.logo, it) } ?: if (patch.logo.isPresent) LocatorLogoOptions() else base.logo
    )

    private fun merge(base: LocatorOptions, patch: LocatorOptionsPatch) = base.copy(
        enabled = patch.enabled ?: base.enabled,
        defaultStyle = patch.defaultStyle.value?.let { merge(base.defaultStyle, it) } ?: base.defaultStyle,
        topLeft = mergeOptional(base.topLeft, patch.topLeft),
        topRight = mergeOptional(base.topRight, patch.topRight),
        bottomLeft = mergeOptional(base.bottomLeft, patch.bottomLeft)
    )

    private fun mergeOptional(base: LocatorCornerStyle?, patch: OptionalField<LocatorCornerStylePatch>): LocatorCornerStyle? {
        if (!patch.isPresent) return base
        val patchValue = patch.value ?: return null
        val existing = base ?: LocatorCornerStyle()
        return merge(existing, patchValue)
    }

    private fun merge(base: AlignmentPatternOptions, patch: AlignmentPatternOptionsPatch) = base.copy(
        enabled = patch.enabled ?: base.enabled,
        shape = patch.shape ?: base.shape,
        color = if (patch.color.isPresent) patch.color.value else base.color,
        sizeRatio = patch.sizeRatio ?: base.sizeRatio
    )

    private fun merge(base: GradientOptions, patch: GradientOptionsPatch) = base.copy(
        type = if (patch.type.isPresent) patch.type.value else base.type,
        stops = if (patch.stops.isPresent) patch.stops.value ?: emptyList() else base.stops,
        rotationRad = patch.rotationRad ?: base.rotationRad
    )

    private fun merge(base: BorderOptions, patch: BorderOptionsPatch) = base.copy(
        thickness = patch.thickness ?: base.thickness,
        color = patch.color ?: base.color,
        round = patch.round ?: base.round,
        inner = mergeOptionalBorder(base.inner, patch.inner),
        outer = mergeOptionalBorder(base.outer, patch.outer)
    )

    private fun mergeOptionalBorder(base: BorderOptions?, patch: OptionalField<BorderOptionsPatch>): BorderOptions? {
        if (!patch.isPresent) return base
        val patchValue = patch.value ?: return null
        return merge(base ?: BorderOptions(), patchValue)
    }

    private fun merge(base: AnimationOptions, patch: AnimationOptionsPatch) = base.copy(
        enabled = patch.enabled ?: base.enabled,
        preset = patch.preset ?: base.preset,
        durationSeconds = patch.durationSeconds ?: base.durationSeconds,
        repeatCount = patch.repeatCount ?: base.repeatCount
    )

    private fun merge(base: RasterOptions, patch: RasterOptionsPatch) = base.copy(
        jpegQuality = patch.jpegQuality ?: base.jpegQuality,
        dpi = patch.dpi ?: base.dpi
    )

    private fun merge(base: CacheOptions, patch: CacheOptionsPatch) = base.copy(
        enabled = patch.enabled ?: base.enabled,
        maxEntries = patch.maxEntries ?: base.maxEntries
    )

    private fun merge(base: AdvancedOptions, patch: AdvancedOptionsPatch) = base.copy(
        moduleOutline = mergeOptionalModuleOutline(base.moduleOutline, patch.moduleOutline),
        quietZoneAccent = mergeOptionalQuietZone(base.quietZoneAccent, patch.quietZoneAccent),
        dropShadow = mergeOptionalDropShadow(base.dropShadow, patch.dropShadow),
        backgroundPattern = mergeOptionalBackgroundPattern(base.backgroundPattern, patch.backgroundPattern),
        gradientMasking = mergeOptionalGradientMasking(base.gradientMasking, patch.gradientMasking),
        microTypography = mergeOptionalMicroTypography(base.microTypography, patch.microTypography)
    )

    private fun mergeOptionalModuleOutline(base: ModuleOutline?, patch: OptionalField<ModuleOutlinePatch>): ModuleOutline? {
        if (!patch.isPresent) return base
        val p = patch.value ?: return null
        val current = base ?: ModuleOutline()
        return current.copy(enabled = p.enabled ?: current.enabled, color = p.color ?: current.color, width = p.width ?: current.width)
    }

    private fun mergeOptionalQuietZone(base: QuietZoneAccent?, patch: OptionalField<QuietZoneAccentPatch>): QuietZoneAccent? {
        if (!patch.isPresent) return base
        val p = patch.value ?: return null
        val current = base ?: QuietZoneAccent()
        return current.copy(enabled = p.enabled ?: current.enabled, color = p.color ?: current.color, width = p.width ?: current.width, dashArray = p.dashArray ?: current.dashArray)
    }

    private fun mergeOptionalDropShadow(base: DropShadow?, patch: OptionalField<DropShadowPatch>): DropShadow? {
        if (!patch.isPresent) return base
        val p = patch.value ?: return null
        val current = base ?: DropShadow()
        return current.copy(enabled = p.enabled ?: current.enabled, blur = p.blur ?: current.blur, opacity = p.opacity ?: current.opacity, offsetX = p.offsetX ?: current.offsetX, offsetY = p.offsetY ?: current.offsetY)
    }

    private fun mergeOptionalBackgroundPattern(base: BackgroundPattern?, patch: OptionalField<BackgroundPatternPatch>): BackgroundPattern? {
        if (!patch.isPresent) return base
        val p = patch.value ?: return null
        val current = base ?: BackgroundPattern()
        return current.copy(enabled = p.enabled ?: current.enabled, type = p.type ?: current.type, color = p.color ?: current.color, opacity = p.opacity ?: current.opacity, size = p.size ?: current.size)
    }

    private fun mergeOptionalGradientMasking(base: GradientMasking?, patch: OptionalField<GradientMaskingPatch>): GradientMasking? {
        if (!patch.isPresent) return base
        val p = patch.value ?: return null
        val current = base ?: GradientMasking()
        return current.copy(
            enabled = p.enabled ?: current.enabled,
            type = p.type ?: current.type,
            centerColor = if (p.centerColor.isPresent) p.centerColor.value else current.centerColor,
            edgeColor = if (p.edgeColor.isPresent) p.edgeColor.value else current.edgeColor
        )
    }

    private fun mergeOptionalMicroTypography(base: MicroTypography?, patch: OptionalField<MicroTypographyPatch>): MicroTypography? {
        if (!patch.isPresent) return base
        val p = patch.value ?: return null
        val current = base ?: MicroTypography()
        return current.copy(enabled = p.enabled ?: current.enabled, text = p.text ?: current.text, fontSize = p.fontSize ?: current.fontSize, color = p.color ?: current.color, path = p.path ?: current.path)
    }
}
