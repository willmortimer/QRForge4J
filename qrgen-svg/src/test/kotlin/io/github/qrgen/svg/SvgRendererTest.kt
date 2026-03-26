package io.github.qrgen.svg

import io.github.qrgen.core.AlignmentPatternOptions
import io.github.qrgen.core.AlignmentPatternShape
import io.github.qrgen.core.DefaultQrGenerator
import io.github.qrgen.core.LayoutOptions
import io.github.qrgen.core.LocatorCornerStyle
import io.github.qrgen.core.LocatorFrameShape
import io.github.qrgen.core.LocatorLogoOptions
import io.github.qrgen.core.LocatorOptions
import io.github.qrgen.core.ModuleOptions
import io.github.qrgen.core.DotType
import io.github.qrgen.core.QrStyleConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class SvgRendererTest {
    @Test
    fun `renders alignment patterns locator logos and animation`() {
        val config = QrStyleConfig(
            layout = LayoutOptions(width = 512, height = 512, backgroundCornerRadius = 24.0),
            modules = ModuleOptions(type = DotType.ROUNDED, roundSize = true, sizeScale = 0.92),
            locators = LocatorOptions(
                enabled = true,
                defaultStyle = LocatorCornerStyle(
                    outerShape = LocatorFrameShape.ROUNDED,
                    logo = LocatorLogoOptions("https://example.com/logo.png")
                )
            ),
            alignmentPatterns = AlignmentPatternOptions(
                enabled = true,
                shape = AlignmentPatternShape.STAR
            )
        )

        val qrResult = DefaultQrGenerator().generateFromText("svg-feature-test", config)
        val svg = DefaultSvgRenderer().render(qrResult)

        assertTrue(svg.contains("<image href=\"https://example.com/logo.png\""))
        assertTrue(svg.contains("<path d=\"M"))
        assertTrue(svg.contains("rx=\"24"))
    }
}
