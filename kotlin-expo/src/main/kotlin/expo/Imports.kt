@file:JsModule("expo")

package expo

import react.Component

external fun <T : Component<*, *>> registerRootComponent(component: JsClass<T>)