package com.hinvr.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Shares one recorded frame of the screen behind a clay-tinted bar.
 * The bar blurs only the patch it covers.
 */
internal class ClayGlassState {
    var sourceLayer: GraphicsLayer? = null
    var sourcePosition: Offset = Offset.Zero
    val effects = mutableListOf<ClayGlassEffectNode>()
}

internal fun Modifier.clayGlassSource(state: ClayGlassState): Modifier =
    this.then(ClayGlassSourceElement(state))

internal fun Modifier.clayGlass(
    state: ClayGlassState,
    blurRadius: Dp,
    tint: Color,
): Modifier = this.then(ClayGlassEffectElement(state, blurRadius, tint))

private class ClayGlassSourceElement(
    private val state: ClayGlassState,
) : ModifierNodeElement<ClayGlassSourceNode>() {
    override fun create(): ClayGlassSourceNode = ClayGlassSourceNode(state)

    override fun update(node: ClayGlassSourceNode) {
        node.state = state
    }

    override fun equals(other: Any?): Boolean =
        other is ClayGlassSourceElement && other.state === state

    override fun hashCode(): Int = state.hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "clayGlassSource"
    }
}

private class ClayGlassSourceNode(
    var state: ClayGlassState,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var layer: GraphicsLayer? = null

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer().also { state.sourceLayer = it }
    }

    override fun onDetach() {
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        if (state.sourceLayer === layer) state.sourceLayer = null
        layer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val next = coordinates.positionInRoot()
        if (next != state.sourcePosition) {
            state.sourcePosition = next
            state.effects.toList().forEach { it.invalidateDraw() }
        }
    }

    override fun ContentDrawScope.draw() {
        val recorded = layer
        val width = size.width.roundToInt()
        val height = size.height.roundToInt()
        if (recorded == null || width <= 0 || height <= 0) {
            drawContent()
            return
        }
        recorded.record(this, layoutDirection, IntSize(width, height)) {
            this@draw.drawContent()
        }
        drawLayer(recorded)
    }
}

private class ClayGlassEffectElement(
    private val state: ClayGlassState,
    private val blurRadius: Dp,
    private val tint: Color,
) : ModifierNodeElement<ClayGlassEffectNode>() {
    override fun create(): ClayGlassEffectNode = ClayGlassEffectNode(state, blurRadius, tint)

    override fun update(node: ClayGlassEffectNode) {
        node.state = state
        node.blurRadius = blurRadius
        node.tint = tint
    }

    override fun equals(other: Any?): Boolean =
        other is ClayGlassEffectElement &&
            other.state === state &&
            other.blurRadius == blurRadius &&
            other.tint == tint

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + blurRadius.hashCode()
        result = 31 * result + tint.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clayGlass"
    }
}

internal class ClayGlassEffectNode(
    var state: ClayGlassState,
    var blurRadius: Dp,
    var tint: Color,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {
    private var blurLayer: GraphicsLayer? = null
    private var position: Offset = Offset.Zero

    override fun onAttach() {
        blurLayer = requireGraphicsContext().createGraphicsLayer()
        state.effects += this
    }

    override fun onDetach() {
        state.effects -= this
        blurLayer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        blurLayer = null
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val next = coordinates.positionInRoot()
        if (next != position) {
            position = next
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val source = state.sourceLayer
        val blurred = blurLayer
        val width = size.width
        val height = size.height
        if (source != null && blurred != null && width > 0f && height > 0f) {
            val expand = blurRadius.toPx()
            val recordWidth = (width + expand * 2f).roundToInt().coerceAtLeast(1)
            val recordHeight = (height + expand * 2f).roundToInt().coerceAtLeast(1)
            blurred.renderEffect = if (expand > 0.5f && BlurEffect(expand, expand).isSupported()) {
                BlurEffect(expand, expand, TileMode.Clamp)
            } else {
                null
            }
            val shiftX = state.sourcePosition.x - position.x
            val shiftY = state.sourcePosition.y - position.y
            blurred.record(
                this,
                layoutDirection,
                IntSize(recordWidth, recordHeight),
            ) {
                translate(shiftX - expand, shiftY - expand) {
                    drawLayer(source)
                }
            }
            translate(-expand, -expand) {
                drawLayer(blurred)
            }
        }
        drawRect(tint)
        drawContent()
    }
}
