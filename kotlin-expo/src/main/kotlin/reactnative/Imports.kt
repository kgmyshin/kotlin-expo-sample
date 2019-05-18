@file:JsModule("react-native")

package reactnative

import react.Component
import react.RProps
import react.RState

external interface ViewProps : RProps {
    var style: dynamic
}

open external class ViewComponent : Component<ViewProps, RState> {
    override fun render(): dynamic
}

open external class ViewBase : ViewComponent

external class View : ViewBase

external object StyleSheet {
    fun create(args: Map<String, Any>): dynamic
}


external interface TextProps : RProps

open external class TextComponent : Component<TextProps, RState> {
    override fun render(): dynamic
}

open external class TextBase : TextComponent
external class Text : TextBase