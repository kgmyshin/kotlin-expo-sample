package reactnative

import react.RBuilder
import react.RHandler
import react.RProps

fun RBuilder.view(handler: RHandler<RProps>) = child(View::class, handler)

fun RBuilder.text(handler: RHandler<RProps>) = child(Text::class, handler)