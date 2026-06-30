package com.example.weathermini.util

object WeatherCodeUtils {
    fun toDescription(code: Int): String = when (code) {
        0          -> "Ясно"
        1          -> "Преимущественно ясно"
        2          -> "Переменная облачность"
        3          -> "Пасмурно"
        45, 48     -> "Туман"
        51, 53, 55 -> "Морось"
        61, 63, 65 -> "Дождь"
        71, 73, 75 -> "Снег"
        80, 81, 82 -> "Ливни"
        95         -> "Гроза"
        96, 99     -> "Гроза с градом"
        else       -> "Код $code"
    }
}
