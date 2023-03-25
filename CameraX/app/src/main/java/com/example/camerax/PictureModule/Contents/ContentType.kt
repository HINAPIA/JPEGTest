package com.example.camerax.PictureModule.Contents

enum class ContentType {
    Image,
    Audio,
    Text

}
enum class Attribute(val code: Int ) {
    general(0),
    focus(1),
    modified(2),
    edited(3)
}
