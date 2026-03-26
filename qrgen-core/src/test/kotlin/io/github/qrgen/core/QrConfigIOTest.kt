package io.github.qrgen.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QrConfigIOTest {
    @Test
    fun `reads yaml template and preserves profile patch`() {
        val document = QrConfigIO.readTemplate(
            """
            profile: kiosk
            config:
              layout:
                width: 640
                height: 640
              alignmentPatterns:
                enabled: true
                shape: STAR
            """.trimIndent(),
            "yaml"
        )

        assertEquals("kiosk", document.profile)
        assertEquals(640, document.config.layout.value?.width)
        assertTrue(document.config.alignmentPatterns.value?.enabled == true)
        assertEquals(AlignmentPatternShape.STAR, document.config.alignmentPatterns.value?.shape)
    }

    @Test
    fun `deep merge preserves unspecified fields and clears nullable fields`() {
        val base = QrStyleConfig(
            colors = ColorOptions(foreground = "#111111", background = "#ffffff"),
            locators = LocatorOptions(
                enabled = true,
                defaultStyle = LocatorCornerStyle(color = "#333333"),
                topRight = LocatorCornerStyle(outerShape = LocatorFrameShape.DIAMOND, innerShape = LocatorDotShape.CIRCLE, color = "#444444")
            ),
            advanced = AdvancedOptions(
                gradientMasking = GradientMasking(enabled = true, centerColor = "#000000", edgeColor = "#ffffff")
            )
        )
        val patch = QrConfigIO.readTemplate(
            """
            config:
              colors:
                background: null
              locators:
                topRight:
                  innerShape: DIAMOND
              advanced:
                gradientMasking:
                  edgeColor: "#ff0000"
            """.trimIndent(),
            "yaml"
        ).config

        val merged = QrStyleConfigMerger.merge(base, patch)

        assertNull(merged.colors.background)
        assertEquals("#111111", merged.colors.foreground)
        assertEquals(LocatorFrameShape.DIAMOND, merged.locators.topRight?.outerShape)
        assertEquals(LocatorDotShape.DIAMOND, merged.locators.topRight?.innerShape)
        assertEquals("#000000", merged.advanced.gradientMasking?.centerColor)
        assertEquals("#ff0000", merged.advanced.gradientMasking?.edgeColor)
    }

    @Test
    fun `profile registry merges profile base with template override`() {
        val registry = QrProfileRegistry().apply {
            register(
                "kiosk",
                QrStyleConfig(
                    layout = LayoutOptions(width = 700, height = 700),
                    animation = AnimationOptions(enabled = true, preset = AnimationPreset.PULSE)
                )
            )
        }

        val merged = registry.merge(
            QrConfigIO.readTemplate(
                """
                profile: kiosk
                config:
                  layout:
                    margin: 24
                  animation:
                    durationSeconds: 2.5
                """.trimIndent(),
                "yaml"
            )
        )

        assertEquals(700, merged.layout.width)
        assertEquals(24, merged.layout.margin)
        assertEquals(AnimationPreset.PULSE, merged.animation.preset)
        assertEquals(2.5, merged.animation.durationSeconds)
    }
}
