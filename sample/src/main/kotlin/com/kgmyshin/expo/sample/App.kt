package com.kgmyshin.expo.sample

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import reactnative.text
import reactnative.view

class App : RComponent<RProps, RState>() {
    override fun RBuilder.render() {
        view {
            text {
                +(1..1000).joinToString { "Hello world." }
            }
        }
    }
}