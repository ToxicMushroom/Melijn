package me.melijn.melijnbot.internals.utils

import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class BorderedTableBuilder(private val split: Boolean) {

    private var headerRow: MutableList<String> = ArrayList()
    private var valueRows: MutableMap<Int, List<String>> = HashMap()
    private var columnWidth: MutableMap<Int, Int> = HashMap()
    private var footerRow: MutableList<String> = ArrayList()

    companion object {
        const val LINE = "══════════════════════════════════════════════════════════════════════════════════"
    }

    fun setColumns(vararg headerElements: String): BorderedTableBuilder {
        headerRow.addAll(headerElements)
        findWidest(*headerElements)
        return this
    }

    fun addRow(vararg rowElements: String): BorderedTableBuilder {
        valueRows[valueRows.size] = listOf(*rowElements)
        findWidest(*rowElements)
        return this
    }

    fun setFooterRow(vararg footerElements: String): BorderedTableBuilder {
        footerRow.addAll(footerElements)
        findWidest(*footerElements)
        return this
    }

    private fun findWidest(vararg rowElements: String) {
        for ((temp, s) in rowElements.withIndex()) {
            if (columnWidth.getOrDefault(temp, 0) < s.length) {
                columnWidth[temp] = s.length
            }
        }
    }

    fun build(): List<String> {
        require(!valueRows.values.stream().anyMatch { array -> array.size > headerRow.size }) {
            "A value row cannot have more values then the header (you can make empty header slots)"
        }

        require(footerRow.size <= headerRow.size) {
            "A footer row cannot have more values then the header (you can make empty header slots)"
        }


        var maxRowWidth = columnWidth.size * 3 - 2
        for (i in columnWidth.values) {
            maxRowWidth += i
        }


        var sb = StringBuilder()
        val toReturn = ArrayList<String>()
        addTop(sb)
        addRow(sb, headerRow)
        addSplicer(sb)

        //main
        for (element in valueRows.values) {
            if (split && sb.length + maxRowWidth > 1997 - (if (footerRow.size > 0) maxRowWidth * 3 else maxRowWidth)) {
                toReturn.add("$sb```")
                sb = StringBuilder()
                sb.append("```prolog\n")
            }

            addRow(sb, element)
        }

        if (footerRow.size > 0) {
            addSplicer(sb)
            addRow(sb, footerRow)
        }
        addBottom(sb)

        toReturn.add("$sb```")

        //less gc
        headerRow.clear()
        valueRows.clear()
        columnWidth.clear()
        footerRow.clear()
        return toReturn
    }

    //╔════╦════╗
    private fun addTop(sb: StringBuilder) {
        sb.append("```prolog\n╔").append(getLine(0, 2))
        for (i in 1 until headerRow.size) {
            sb.append("╦").append(getLine(i, 2))
        }
        sb.append("╗\n")
    }

    //║    ║    ║
    private fun addRow(sb: StringBuilder, list: List<String>) {
        sb.append("║")
        for ((i, value) in list.withIndex()) {
            sb.append(" ")
                .append(value)
                .append(getSpaces(i, value))
                .append(" ║")
        }
        sb.append("\n")
    }


    //╠════╬════╣
    private fun addSplicer(sb: StringBuilder) {
        sb.append("╠").append(getLine(0, 2))
        for (i in 1 until headerRow.size) {
            sb.append("╬").append(getLine(i, 2))
        }
        sb.append("╣\n")
    }

    //╚════╩════╝
    private fun addBottom(sb: StringBuilder) {
        sb.append("╚").append(getLine(0, 2))
        for (i in 1 until headerRow.size) {
            sb.append("╩").append(getLine(i, 2))
        }
        sb.append("╝")
    }

    private fun getSpaces(widthIndex: Int, value: String): String {
        return columnWidth[widthIndex]?.minus(value.length)?.let {
            " ".repeat(50).substring(0, it)
        } ?: ""
    }

    private fun getLine(widthIndex: Int, extra: Int = 0): String {
        return columnWidth[widthIndex]?.let { LINE.substring(0, it + extra) } ?: ""
    }
}