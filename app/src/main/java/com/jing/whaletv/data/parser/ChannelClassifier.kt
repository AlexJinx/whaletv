package com.jing.whaletv.data.parser

object ChannelClassifier {
    private val provinceHints = listOf(
        "北京", "上海", "天津", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江",
        "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南",
        "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海",
        "内蒙古", "广西", "西藏", "宁夏", "新疆", "BRTV", "Satellite",
    )

    fun priority(id: String, name: String, groupTitle: String): Int {
        return when {
            isCctv(id, name) -> 10
            isSatellite(name) -> 20
            isLocal(name, groupTitle) -> 30
            groupTitle.equals("News", ignoreCase = true) -> 40
            else -> 60
        }
    }

    fun isCctv(id: String, name: String): Boolean {
        val value = "$id $name".uppercase()
        return value.contains("CCTV") || value.contains("CGTN") || value.contains("CCTV+")
    }

    fun isSatellite(name: String): Boolean {
        return name.contains("卫视") ||
            name.contains("Satellite", ignoreCase = true) ||
            provinceHints.any { hint -> name.contains(hint, ignoreCase = true) && name.contains("TV", ignoreCase = true) }
    }

    fun isLocal(name: String, groupTitle: String): Boolean {
        if (isSatellite(name) || isCctv("", name)) return false
        return groupTitle.equals("News", ignoreCase = true) ||
            groupTitle.equals("General", ignoreCase = true) ||
            name.contains("Channel", ignoreCase = true) ||
            name.contains("新闻") ||
            name.contains("综合")
    }
}
